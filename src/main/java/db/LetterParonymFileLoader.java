package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.sql.SQLException;

import db.WordFromFile;
import lingv.GramDecoder;
import lingv.Distance;

/**
 * Осуществляет заполнение БД буквенными паронимами на основе текстового файла
 */
public class LetterParonymFileLoader extends ParonymFileLoader {
    /**
     * Имя базы данных
     */
    String dbName;

    /**
     * Имя входного файла
     */
    String inputFileName;

    /**
     * Имя файла, содержащего содержание таблиц с кодами грамм. характеристик.
     */
    String codesFileName;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        LetterParonymFileLoader loader;
        String dbName = args[0];
        String inputFileName = args[1];
        String codesFileName = "inputFiles\\codes.txt";

        try {
            loader = new LetterParonymFileLoader(dbName, inputFileName,
                    codesFileName);
            loader.run_prepareLetParDb();
            loader.unInit();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public LetterParonymFileLoader(String dbName, String inputFileName,
            String codesFileName) throws Exception {
        super.init(dbName);
        this.dbName = dbName;
        this.inputFileName = inputFileName;
        this.codesFileName = codesFileName;
    }

    /**
     * Создать базу данных буквенных паронимов на основе текстового файла.
     * Удалить существительные во множественном числе.
     * 
     * @throws Exception
     *             ошибка в работе процедуры загрузки
     */
    public void run() throws Exception {
        db.createDB();
        loadCodes(codesFileName);
        loadDataFromFileToDb(inputFileName, true);
        removePluralNouns();
    }

    /**
     * Составляет подготовительную БД на основе файла буквенных паронимов для
     * дальнейшего слияния с БД морфемных паронимов.
     * 
     * @throws Exception
     *             ошибка в работе процедуры загрузки
     */
    public void run_prepareLetParDb() throws Exception {
        db.createDB();
        loadCodes(codesFileName);
        db.update("alter table words add column mdb_id integer");
        loadDataFromFileToDb(inputFileName, true);
    }

    /**
     * Создать базу данных буквенных паронимов на основе текстового файла.
     * Удалить существительные во множественном числе. Автоматически пополнить
     * базу.
     * 
     * @throws Exception
     *             ошибка в работе процедуры загрузки
     */
    public void run_FileAndAutoParonyms() throws Exception {
        db.createDB();
        loadCodes(codesFileName);
        loadDataFromFileToDb(inputFileName, true);
        removePluralNouns();
        addAllParonyms(1, 0, 0);
    }

    /**
     * Загрузить словарные данные из текстового файла в базу данных.
     * 
     * @param inputFileName
     *            имя исходного текстового файла
     * @param addParonyms
     *            true - загрузить слова и паронимические отношения, false -
     *            загрузить только слова
     * @throws SQLException
     *             ошибка при работе с БД
     * @throws ParonymCodeException
     *             ошибка кодирования параметров паронимического отношения
     */
    void loadDataFromFileToDb(String inputFileName, boolean addParonyms)
            throws SQLException, ParonymCodeException {
        // initialFileName = C:\java\paronym\letpar.txt
        BufferedReader inputFile;
        String s;
        WordFromFile headword = null;
        int id1 = 0;
        int id2;
        int dl;
        int dl2;
        WordFromFile w;
        int counter = 0;

        byte dlcode;

        try {
            inputFile = new BufferedReader(new FileReader(inputFileName));
            System.out.println("Loading data from file " + inputFileName);

            while ((s = inputFile.readLine()) != null) {

                counter++;
                if (counter % 10000 == 0) {
                    System.out.println(counter);
                }
                s = s.toLowerCase();

                // обработка предыдущей группы паронимов
                if (s.charAt(0) != ' ') {
                    // обнулить старое заглавное слово
                    headword = null;
                }

                // обработка текущего слова
                w = makeWordFromFile(s.trim().toCharArray());

                if (headword != null) {
                    w.part = headword.part;
                }

                if (!db.queryResult(
                        "select * from words where " + "(word='" + w.word
                                + "')and(part=" + w.part + ")").next()) {

                    db.update("INSERT INTO WORDS(word,part) VALUES( '" + w.word
                            + "'," + w.part + ")");

                }

                w.id = db.queryAndGetId("select id from words where "
                        + "(word='" + w.word + "')and(part=" + w.part + ")");

                if (s.charAt(0) != ' ') {
                    // заглавное слово
                    headword = w;
                    id1 = w.id;
                } else if (addParonyms) {
                    id2 = w.id;
                    dl = Distance.countDistance(w.word, headword.word, false);
                    dl2 = Distance.countDistance(w.word, headword.word, true);

                    dlcode = ParonymCoder.generateDlCode(dl, dl2);
                    if ((dlcode != -1)
                            && !db.queryResult(
                                    "select * from paronyms where "
                                            + "(id1="
                                            + id1
                                            + ") and(id2="
                                            + id2
                                            + ") union "
                                            + "select * from paronyms where (id1="
                                            + id2 + ") and(id2=" + id1 + ")")
                                    .next()) {
                        db.update("insert into paronyms (id1,id2,dlcode) values "
                                + "(" + id1 + "," + id2 + "," + dlcode + ")");
                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Преобразовать строку из входного файла в объект WordFromFile. Во время
     * преобразования выбрасываются цифры, определяется часть речи (если она
     * указана).
     * 
     * @param str
     *            строка файла
     * @return слово, соответствующее строке str (объект WordFromFile).
     */
    WordFromFile makeWordFromFile(char[] str) {
        int len = str.length;
        int i = 0;

        // для слов со знаком ^
        int j = 0;
        boolean shift = false;

        WordFromFile w = new WordFromFile();

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

        return w;
    }

}
