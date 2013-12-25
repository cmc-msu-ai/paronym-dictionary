package db;

import aot.Paradigm;
import aot.MorphAn;
import aot.MorphAnException;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Arrays;

import static lingv.GramDecoder.NOUN;

import lingv.GramDecoder;

/**
 * Подсчет статистических данных для входных файлов с паронимами.
 */
public class InputFilesStatistics {

    /**
     * Запуск подсчета статистики
     * @param args не используется
     */
   public static void main(String[] args) {
        InputFilesStatistics ifs;

        try{
            ifs = new InputFilesStatistics();
            ifs.convertInputFile(new BufferedReader(new FileReader("inputFiles\\morphpar.txt")));
            ifs.countSingPlurMismatch(new BufferedReader(new FileReader("outputFiles\\convertedMorphpar.txt")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Составить файл, содержащий все слова входного файла, которые присутствуют в ед.ч., но не присутств. во множ. ч.
     * или наоборот
     * @throws IOException ошибка работы с файлами
     * @param inputFile конвертир. файл
     * @throws aot.MorphAnException
     */
    private void countSingPlurMismatch(BufferedReader inputFile) throws IOException, MorphAnException {
        BufferedWriter outputFile1 = new BufferedWriter(new FileWriter("outputFiles\\NonPaired.txt"));
        BufferedWriter outputFile2 = new BufferedWriter(new FileWriter("outputFiles\\Paired.txt"));
        ArrayList<Paradigm> wordlist = new ArrayList<Paradigm>();
        String s;
        StringTokenizer tok;
        int part;
        Paradigm word;
        Paradigm pairWord;
        int counter1 = 0;
        int counter2 = 0;
        boolean nounGroup = false;

        while ((s = inputFile.readLine()) != null) {
            if (s.isEmpty()) {
                //end of group
                //add all words without pair to outputFile1
                for (Paradigm w : wordlist) {
                    outputFile1.write(w.form);
                    outputFile1.newLine();
                    counter1++;
                }
                if (!wordlist.isEmpty()) {
                    outputFile1.newLine();
                    wordlist.clear();
                }
                continue;
            }
            if (s.startsWith("Всего")) {
                continue;
            }
            tok = new StringTokenizer(s);
            part = new Integer(tok.nextToken());
            if (part != NOUN) {
                if (nounGroup) {
                    outputFile2.newLine();
                }
                nounGroup = false;
                continue;
            }
            word = MorphAn.analyzeAsNounForm(tok.nextToken());
            if ((pairWord = findPairWord(word, wordlist)) != null) {
                wordlist.remove(pairWord);

                //add all pair words to outputFile2
                outputFile2.write(word.norm);
                outputFile2.newLine();
                counter2++;
                nounGroup = true;
            } else {
                wordlist.add(word);
            }
        }
        outputFile1.write("Всего: " + counter1);
        outputFile1.newLine();
        outputFile1.close();
        outputFile2.write("Всего пар: " + counter2);
        outputFile2.newLine();
        outputFile2.close();
    }

    /**
     * Найти в списке wordlist слово, принадлежащее той же парадигме, что и word
     * @param word слово
     * @param wordlist список
     * @return первое подходящее слово или null.
     */
    private Paradigm findPairWord(Paradigm word, ArrayList<Paradigm> wordlist) {
        for (Paradigm w : wordlist) {
            if (word.norm.equals(w.norm)) {
                return w;
            }
        }
        return null;
    }

    /**
     * Преобразовать входной файл для работы функции countSingPlurMismatch
     * Для слов определяются части речи и удаляются омонимы
     * @param inputFile входной файл
     * @throws java.io.IOException ошибка работы с файлом
     * @throws aot.MorphAnException ошибка работы морфологического анализатора
     */
    private void convertInputFile(BufferedReader inputFile) throws IOException, MorphAnException {
        BufferedWriter convertedFile = new BufferedWriter(new FileWriter("outputFiles\\convertedMorphpar.txt"));
        String s;
        int counter = 0;
        int groupPart = -1;
        boolean groupError = false;
        ArrayList<WordFromFile> wordlist = new ArrayList<WordFromFile>();
        WordFromFile w;
        boolean[] posArray = new boolean[GramDecoder.allParts.length];
        ArrayList<Paradigm> paradigms;
        String gramStr;

        int countWord = 0;
        int countGroup = 0;

        while ((s = inputFile.readLine()) != null) {
            counter++;
            if (counter % 10000 == 0) {
                System.out.println(counter);
            }

            s = s.toLowerCase();

            if (s.charAt(0) != ' ') { //1ое слово группы

                //обработка предыдущей группы паронимов

                if ((groupPart != -1) && !groupError) {
                    //если определилась часть речи
                    // присвоить ее всем словам группы
                    setPosForGroup(wordlist, groupPart);

                    //записать группу в файл
                    for (WordFromFile wrd : wordlist) {
                        convertedFile.write(wrd.part + " ");
                        convertedFile.write(wrd.word);
                        convertedFile.newLine();
                        countWord++;
                    }
                    //вставить пустую строку. обозначает конец группы.
                    if (!wordlist.isEmpty()) {
                        convertedFile.newLine();
                        countGroup++;
                    }

                }

                groupPart = -1;
                groupError = false;
                wordlist.clear();
            }

            //обработка текущего слова

            s = s.trim();

            //запомнить часть речи, на случай выброса исключения при разборе
            if (s.endsWith("<av")) {
                groupPart = GramDecoder.ADVERB;
            }

            w = makeWordFromFile(s, groupPart);

            //избавление от омонимов
            if (wordIsIn(w, wordlist)) {
                continue;
            }

            //попробовать найти в АОТ
            paradigms = MorphAn.analyzeForm(w.word);
            if (paradigms.isEmpty()) {
                //слово не найдено
                continue;
            }

            //для каждой части речи, найденной для слова АОТом,
            // объявляется истинным соответствующий этой чр элемент массива
            fillPosArray(paradigms, posArray);

            //если истинным явл. только один элемент массива - ч.р. определилась однозначно
            w.part = getPosArrayValue(posArray);
            if (w.part != -1) {
                if ((groupPart == -1)) {
                    groupPart = w.part;
                } else if (w.part != groupPart) {
                    //"эффект кипятильников"
                    //ошибка входного файла: слова разной части речи попали в 1 группу
                    groupError = true;
                }
            }

            //добавить текущее слово в группу
            wordlist.add(w);

        } //while

        //обработать последнюю группу
        setPosForGroup(wordlist, groupPart);
        inputFile.close();

        convertedFile.write("Всего слов: " + countWord);
        convertedFile.newLine();
        convertedFile.write("Всего групп: " + countGroup);
        convertedFile.newLine();
        convertedFile.close();

    }

    private WordFromFile makeWordFromFile(String s, int groupPart) {
        String word;
        char[] str = s.toCharArray();
        char[] wrd = new char[64];
        int i = 0;

        for (char c : str) {
            if (c == '<') {
                break;
            }
            if (Character.isLetter(c)) {
                wrd[i] = c;
                i++;
            }
        }
        word = new String(wrd, 0, i);
        return new WordFromFile(word, groupPart);
    }

    //copied from MorphemParonymFileLoader
    private boolean wordIsIn(WordFromFile w, ArrayList<WordFromFile> parlist) {
        for (WordFromFile word : parlist) {
            if (word.word.equals(w.word)) {
                return true;
            }
        }
        return false;
    }

    //copied from MorphemParonymFileLoader
    private int getPosArrayValue(boolean[] posArray) {
        int part = -1;
        boolean found = false;
        for (int i = 0; i < posArray.length; i++) {
            if (posArray[i]) {
                if (found) {
                    return -1;
                }
                part = i;
                found = true;
            }
        }
        return part;
    }

    private void fillPosArray(ArrayList<Paradigm> paradigms, boolean[] posArray) {
        Arrays.fill(posArray, false);
        for (Paradigm w : paradigms) {
            if (w.part != -1) {
                posArray[w.part] = true;
            }

        }
    }

    private void setPosForGroup(ArrayList<WordFromFile> list, int groupPart){

        for (WordFromFile wrd : list) {
            wrd.part = groupPart;
        }
    }
}
