package db;

import aot.Paradigm;
import aot.MorphAn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.HashSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;

import lingv.GramDecoder;
import lingv.Distance;
import static lingv.GramDecoder.PREF;
import static lingv.GramDecoder.ROOT;
import static lingv.GramDecoder.SUFF;
import static lingv.GramDecoder.ENDING;
import db.WordFromFile;
import dict.Morph;

/**
 * Created by IntelliJ IDEA. User: Таня Date: 01.04.2009 Time: 12:15:31 To
 * change this template use File | Settings | File Templates.
 */
public class ParonymFileLoader {
    Database db;

    final boolean ADD_PARONYMS = true;
    final boolean DO_NOT_ADD_PARONYMS = false;
    final boolean CREATE_ROOTS_FILES = true;
    final boolean DO_NOT_CREATE_ROOTS_FILES = false;
    final boolean INCLUDE_NOT_FOUND_WORDS = true;
    final boolean DO_NOT_INCLUDE_NOT_FOUND_WORDS = false;
    final boolean ALLOW_SING_PLUR_FORMS_MIX = true;
    final boolean DO_NOT_ALLOW_SING_PLUR_FORMS_MIX = false;

    public void init(String dbName) throws Exception {
        db = new Database(dbName);
    }

    public void unInit() throws Exception {
        MorphAn.unInit();
        db.shutdown();
    }

    public void loadCodes(String codesFileName) throws Exception {
        BufferedReader codesFile = new BufferedReader(new FileReader(
                codesFileName));
        String s;
        StringTokenizer tok;
        String tableName;
        int code;
        String str;

        System.out.println("Loading Codes");
        try {
            while ((s = codesFile.readLine()) != null) {
                tok = new StringTokenizer(s);
                tableName = tok.nextToken();
                code = new Integer(tok.nextToken());
                str = tok.nextToken();
                db.update("insert into " + tableName + "(code,str) values ("
                        + code + ",'" + str + "')");
            }

        } catch (NoSuchElementException e) {
            System.out.println("Неправильный формат файла " + codesFileName);
            throw e;
        }
    }

    /*
     * removes words in plural forms, leaving only that don't have singular form
     * работает только с существительными. часть речи должна быть заранее
     * определена removes paronyms and words_morphs containing removed words
     * puts removed words into file (logfile)
     */
    public void removePluralNouns() throws Exception {
        ResultSet rs;
        int id;
        String word;
        int part;
        String gramStr;

        Boolean good;
        int counter = 0;

        BufferedWriter logfile = new BufferedWriter(new FileWriter(
                "removePlural.txt"));

        System.out.println("Removing plural nouns");
        try {
            // for all words
            rs = db.queryResult("select * from words"); // @todo maybe too large
                                                        // ResultSet
            while (rs.next()) {
                word = rs.getString("word");
                part = rs.getInt("part");

                if (part != GramDecoder.NOUN) {
                    // не сущ. не рассматриваются
                    continue;
                }
                if (MorphAn.analyzeAsNounNorm(word) == null) {
                    // remove word from db
                    id = rs.getInt("id");
                    db.update("delete from paronyms where id1=" + id);
                    db.update("delete from paronyms where id2=" + id);
                    db.update("delete from words_morphs where id=" + id);
                    db.update("delete from words where id=" + id);
                    logfile.write(word);
                    logfile.newLine();
                    counter++;
                }

            }
            logfile.write("Всего выброшено слов: " + counter);
            logfile.newLine();
            logfile.close();
        } catch (SQLException ex) {
            System.out.println("could not finish removePluralNouns");
            ex.printStackTrace();
            throw ex;
        }
    }

