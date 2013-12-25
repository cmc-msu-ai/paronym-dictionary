package db;

import java.io.*;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;

import db.WordFromFile;
import lingv.GramDecoder;
import lingv.Distance;
import dict.Morph;
import aot.MorphAn;
import aot.Paradigm;
import aot.MorphAnException;

/**
 * Осуществляет загрузку в БД словарной информации о морфемных паронимах на основе текстового файла
 */
public class MorphemParonymFileLoader extends ParonymFileLoader {
    /**
     * Файл, в который записываются слова, часть речи которых не определилась автоматически.
     */
    BufferedWriter humanTask;

    /**
     * Лог-файл, содержащий информацию об ошибках, возникших при загрузке, и статистику загруженных данных.
     */
    BufferedWriter logfile;

    /**
     * Файл, получаемый из исходного файла морфемных паронимов, используемый при загрузке информации в БД.
     */
    BufferedReader inputFile;

    /**
     * Исходный текстовый файл морфемных паронимов. На его основе создается файл inputFile.
     */
    BufferedReader initialFile;

    /**
     * Имя базы данных
     */
    String dbName;

    /**
     * Имя дополнительной БД (буквенных паронимов), которая в процессе загрузки сливается с БД морфемных паронимов.
     */
    String addDbName;

    /**
     * Имя файла, содержащего содержание таблиц с кодами грамм. характеристик.
     */
    String codesFileName;

    /**
     * Имя файла initialFile.
     */
    String initialFileName;

    /**
     * Имя файла inputFile.
     */
    String inputFileName;

    /**
     * Имя файла, содержащего группы чередующихся корней.
     */
    String rootsFileName;

    public MorphemParonymFileLoader(String dbName,
                                    String codesFileName,
                                    String initialFileName,
                                    String inputFileName,
                                    String rootsFileName,
                                    String addDbName) throws Exception {
        super.init(dbName);
        this.dbName = dbName;
        this.codesFileName = codesFileName;
        this.initialFileName = initialFileName;
        this.inputFileName = inputFileName;
        this.rootsFileName = rootsFileName;
        this.addDbName = addDbName;

    }

