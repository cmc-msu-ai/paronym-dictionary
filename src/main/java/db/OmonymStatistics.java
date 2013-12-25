package db;

import lingv.GramDecoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

//сравнение числа паронимов у омонимов
public class OmonymStatistics {

    public static void main(String[] args) {
        OmonymStatistics os = new OmonymStatistics();
        os.omonymStatistics("inputFiles\\letpar.txt");

    }

    public void omonymStatistics(String inputFileName) {
        // name = C:\java\paronym\letpar.txt
        BufferedReader inputFile;
        String s;

        // omonyms
        ArrayList<Omonym> olist = new ArrayList<Omonym>();
        Omonym omon;
        Omonym omonym;
        int curCount;
        int i = 0;

        try {
            inputFile = new BufferedReader(new FileReader(inputFileName));
            s = inputFile.readLine();
            while (s != null) {

                s = s.toLowerCase();

                // обработка группы паронимов
                if (s.charAt(0) != ' ') {
                    omon = makeOmonym(s.toCharArray());
                    if (omon != null) {
                        if ((omonym = getOmonym(olist, omon)) != null) {
                            curCount = omonym.parCount;
                            while ((s = inputFile.readLine()) != null) {
                                if (s.charAt(0) != ' ') {
                                    if (i != curCount) {
                                        omonym.dump();
                                    }
                                    i = 0;
                                    break;
                                } else {
                                    i++;
                                }
                            }
                        } else {
                            olist.add(omon);
                            while ((s = inputFile.readLine()) != null) {
                                if (s.charAt(0) != ' ') {
                                    omon.parCount = i;
                                    i = 0;
                                    break;
                                } else {
                                    i++;
                                }
                            }
                        }

                    }

                }

                s = inputFile.readLine();

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Omonym getOmonym(ArrayList<Omonym> olist, Omonym omon) {
        for (Omonym o : olist) {
            if (o.word.equals(omon.word) && (o.part == omon.part)) {
                return o;
            }
        }
        return null;
    }

    private Omonym makeOmonym(char[] str) {
        int len = str.length;
        int i = 0;

        // для слов со знаком ^
        int j = 0;
        boolean shift = false;
        boolean omonym = false;

        Omonym w = new Omonym();

        try {
            while (i < len) {
                if (str[i] == '<') {
                    // @todo случай неправильных вх. данных (возможен выход за
                    // гр. массива)
                    switch (str[i + 1]) {
                    case 's':
                        w.part = GramDecoder.NOUN;
                        break;
                    case 'v':
                        w.part = GramDecoder.VERB;
                        break;
                    case 'a':
                        if (str[i + 2] == 'j') {
                            w.part = GramDecoder.ADJECT;
                        } else {
                            w.part = GramDecoder.ADVERB;
                        }
                        break;
                    default:
                        throw new InvalidInputException();
                    }
                    len = i;
                    break;
                } else if (Character.isDigit(str[i])) {
                    omonym = true;
                    shift = true;
                    j--;
                } else if (str[i] == '^') {
                    shift = true;
                    str[j] = Character.toUpperCase(str[i + 1]);
                    i++; // пропустить 1 символ
                } else if (shift) {
                    str[j] = str[i];
                }
                i++;
                j++;
            }
            w.word = shift ? new String(str, 0, j) : new String(str, 0, len);
        } catch (InvalidInputException e) {
            System.out.println("Ошибка в задании части речи:" + str);
            return null;
        }

        return omonym ? w : null;
    }

    class Omonym {
        public String word;
        public int part;
        public int parCount;

        public void dump() {
            System.out.println(word + " " + part);
        }
    }
}