    // осуществляет подсчет буквенных и морфемных расстояний методом
    // сопоставления «каждого с каждым»
    public void addAllParonyms(int dlLimit, int dmLimit, int drLimit)
            throws Exception {
        ResultSet rs;
        boolean found = false;
        int counter = 0;
        ArrayList<WordFromFile> wordList = new ArrayList<WordFromFile>();

        WordFromFile word1;
        WordFromFile word2;

        int id1;
        int id2;
        int id;
        int num;

        double q = -1;
        int dl;

        byte dlcode;
        int dmcode;

        System.out.println("Adding new paronyms");
        // db.update("delete from paronyms");
        rs = db.queryResult("select * from words");
        while (rs.next()) {
            id = rs.getInt("id");
            num = rs.getObject("num") == null ? -1 : rs.getInt("num");
            wordList.add(new WordFromFile(id, rs.getString("word"), rs
                    .getInt("part"), num, Query.constructMorphList(db, id)));
        }
        rs.close();
        for (int i = 0; i < wordList.size(); i++) {
            db.execute("commit");
            for (int j = i + 1; j < wordList.size(); j++) {
                counter++;
                if (counter % 100000000 == 0) { // 100 миллионов
                    System.out.println(counter);
                }
                word1 = wordList.get(i);
                word2 = wordList.get(j);

                id1 = word1.id;
                id2 = word2.id;

                if ((word1.num != word2.num) && (word1.num != -1)
                        && (word2.num != -1)) {
                    continue;
                }

                // dl
                dlcode = getDlCode(word1, word2, dlLimit);

                // dm
                dmcode = getDmCode(word1, word2, dmLimit, drLimit);

                // q
                if ((dlcode != -1) || (dmcode != -1)) {
                    dl = dlcode == -1 ? Distance.countDistance(word1.word,
                            word2.word, false) : ParonymCoder.getDl(dlcode);
                    q = Distance.countFormula(word1.word, word2.word, dl);

                    found = db.queryResult(
                            "select * from paronyms where " + "(id1=" + id1
                                    + ") and(id2=" + id2 + ") union all "
                                    + "select * from paronyms where " + "(id1="
                                    + id2 + ") and(id2=" + id1 + ")").next();
                }

                if (dlcode != -1) {
                    if (!found) {
                        db.update("insert into paronyms (id1,id2,dlcode, q) values "
                                + "("
                                + id1
                                + ","
                                + id2
                                + ","
                                + dlcode
                                + ","
                                + q + ")");
                    }
                }
                if (dmcode != -1) {
                    if (!found) {
                        db.update("insert into paronyms (id1,id2,dmcode, q) values "
                                + "("
                                + id1
                                + ","
                                + id2
                                + ","
                                + dmcode
                                + ","
                                + q + ")");
                    }
                }

            }
        }

    }

    int getDmCode(WordFromFile word1, WordFromFile word2, int dmLimit,
            int drLimit) throws Exception {
        if (dmLimit < 1) {
            return -1;
        }

        int dm = -1;
        int dr = -1; // буквенное расстояние между корнями
        boolean relRoots = false;
        boolean pref = false;
        boolean root = false;
        boolean suff = false;
        boolean end = false;
        Morph root1;
        Morph root2;
        ArrayList<Morph> morphList1;
        ArrayList<Morph> morphList2;
        HashSet<Integer> diffMorphTypes;
        ArrayList<ArrayList<Morph>> diff;

        morphList1 = word1.morphList;
        morphList2 = word2.morphList;

        if (!morphList1.isEmpty() && !morphList2.isEmpty()) {
            root1 = word1.root;
            root2 = word2.root;
            dr = Distance.countDistance(root1.morph, root2.morph, false);
            if (dr <= drLimit) {
                dm = Distance.countDistance(morphList1, morphList2, false);
            } else {
                dm = -1;
            }
            if ((dm > dmLimit) || (dm < 1)) {
                dm = -1;
            }
            if (dm != -1) {
                // вычислить остальные параметры, связанные с морфемным составом
                root = !root1.morph.equals(root2.morph);
                relRoots = relativeRoots(root1, word1.part, root2, word2.part);
                diff = Distance.getDiffer(morphList1, morphList2);
                diffMorphTypes = getDifferMorphTypes(diff);
                pref = diffMorphTypes.contains(PREF);
                suff = diffMorphTypes.contains(SUFF);
                end = diffMorphTypes.contains(ENDING);
            }

        }
        return ParonymCoder.generateDmCode(dm, dr, pref, root, relRoots, suff,
                end);
    }

    byte getDlCode(WordFromFile word1, WordFromFile word2, int dlLimit)
            throws ParonymCodeException {
        if (dlLimit < 1) {
            return -1;
        }

        int dl = -1;
        int dl2 = -1;

        dl = Distance.countDistance(word1.word, word2.word, false);
        dl2 = Distance.countDistance(word1.word, word2.word, true);
        if ((dl2 > dlLimit) || (dl2 < 1)) {
            dl = -1;
        }
        return ParonymCoder.generateDlCode(dl, dl2);
    }