    /**
     * Запуск загрузчика файла морфемных паронимов.
     * @param args args[0] - имя создаваемой БД (dbName)
     * args[1] -  имя исходного текстового файла (initialFileName)
     * args[2] -  имя дополнительной БД (буквенных паронимов) (addDbName)
     */
    public static void main(String[] args) {
        MorphemParonymFileLoader loader;
        String dbName = args[0];
        String codesFileName = "inputFiles\\codes.txt";
        String initialFileName = args[1];
        String inputFileName = "inputFiles\\morphParInputFile.txt";
        String rootsFileName = "inputFiles\\rootsId_groups.txt";
        String addDbName = args[2];

        try {
            loader = new MorphemParonymFileLoader(dbName, codesFileName, initialFileName, inputFileName, rootsFileName, addDbName);
            loader.run();
            loader.unInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Процедура составления словаря буквенных и морфемных паронимов.
     * Выполняется реформатирование исходного файла, загрузка файлов с кодировками грамм. параметров,
     * загрузка словарных данных из текстового файла в БД, заполнение таблицы чередующихся корней,
     * добавление паронимических отношений из файла,
     * автоматическое пополнение морфемных паронимов,
     * слияние с БД буквенных паронимов,
     * автоматическое пополнение буквенных паронимов.
     * @throws Exception ошибка в процедуре загрузки
     */
    public void run() throws Exception {
        reformatMorphParFile(initialFileName);
        db.createDB();
        loadCodes(codesFileName);
        loadDataFromFile(inputFileName, CREATE_ROOTS_FILES, INCLUDE_NOT_FOUND_WORDS);
        fillRootsTable(rootsFileName);
        loadParonymsFromFile(inputFileName, INCLUDE_NOT_FOUND_WORDS, DO_NOT_ALLOW_SING_PLUR_FORMS_MIX);
        addAllParonyms(0, 2, 2);
        mergeWithDb(addDbName);
        addAllParonyms(2, 0, 0);
    }

    /**
     * Добавить в БД данные из таблиц words и paronyms базы буквенных паронимов
     * @param addDbName имя БД буквенных паронимов
     * @throws Exception ошибка в процедуре загрузки
     */
    private void mergeWithDb(String addDbName) throws Exception {
        BufferedWriter notfoundFile = new BufferedWriter(new FileWriter("outputFiles\\LetterParonym_notFound.txt"));
        ResultSet lrs;
        ResultSet rs;

        Database ldb = new Database(addDbName);

        WordFromFile w;
        int id;

        int counter = 0;

        //words
        System.out.println("Merging WORDS");

        lrs = ldb.queryResult("select * from words");
        while (lrs.next()) {
            counter++;
            if (counter % 10000 == 0) {
                System.out.println(counter);
            }
            w = new WordFromFile(lrs.getInt("id"),lrs.getString("word"), lrs.getInt("part"));
            rs = db.queryResult("select * from words where (word = '" + w.word + "')and(part=" + w.part + ")");
            if (rs.next()) {
                id = rs.getInt("id");
            } else {
                db.update("insert into words (word, part) values ('" + w.word + "'," + w.part + ")");
                id = db.queryAndGetId("call identity()");


                morphAn(w);
                if (w.aot_id != -1) {
                    db.update("update words set aot_id = " + w.aot_id + "where id =" + id);
                } else {
                    notfoundFile.write(w.word);
                    notfoundFile.newLine();
                }
                if (w.numflag != WordFromFile.NOTSET) {
                    db.update("update words set num = " + w.num + "where id =" + id);
                }
                if (w.genflag != WordFromFile.NOTSET) {
                    db.update("update words set gen = " + w.gen + "where id =" + id);
                }
                if (w.animflag != WordFromFile.NOTSET) {
                    db.update("update words set anim = " + w.anim + "where id =" + id);
                }
            }
            ldb.update("update words set mdb_id = " + id + "where id =" + w.id);
        }

        //paronyms
        int lid1;
        int lid2;
        int id1;
        int id2;
        byte dlcode;
        double q;
        ResultSet rs1;
        WordFromFile word1;
        WordFromFile word2;
        counter = 0;

        System.out.println("Merging paronyms");
        lrs = ldb.queryResult("select * from paronyms");
        while (lrs.next()) {
            counter++;
            if (counter % 10000 == 0) {
                System.out.println(counter);
            }

            lid1 = lrs.getInt("id1");
            lid2 = lrs.getInt("id2");
            dlcode = lrs.getByte("dlcode");

            rs1 = ldb.queryResult("select * from words where id =" + lid1);
            rs1.next();
            id1 = rs1.getInt("mdb_id");
            word1 = new WordFromFile(rs1.getString("word"));

            rs1 = ldb.queryResult("select * from words where id =" + lid2);
            rs1.next();
            id2 = rs1.getInt("mdb_id");
            word2 = new WordFromFile(rs1.getString("word"));

            q = Distance.countFormula(word1.word, word2.word, ParonymCoder.getDl(dlcode));

            if (db.queryResult("select * from paronyms where " +
                    "(id1=" + id1 + ") and(id2=" + id2 + ")").next()) {
                db.update("update paronyms set dlcode = " + dlcode + ", q= " + q  +
                        "where (id1 =" + id1 + ")and(id2=" + id2 + ")");

            } else if (db.queryResult("select * from paronyms where " +
                    "(id1=" + id2 + ") and(id2=" + id1 + ")").next()) {
                db.update("update paronyms set dlcode = " + dlcode + ", q= " + q +
                        "where (id1 =" + id2 + ")and(id2=" + id1 +")");

            } else {
                db.update("insert into paronyms (id1,id2,dlcode, q) values " +
                                "(" + id1 + "," + id2 + "," + dlcode + "," + q +")");
            }
        }
        notfoundFile.close();
        ldb.shutdown();
    }


    private void reformatMorphParFile(String initialFileName) throws IOException, MorphAnException, SQLException {
        initialFile = new BufferedReader(new FileReader(initialFileName));
        BufferedWriter resultFile = new BufferedWriter(new FileWriter("inputFiles\\morphParInputFile.txt"));
        humanTask = new BufferedWriter(new FileWriter("outputFiles\\MorphemLoader_humanTask.txt"));
        logfile = new BufferedWriter(new FileWriter("outputFiles\\MorphemLoader_logfile.txt"));
        String s;
        ArrayList<WordFromFile> wordList = new ArrayList<WordFromFile>();
        WordFromFile w = new WordFromFile();

        //для статистики
        int counter = 0;   //счетчик считанных из файла слов
        int invCount = 0; // cчетчик некорректных слов

        //определение части речи
        int groupPart = -1; //часть речи группы
        boolean groupError = false;
        boolean[] posArray = new boolean[GramDecoder.allParts.length];

        ArrayList<Paradigm> paradigms;

        System.out.println("Reformatting file " + initialFileName);

        try {

            while ((s = initialFile.readLine()) != null) {
                counter++;
                if (counter % 10000 == 0) {
                    System.out.println(counter);
                }

                s = s.toLowerCase();

                if (s.charAt(0) != ' ') { //1ое слово группы

                    //обработка предыдущей группы

                    if ((groupPart == -1) || groupError) {
                        humanTask.newLine();
                        //если часть речи не определилась - удалить все слова
                        for (WordFromFile wrd : wordList) {
                            humanTask.write(wrd.word+ " " + GramDecoder.partIntToStr(wrd.part));
                            humanTask.newLine();
                        }
                        wordList.clear();
                    } else {
                         //если определилась - присвоить ее всем словам группы
                        setPosForGroup(wordList, groupPart);
                    }

                    //добавить слова в файл
                    for (WordFromFile wrd : wordList) {
                        resultFile.write(wrd.word + " " + wrd.splitWord + " " + wrd.part);
                        resultFile.newLine();
                    }
                    if (!wordList.isEmpty()) {
                        resultFile.newLine();
                    }

                    groupPart = -1;
                    groupError = false;
                    wordList.clear();
                }

                //обработка текущего слова
                try {
                    s = s.trim();

                    //запомнить часть речи, на случай выброса исключения при разборе
                    if (s.endsWith("<av")) {
                        groupPart = GramDecoder.ADVERB;
                    }

                    w = makeWordFromFile(s, groupPart);

                    //избавление от омонимов
                    if (wordIsIn(w, wordList)) {
                        throw new InvalidInputException("Duplicate word: " + s);
                    }

                    //попробовать найти в словаре морф. анализатора
                    paradigms = MorphAn.analyzeForm(w.word);

                    if (!paradigms.isEmpty()) {
                        //слово найдено. Попробовать определить часть речи.

                        //для каждой части речи, найденной для слова,
                        // объявляется истинным соответствующий этой чр элемент массива
                        fillPosArray(paradigms, posArray);

                        //если истинным явл. только один элемент массива - ч.р. определилась однозначно
                        w.part = getPosArrayValue(posArray);
                        if (w.part != -1){
                            if ((groupPart == -1)) {
                                groupPart = w.part;
                            } else if (w.part != groupPart) {
                                //"эффект кипятильников"
                                //ошибка входного файла: слова разной части речи попали в 1 группу
                                groupError = true;
                            }
                        }
                    }

                    //добавить текущее слово в группу
                    wordList.add(w);

                } catch (InvalidInputException e) {
                    invCount++;
                    if (!e.getMessage().startsWith("Duplicate")) {
                        logfile.write(e.getMessage());
                        logfile.newLine();
                    }

                }

            } //while

            //обработать последнюю группу
            setPosForGroup(wordList, groupPart);

            logStatistics(counter, invCount);

            humanTask.close();
            logfile.close();
            resultFile.close();
            initialFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            throw new SQLException(w.splitWord);
        }
    }


    /*
    *  Алгоритм:
    * слово проверяется:
    *      на правильность разбора (по указаниям автора)
    *      на наличие в базе АОТ
    * если проверка не пройдена, слово игнорируется
    * с помощью АОТ определяется часть речи,
    * если ч.р. не определяется однозначно сразу для всех слов группы, то все они записываются в файл,
    *                                      для них ч.р. должна быть определена вручную
    *
    * НЕ удаляет слова в форме множ. числа. (т.к. для этого сначала необходимо определить часть речи всей группы)
    */
    public void loadDataFromFile(String inputFileName, boolean createRootsFiles, boolean includeNotFoundWords)
            throws SQLException, IOException, MorphAnException {

        inputFile = new BufferedReader(new FileReader(inputFileName));
        logfile = new BufferedWriter(new FileWriter("outputFiles\\MorphemLoader_duplicateWords.txt"));
        BufferedWriter notfoundFile = new BufferedWriter(new FileWriter("outputFiles\\MorphemLoader_notFound.txt"));

        String s;
        ArrayList<WordFromFile> wordList = new ArrayList<WordFromFile>();
        WordFromFile w = new WordFromFile();

        //для статистики
        int counter = 0;   //счетчик считанных из файла слов
        int invCount = 0; // cчетчик неправильных слов
        int notFoundCount = 0; //счетчик правильных, но не вставленных в бд слов (из-за несоответствия параметрам)
        int nounCount = 0;
        int verbCount = 0;
        int adjCount = 0;
        int advCount = 0;
        int groupCount = 0;

        int groupPart;

        //для чередующихся корней
        ArrayList<HashSet<Integer>> rootsList = new ArrayList<HashSet<Integer>>(); //список наборов чередующихся корней
        ArrayList<Integer> rootsPartsList = new ArrayList<Integer>(); //список частей речи для элементов rootsList
        HashSet<Integer> rootsSet;  //набор чередующихся корней

        System.out.println("Loading data from file " + inputFileName);

        try {

            while ((s = inputFile.readLine()) != null) {

                if (counter % 10000 == 0) {
                    System.out.println(counter);
                }

                if (s.isEmpty()) { //конец группы
                    if (!wordList.isEmpty()) {
                        groupPart = wordList.get(0).part;

                        //возвращается набор чередующихся корней
                        rootsSet = addWordsToDB(wordList);
                        if ((rootsSet.size() > 1) && !rootsList.contains(rootsSet)) {
                            rootsList.add(rootsSet);
                            rootsPartsList.add(groupPart);
                        }

                        //сбор статистики
                        groupCount++;
                        switch (groupPart) {
                            case GramDecoder.NOUN:
                                nounCount += wordList.size();
                                break;
                            case GramDecoder.VERB:
                                verbCount += wordList.size();
                                break;
                            case GramDecoder.ADJECT:
                                adjCount += wordList.size();
                                break;
                            case GramDecoder.ADVERB:
                                advCount += wordList.size();
                                break;
                            default:
                        }


                    }
                    wordList.clear();
                } else {
                    //обработка текущего слова
                    try {
                        counter++;
                        w = makeWordFromFormattedFile(s);

                        //проверить на отсутствие дубликатов в базе
                        if (db.queryResult("select * from words where " +
                                "(word='" + w.word + "')and(part=" + w.part + ")").next()) {
                            throw new InvalidInputException(w.word);
                        }

                        //попробовать произвести морф. анализ.
                        // В случае успеха aot_id != -1, заполняются поля грам. хар-к
                        morphAn(w);

                        if (w.aot_id == -1) {
                            notFoundCount++;
                            notfoundFile.write(w.word);
                            notfoundFile.newLine();
                        }
                        if (!includeNotFoundWords) {
                            continue;
                        }

                        //добавить текущее слово в группу
                        wordList.add(w);

                    } catch (InvalidInputException e) {
                        invCount++;
                        logfile.write(e.getMessage());
                        logfile.newLine();
                    }
                }

            } //while

            if (createRootsFiles) {
                 //создать файлы с наборами чередующихся корней
                createRootsFiles(rootsList, rootsPartsList);
            }

            logStatistics(counter, invCount, notFoundCount,
                                includeNotFoundWords ? invCount : (invCount + notFoundCount),
                                nounCount, verbCount, adjCount, advCount, groupCount);
            logfile.close();
            notfoundFile.close();
            inputFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            throw new SQLException(w.splitWord);
        }
    }



    private WordFromFile makeWordFromFormattedFile(String s) throws SQLException, InvalidInputException {
        WordFromFile w = new WordFromFile();
        StringTokenizer tok = new StringTokenizer(s);
        try {
            w.word = tok.nextToken();
            w.splitWord = tok.nextToken();
            w.part = new Integer(tok.nextToken());
        } catch (NoSuchElementException e) {
            throw new InvalidInputException(s);
        }
        addMorphs(w);
        return w;
    }



    private void addMorphs(WordFromFile w) throws SQLException, InvalidInputException {
        w.morphList = new ArrayList<Morph>();
        char[] str = w.splitWord.toCharArray();
        int len = str.length;
        int i = 0;
        int type;
        String strmorph;

        while (i < len) {
            strmorph = "";
            switch (str[i]) {
                case'-':
                    type = GramDecoder.PREF;
                    i++;
                    while ((i < len) && Character.isLetter(str[i])) {
                        strmorph = strmorph + str[i];
                        i++;
                    }
                    addMorph(strmorph, w, type);
                    break;
                case'+':
                    type = GramDecoder.ROOT;
                    i++;
                    while ((i < len) && Character.isLetter(str[i])) {
                        strmorph = strmorph + str[i];
                        i++;
                    }
                    addMorph(strmorph, w, type);
                    break;
                case'^':
                    type = GramDecoder.SUFF;
                    i++;
                    while ((i < len) && Character.isLetter(str[i])) {
                        strmorph = strmorph + str[i];
                        i++;
                    }
                    addMorph(strmorph, w, type);
                    break;
                case'*':
                    type = GramDecoder.ENDING;
                    i++;
                    while ((i < len) && Character.isLetter(str[i])) {
                        strmorph = strmorph + str[i];
                        i++;
                    }
                    addMorph(strmorph, w, type);
                    break;
                default:
                    throw new InvalidInputException("Word not correctly marked: " + w.splitWord);
            }
        }
    }

    private void morphAn(WordFromFile w) throws MorphAnException {
        boolean found;
        ArrayList<Paradigm> paradigms;
        paradigms = MorphAn.analyzeNorm(w.word);
        found = trySetGramFields(w, paradigms);
        if (!found) {
            paradigms = MorphAn.analyzeForm(w.word);
            found = trySetGramFields(w, paradigms);
            if (found && (w.part == GramDecoder.NOUN)) {
                w.num = GramDecoder.PLUR;
            }
        }


    }

    private boolean trySetGramFields(WordFromFile w, ArrayList<Paradigm> paradigms) {
        for (Paradigm p : paradigms) {
                if (w.part == p.part) {
                    w.aot_id = p.aot_id;

                    if (p.params.isEmpty() && p.paramForms.isEmpty()) {
                        //слово с пустой строкой грам. признаков
                        return true;
                    }

                    //определить грам. параметры
                    if (MorphAn.decParam(w, p.params + (p.paramForms.isEmpty() ? "" : p.paramForms.get(0)))) {
                        //если одна успешная парадигма найдена, остальные не проверяются
                        return true;
                    }
                }
        }
        w.aot_id = -1;
        return false;
    }




    private HashSet<Integer> addWordsToDB(ArrayList<WordFromFile> list) throws SQLException, IOException {
        HashSet<Integer> rootsSet = new HashSet<Integer>();
        int id_m;


        //считается, что список уже проверен на отсутствие дубликатов в бд
        for (WordFromFile w : list) {

            //add word and morphs
            db.update("INSERT INTO words(word, part) " +
                    "VALUES('" + w.word + "'," + w.part + ")");

            w.id = db.queryAndGetId("call identity()");

            for (int k = 0; k < w.morphList.size(); k++) {
                id_m = w.morphList.get(k).id_m;
                db.update("insert into words_morphs (id, n, id_m)" +
                        "values (" + w.id + "," + k + "," + id_m + ")");
                if (w.morphList.get(k).type == GramDecoder.ROOT) {
                    rootsSet.add(id_m);
                }
            }
            if (w.aot_id != -1) {
                db.update("update words set aot_id = " + w.aot_id + "where id =" + w.id);
            }
            if (w.numflag != WordFromFile.NOTSET) {
                db.update("update words set num = " + w.num + "where id =" + w.id);
            }
            if (w.genflag != WordFromFile.NOTSET) {
                db.update("update words set gen = " + w.gen + "where id =" + w.id);
            }
            if (w.animflag != WordFromFile.NOTSET) {
                db.update("update words set anim = " + w.anim + "where id =" + w.id);
            }
        }

        return rootsSet;
    }

    private void logStatistics(int counter, int invCount, int notfoundCount, int skipCount,
                               int nounCount, int verbCount,
                               int adjCount, int advCount, int groupCount) throws IOException {

        logfile.newLine();
        logfile.write("СТАТИСТИКА");
        logfile.newLine();
        logfile.write("Всего групп в файле: " + groupCount);
        logfile.newLine();
        logfile.write("Всего слов в файле: " + counter);
        logfile.newLine();
        logfile.write("Существительных : " + nounCount);
        logfile.newLine();
        logfile.write("Глаголов: " + verbCount);
        logfile.newLine();
        logfile.write("Прилагательных: " + adjCount);
        logfile.newLine();
        logfile.write("Наречий: " + advCount);
        logfile.newLine();
        logfile.write("Некорректных слов (дублирующихся): " + invCount);
        logfile.newLine();
        logfile.write("Слов, не найденных морф. анализатором: " + notfoundCount);
        logfile.newLine();
        logfile.write("Слов, не включенных в базу: " + skipCount);
        logfile.newLine();
    }

    private void logStatistics(int counter, int invCount) throws IOException {

        logfile.newLine();
        logfile.write("СТАТИСТИКА");
        logfile.newLine();
        logfile.write("Всего слов в файле: " + counter);
        logfile.newLine();
        logfile.write("Некорректных слов: " + invCount);
        logfile.newLine();
    }

    private void loadParonymsFromFile(String inputFileName, boolean includeNotFoundWords, boolean allowSingPlurFormsMix )
            throws Exception {
        inputFile = new BufferedReader(new FileReader(inputFileName));
        ResultSet rs;
        String s;
        int counter = 0;
        StringTokenizer tok;
        String word;
        int part;
        int id;
        WordFromFile w;
        WordFromFile w1;
        WordFromFile w2;
        ArrayList<WordFromFile> wordList = new ArrayList<WordFromFile>();
        int dmcode;
        double q;

        System.out.println("Loading paronyms from file " + inputFileName);

        while ((s = inputFile.readLine()) != null) {
            try {
                counter++;
                if (counter % 10000 == 0) {
                    System.out.println(counter);
                }


                if (s.isEmpty()) { //конец группы
                    if (!wordList.isEmpty()) {
                        //сопоставить каждый с каждым
                        for (int i = 0; i < wordList.size(); i++) {
                            w1 = wordList.get(i);
                            for (int j = 0; j < i; j++) {
                                w2 = wordList.get(j);
                                if (!allowSingPlurFormsMix && (w1.num != w2.num) && (w1.num != -1) && (w2.num != -1)) {
                                    continue;
                                }
                                dmcode = getDmCode(w1, w2, Integer.MAX_VALUE, Integer.MAX_VALUE);
                                if (dmcode != -1) {
                                    q = Distance.countFormula(w1.word, w2.word, Distance.countDistance(w1.word, w2.word, false));
                                    if (!db.queryResult("select * from paronyms where " +
                                            "(id1=" + w1.id + ") and(id2=" + w2.id + ") union " +
                                            "select * from paronyms where (id1=" + w2.id + ") and(id2=" + w1.id + ")").next()
                                            ) {
                                      
                                        db.update("INSERT INTO paronyms(id1,id2,dmcode,q) " +
                                            "VALUES(" + w1.id + "," + w2.id + "," + dmcode + "," + q + ")");

                                    }

                                }
                            }
                        }
                        wordList.clear();
                    }

                } else {

                    tok = new StringTokenizer(s);
                    word = tok.nextToken();
                    tok.nextToken();
                    part = new Integer(tok.nextToken());

                    rs = db.queryResult
                            ("select * from words where (word = '" + word + "')and(part = " + part + ")");
                    if (!rs.next()) {
                        continue;
                    }
                    id = rs.getInt("id");
                    w = new WordFromFile(id, word, part, Query.constructMorphList(db, id));
                    w.aot_id = rs.getObject("aot_id") == null ? -1 : rs.getInt("aot_id");
                    w.num = rs.getObject("num") == null ? -1 : rs.getInt("num");

                    if (!includeNotFoundWords && (w.aot_id == -1)) {
                        continue;
                    }

                    //добавить текущее слово в группу
                    wordList.add(w);
                }
            } catch (NoSuchElementException e) {
            }

        } //while



    }

    private void createRootsFiles(ArrayList<HashSet<Integer>> rootsList, ArrayList<Integer> rootsPartsList) throws IOException, SQLException {

        BufferedWriter rootsFile = new BufferedWriter(new FileWriter("outputFiles\\roots_groups.txt"));
        BufferedWriter rootsIdFile = new BufferedWriter(new FileWriter("inputFiles\\rootsId_groups.txt"));
        ResultSet rs;
        double counter = 0; //для подсчета среднего числа корней в группе
        int i = 0;
        int part;

        for (HashSet<Integer> set : rootsList) {
            counter += set.size();
            part = rootsPartsList.get(i);
            rootsFile.write(part + " ");
            rootsIdFile.write(part + " ");
            i++;
            for (int id : set) {
                rs = db.queryResult("select * from morphs where id_m =" + id);
                rs.next();
                //первое число в строке - часть речи
                rootsFile.write(rs.getString("morph") + " ");
                rootsIdFile.write(id + " ");
            }
            rootsFile.newLine();
            rootsIdFile.newLine();
        }
        counter /= rootsList.size();
        rootsFile.write("Среднее кол-во черед. корней в группе: " + counter);

        rootsFile.flush();
        rootsFile.close();
        rootsIdFile.flush();
        rootsIdFile.close();
    }

    private boolean wordIsIn(WordFromFile w, ArrayList<WordFromFile> parlist) {
        for (WordFromFile word : parlist) {
            if (word.word.equals(w.word)) {
                return true;
            }
        }
        return false;
    }

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


    private void setPosForGroup(ArrayList<WordFromFile> parlist, int groupPart) throws SQLException {

        for (WordFromFile wrd : parlist) {
            wrd.part = groupPart;
//            db.update("update words set part = " + groupPart + " where id = " + wrd.id);   //@todo ?????
        }
    }

    WordFromFile makeWordFromFile(String s, int part) throws InvalidInputException, SQLException {

        int len = s.length();
        char[] str = s.toCharArray();
        WordFromFile w = new WordFromFile();

        boolean pref = true; //определяет, как интерпретировать знак '-' (до корня - префикс, после - суффикс)
        int i = 0; //номер позиции

        //для проверки правильности разбора
        int rootcount = 0;
        int endcount = 0;
        int prefcount = 0;
        int suffcount = 0;
        boolean suffAfterEnd = false;

        // morph parser
        try {
            while (i < len) {

                switch (str[i]) {
                    case'-':
                        if (pref) {
                            prefcount++;
                            i = getMorph(i + 1, str, w, GramDecoder.PREF, false);
                        } else if (suffAfterEnd) {
                            str[i] = '^';
                            suffcount++;
                            i = getMorph(i + 1, str, w, GramDecoder.SUFF, true);
                        } else {
                            str[i] = '^';
                            suffcount++;
                            i = getMorph(i + 1, str, w, GramDecoder.SUFF, false);
                        }

                        break;

                    case'+':
                        pref = false;
                        rootcount++;
                        i = getMorph(i + 1, str, w, GramDecoder.ROOT, false);
                        break;
                    case'*':
                        suffAfterEnd = true;
                        endcount++;
                        i = getMorph(i + 1, str, w, GramDecoder.ENDING, false);
                        break;
                    case'<':
                        //@todo случай неправильных вх. данных (возможен выход за гр. массива)
                        switch (str[i + 1]) {
                            case's':
                                w.part = 0;
                                break;
                            case'v':
                                w.part = 1;
                                break;
                            case'a':
                                if (str[i + 2] == 'j') {
                                    w.part = 2;
                                } else {
                                    w.part = 3;
                                }
                                break;
                            default:
                                throw new InvalidInputException("Unknown part of speech: " + str);
                        }
                        len = i;
                        break;
                    default:
                        if (i == 0) {
                            //означает, что слово не полностью разобрано
                            throw new InvalidInputException("Word not correctly marked: ");
                        }
                }


                i++;
            }

            if (part == GramDecoder.ADVERB) {
                //для корректной проверки разбора наречий
                w.part = part;
            }
            checkMorphCount(w.part, prefcount, rootcount, suffcount, endcount);


            char[] splitWord = new char[s.length()];
            int j = 0;
            for (char c : str) {
                if (Character.isDigit(c)) {
                    continue;
                }
                if (c == '<') {
                    break;
                }
                splitWord[j] = c;
                j++;

            }


            w.splitWord = new String(splitWord, 0, j);
        } catch (InvalidInputException e) {
            throw new InvalidInputException(e.getMessage() + s);
        }


        return w;
    }

    /**
     * считывает морф. цифры игнорируются.
     * добавляет его к полю word текущего слова.
     * <p/>
     * вариант 1 (проверяет, есть ли морф в таблице морфов. если нет - бросает исключение)
     * вариант 2(подразумевается, что исходный файл содержит только правильные морфы.
     * добавляет новый морф в таблицу морфов, если его там еще нет)
     * добавляет Id морфа в список morphList текущего слова
     *
     * @param i            текущая позиция в массиве str
     * @param str          массив, содержащий текущее слово
     * @param w            текущее слово
     * @param type         тип считываемого морфа
     * @param suffAfterEnd является ли считываемый морф суффиксом, следующим после окончания
     * @return номер последней позиции считанного морфа
     * @throws InvalidInputException неправильный разбор слова
     * @throws SQLException          ошибка при работе с базой данных
     */
    int getMorph(int i, char[] str,  WordFromFile w, int type, boolean suffAfterEnd) throws InvalidInputException {

        String strmorph;

        int len = str.length;
        int j = 0;

        //@todo не проверяется, что буква кириллическая!!!
        while ((i < len) && (Character.isLetter(str[i]))) {

            i++;
            j++;
        }
        strmorph = new String(str, i - j, j);

        //суффикс, следующий после окончания. Мб только "сь" или "ся"
        if (suffAfterEnd && (!strmorph.equals("сь")) && (!strmorph.equals("ся"))) {
            throw new InvalidInputException("Suffix after ending. " + strmorph + " ");
        }

        // проверка морфа на правильность
        checkMorph(strmorph, type);

        w.word = w.word.concat(strmorph);  //поэтапное заполнение;

        return i - 1;  //return position of the last symbol in morph
    }

    private void addMorph(String strmorph, WordFromFile w, int type) throws SQLException {
        int id;

        if ((id = db.queryAndGetId("select * from morphs where " +
                    "(morph='" + strmorph + "')and(type=" + type + ")")) == -1) {

                //в таблице морфов такого еще нет - добавить.
                db.update("INSERT INTO morphs(morph,type) VALUES ('" + strmorph + "'," + type + ")");
                id = db.queryAndGetId("call identity()");
            }
        w.morphList.add(new Morph(id, type, strmorph));
    }

    private void checkMorph(String morph, int type) throws InvalidInputException {
        int l = morph.length();
        switch (type) {
            case GramDecoder.PREF:
                if ((l > 7) || (l < 1)) {
                    throw new InvalidInputException("CheckMorph. Prefix empty or longer than 7 symbols: " + morph + " in ");
                }
                break;
            case GramDecoder.ROOT:
                if ((l > 10)|| (l < 1)) {
                    throw new InvalidInputException("" +
                            "CheckMorph. Root empty or longer than 10 symbols: " + morph + " in ");
                }
                break;
            case GramDecoder.SUFF:
                if ((l > 5)|| (l < 1)) {
                    throw new InvalidInputException("CheckMorph. Suffix empty or longer than 5 symbols: " + morph + " in ");
                }
                break;
            case GramDecoder.ENDING:
                break;

        }
    }

    private void checkMorphCount(int part, int pref, int root, int suff, int end) throws InvalidInputException {
        if (root != 1) {
            throw new InvalidInputException("Too many/few roots: " + root + " ");
        } else if ((part == GramDecoder.ADVERB) ? (end != 0) : (end != 1)) {
            throw new InvalidInputException("Too many/few endings: " + end + " ");
        } else if (pref > 3) {
            throw new InvalidInputException("Too many prefixes" + pref + " ");
        } else if (suff > 6) {
            throw new InvalidInputException("Too many suffixes" + suff + " ");
        }
    }





   
   //заполняет таблицу ROOTS данными о чередующихся корнях. Данные берутся из файла с именем rootsIdFileName
    public void fillRootsTable(String rootsIdFileName) throws IOException, SQLException {
        BufferedReader f = new BufferedReader(new FileReader(rootsIdFileName));
        String s;
        Scanner scan;
        ArrayList<Integer> group = new ArrayList<Integer>();
        int id1;
        int part;

       System.out.println("Filling ROOTS table");
        while ((s = f.readLine()) != null) {
            scan = new Scanner(s);
            //первое число в строке - часть речи
            part = scan.nextInt();
            while (scan.hasNextInt()) {
                id1 = scan.nextInt();
                for (int id2 : group) {
                    if (!db.queryResult("select * from roots where (id1 =" + id1 + ")and(id2=" + id2 + ")and(part=" + part + ")" +
                            "union select * from roots where (id1 =" + id2 + ")and(id2=" + id1 + ")and(part=" + part + ")").next()) {

                        db.update("insert into roots (id1, id2, part) values (" + id1 + "," + id2 + "," + part + ")");
                    }
                }
                group.add(id1);
            }
            group.clear();
        }
    }



    private void addWordsFromDb(String addDbName) throws Exception {
        try {
            Database addDb = new Database(addDbName);
            ResultSet rs;
            int part;
            String word;

            System.out.println("Adding words from additional database " + addDbName);
            rs = addDb.queryResult("select * from words");
            while (rs.next()) {
                part = rs.getInt("part");
                word = rs.getString("word");
                if (!db.queryResult("select * from words where (word = '" + word + "')and(part= " + part + ")").next()) {
                    db.update("insert into words (word,part) values ('" + word + "'," + part + ")");
                }
            }

            addDb.shutdown();

        } catch (SQLException ex) {
            System.out.println("could not finish addWordsFromDb");
            ex.printStackTrace();
            throw ex;
        }


    }

    //вставка в таблицу paronyms отношений между словами с чередующимися корнями
    public void insertRootsDistances() throws Exception {
        ResultSet rs;
        ResultSet rs2;
        ResultSet rswords;
        ArrayList<Integer> rslist = new ArrayList<Integer>(); //слова с корнем rootId1
        ArrayList<WordFromFile> wordList = new ArrayList<WordFromFile>();
        int rootId1;
        int rootId2;
        WordFromFile word1;
        WordFromFile word2;
        int part;
        int id;
        int id1;
        int id2;
        int dmcode;
        double q;
        int counter = 0;

        System.out.println("Adding ROOTS Distances");
        rs = db.queryResult("select * from roots");
        while (rs.next()) {
            counter++;
            if (counter % 500 == 0) {
                System.out.println(counter);
            }
            rslist.clear();
            wordList.clear();

            rootId1 = rs.getInt("id1");
            rootId2 = rs.getInt("id2");
            part = rs.getInt("part");

            rswords = db.queryResult("select * from words_morphs wm " +
                    "join words w on w.id = wm.id " +
                    "where (wm.id_m = " + rootId1 + ")and(w.part=" + part + ")");

            while (rswords.next()) {
                id = rswords.getInt("id");
                wordList.add(new WordFromFile
                        (id, rswords.getString("word"),
                                rswords.getInt("part"),Query.constructMorphList(db, id)));
            }

            rswords = db.queryResult("select * from words_morphs wm " +
                    "join words w on w.id = wm.id " +
                    "where (wm.id_m = " + rootId2 + ")and(w.part=" + part + ")");
            while (rswords.next()) {
                id = rswords.getInt("id");
                wordList.add(new WordFromFile
                        (id, rswords.getString("word"),
                                rswords.getInt("part"),Query.constructMorphList(db, id)));
            }

            for (int i = 0; i < rslist.size(); i++) {
                for (int j = i+1; j < rslist.size(); j++) {
                    word1 = wordList.get(i);
                    word2 = wordList.get(j);

                    dmcode = getDmCode(word1, word2, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    if (dmcode != -1) {
                        q = Distance.countFormula(word1.word, word2.word, Distance.countDistance(word1.word, word2.word, false));

                        if (!(rs2 = db.queryResult("select * from paronyms where " +
                                "(id1=" + word1.id + ") and(id2=" + word2.id + ") union " +
                                "select * from paronyms where (id1=" + word2.id + ") and(id2=" + word1.id + ")")).next()) {

                            //добавить в таблицу новую строчку
                            db.update("INSERT INTO paronyms(id1,id2,dmcode, q) VALUES" +
                                    "(" + word1.id + "," + word2.id + "," + dmcode + "," + q + ")");
                        } else {
                            //Добавить в существующую строку значение dm
                            id1 = rs2.getInt("id1");
                            id2 = rs2.getInt("id2");
                            db.update("update paronyms set dmcode = " + dmcode + ",q = " + q +
                                    " where (id1=" + id1 + ")and(id2=" + id2 + ")");
                        }
                    }
                }
            }
        }
    }

}