    private boolean relativeRoots(Morph root1, int part1, Morph root2, int part2)
            throws SQLException {
        int rootId1 = root1.id_m;
        int rootId2 = root2.id_m;
        return db.queryResult(
                "select * from roots where " + "(id1=" + rootId1 + ") and(id2="
                        + rootId2 + ")and(part=" + part1 + ") union "
                        + "select * from roots where (id1=" + rootId2 + ") "
                        + "and(id2=" + rootId1 + ")and(part=" + part2 + ")")
                .next();
    }

    private HashSet<Integer> getDifferMorphTypes(
            ArrayList<ArrayList<Morph>> diff) throws Exception {
        Morph m;
        HashSet<Integer> types = new HashSet<Integer>();

        if (!diff.isEmpty()) {
            for (ArrayList<Morph> mm : diff) {
                for (int j = 0; j < 2; j++) {
                    m = mm.get(j);
                    if ((m == null) || (m.type == ROOT)) {
                        continue;
                    }
                    types.add(m.type);
                }
            }
        }
        return types;
    }

    // fills gram columns in db.words using aot
    public void fillGramFields() throws Exception {
        ResultSet rs;
        BufferedWriter file = new BufferedWriter(new FileWriter(
                "addGram_WordsNotFound.txt"));
        boolean found;
        int counter = 0;
        int notFoundCounter = 0;

        WordFromFile w;
        String word;
        int part;
        int id;

        ArrayList<Paradigm> paradigms;

        System.out.println("Filling grammatical parameters");

        rs = db.queryResult("select  * from words");

        // for each word
        while (rs.next()) {
            counter++;
            if (counter % 10000 == 0) {
                System.out.println(counter);
            }
            word = rs.getString("word");
            part = rs.getInt("part");
            id = rs.getInt("id");

            w = new WordFromFile(id, word, part);

            // try to find word as Norm
            paradigms = MorphAn.analyzeNorm(word);
            found = trySetGramFields(w, paradigms);

            // try to find word as Form if not found yet
            if (!found) {
                paradigms = MorphAn.analyzeForm(word);
                found = trySetGramFields(w, paradigms);
            }
            if (!found) {
                // word with these gram values is not found
                notFoundCounter++;

                // remove it
                db.update("delete from paronyms where id1=" + id);
                db.update("delete from paronyms where id2=" + id);
                db.update("delete from words_morphs where id=" + id);
                db.update("delete from words where id=" + id);

                // add to log
                file.write(word);
                file.newLine();

            }
        }
        file.write("Всего не найденных АОТом слов: " + notFoundCounter);
        file.newLine();
        file.flush();
        file.close();
    }

    private boolean trySetGramFields(WordFromFile w,
            ArrayList<Paradigm> paradigms) throws SQLException {
        boolean found = false;

        for (Paradigm p : paradigms) {
            if (w.part == p.part) {
                w.aot_id = p.aot_id;
                db.update("update words set aot_id = " + w.aot_id
                        + "where id =" + w.id);

                if (p.params.isEmpty() && p.paramForms.isEmpty()) {
                    // слово с пустой строкой грам. признаков
                    found = true;
                    break;
                }
                if (MorphAn.decParam(w, p.params
                        + (p.paramForms.isEmpty() ? "" : p.paramForms.get(0)))) { // false,
                                                                                  // если
                                                                                  // был
                                                                                  // конфликт
                                                                                  // параметров

                    if (w.numflag != WordFromFile.NOTSET) {
                        db.update("update words set num = " + w.num
                                + "where id =" + w.id);
                    }
                    if (w.genflag != WordFromFile.NOTSET) {
                        db.update("update words set gen = " + w.gen
                                + "where id =" + w.id);
                    }
                    if (w.animflag != WordFromFile.NOTSET) {
                        db.update("update words set anim = " + w.anim
                                + "where id =" + w.id);
                    }
                    // если одна успешная парадигма найдена, остальные не
                    // проверяются
                    found = true;
                    break;
                } else {
                    // restore default gram values and try next paradigm
                    w = new WordFromFile(w.id, w.word, w.part);
                }
            }
        }
        return found;
    }

    public static void main(String[] args) {
        ParonymFileLoader loader = new ParonymFileLoader();

        try {
            loader.init("db\\mdb\\mdb");
            ArrayList<Morph> list = Query.constructMorphList(loader.db, 20531);
            loader.unInit();
        } catch (Exception e) {

        }

    }

}
