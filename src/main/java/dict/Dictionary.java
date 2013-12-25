package dict;

import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.sql.ResultSet;
import java.sql.SQLException;

import aot.MorphAn;
import aot.Paradigm;
import db.Database;
import db.Query;
import db.ParonymCoder;
import lingv.GramDecoder;
import lingv.Distance;


public class Dictionary implements DictionaryInterface {
    Database db;
    final static QueryParameters DEFAULT_QUERY_PARAMETERS =
            new QueryParameters(-1,-1,-1,0.3,false,true,false,false,false,true,true,true,true,true,true,true,0);

    public Dictionary(Database d) {
        db = d;
    }

    //обработка запроса к словарю
    //действие выбирается в зависимости от набора параметров
    public ArrayList<ParonymList> executeQuery(String word, QueryParameters queryParams) throws Exception {
        if (queryParams.enableForms) {
            return findWordFormParonyms(word, queryParams);
        } else {
            return findWordNormParonyms(word, queryParams);
        }
    }

    //поиск паронимов для слова по его нормальной форме
    public ArrayList<ParonymList> findWordNormParonyms(String word, QueryParameters queryParams) throws Exception {
        ArrayList<ParonymList> paronymLists = new ArrayList<ParonymList>();
        ResultSet rs;
        boolean found = false; //true, если заданное слово найдено в бд
        Word w;
        String notAot = queryParams.includeNotFound ? "" : "and(aot_id is not null)";
        String setPart = queryParams.setPart == -1 ? "" : "and(part = " + queryParams.setPart + ")";

        rs = db.queryResult(
                "select * from words where (word ='" + word + "') " + notAot + setPart);
        while (rs.next()) {
            found = true;
            w = makeWord(rs, "", true, true);
            paronymLists.add(new ParonymList(w, findParonyms(w, queryParams)));
        }
        if (!found) {
            throw new ParonymsNotFoundException("Слово «" + word + "» не найдено");
        }
        return paronymLists;
    }


    /**
     * поиск паронимов по бд для заданной словоформы word
     * с помощью аот для словоформы находятся все варианты нормальных форм,
     * для каждой нормальной формы - все варианты наборов граммем
     * (например: пекло. Норм. формы: печь(гл) ; пекло(сущ): им.п., вин.п.)
     *  для каждого варианта ищутся паронимы по бд
     *  с помощью аот паронимы возвращаются в ту же форму, что и исходное слово
     * @param word  исходная словоформа
     * @param queryParams параметры поиска
     * @return список списков паронимов
     * @throws Exception
     */

    public ArrayList<ParonymList> findWordFormParonyms(String word, QueryParameters queryParams) throws Exception {
        ArrayList<ParonymList> paronymLists = new ArrayList<ParonymList>();
        ResultSet rs;
        Word w;
        ArrayList<Paradigm> paradigms;
        String notAot = queryParams.includeNotFound ? "" : "and(aot_id is not null)";

        paradigms = MorphAn.analyzeForm(word);
        if (paradigms.isEmpty()) {
            throw new ParonymsNotFoundException("Словоформа «" + word + "» не найдена");
        }
        for (Paradigm p : paradigms) {
            //поиск слова в бд по aot_id
            if ((queryParams.setPart != -1) && (p.part != queryParams.setPart)) {
                continue;
            }
            rs = db.queryResult(
                    "select * from words where (aot_id =" + p.aot_id + ")and(word='" + p.norm + "')" +
                            notAot);
            if (!rs.next()) {
                continue;
            }
            //для каждого набора граммем (напр. с различием в падеже)
            // создается свой объект Word и свой список паронимов
            for (String gram : p.paramForms) {
                w = makeWord(rs, gram, true, true);
                paronymLists.add(new ParonymList(w, findParonyms(w, queryParams)));
            }
        }

        return paronymLists;
    }



    //поиск паронимов по бд для слова w
    public ArrayList<Paronym> findParonyms(Word w, QueryParameters queryParams) throws Exception {
        ArrayList<Paronym> paronymList = new ArrayList<Paronym>();
        ResultSet rs;
        Paronym paronym;

        if (!queryParams.enablePlural && (w.num == GramDecoder.PLUR)) {
            return paronymList;
        }

        //дополнительные условия sql-запроса в зависимости от queryParams
        String partQuery = queryParams.part ? "and(part=" + w.part + ")" : "";
        String genQuery = queryParams.gen && (w.gen != -1) ? "and(gen=" + w.gen + ")" : "";
        String numQuery = queryParams.num && (w.num != -1) ? "and(num=" + w.num + ")" : "";

        String notAot = queryParams.includeNotFound ? "" : "and(aot_id is not null)";

        //поиск паронимов
        rs = db.queryResult
                ("select par.*, d.* " +
                        "from paronyms d  " +
                        "join words par on (par.id=d.id2) " +
                        "where (d.id1= " + w.id + ")" + partQuery + genQuery + numQuery  + notAot +

                        "union " +

                        "select par.*, d.* " +
                        "from paronyms d  " +
                        "join words par on (par.id=d.id1) " +
                        "where (d.id2=" + w.id + ")" + partQuery + genQuery + numQuery + notAot);


        //для каждого слова, найденного по бд, сконструировать объект Paronym,
        //если пароним удовлетворяет параметрам поиска, включить его в список
        while (rs.next()) {
            paronym = makeParonym(w, rs, queryParams);
            if (paronym != null) {
                paronymList.add(paronym);
            }
        }
        return paronymList;
    }


    /**
     *
     * @param rs
     * @param gram grammatical form parameters of the initial word
     * @param sameGen
     * @param sameNum
     * @return
     * @throws SQLException
     */
    public Word makeWord(ResultSet rs, String gram, boolean sameGen, boolean sameNum) throws Exception {
        String word;
        int id;
        int gen; //gen
        int num; //num
        int anim; //anim
        int aot_id;
        String splitWord;
        String form;
        int rootId;
        ArrayList<Morph> morphList;

        id = rs.getInt("id");
        aot_id = rs.getObject("aot_id") == null ? -1 : rs.getInt("aot_id");
        word = rs.getString("word");
        gen = rs.getObject("gen") == null ? -1 : rs.getInt("gen");
        num = rs.getObject("num") == null ? -1 : rs.getInt("num");
        anim = rs.getObject("anim") == null ? -1 : rs.getInt("anim");
        morphList = Query.constructMorphList(db, id);
        rootId = getRootId(morphList);
        splitWord = splitToMorphs(morphList);

        //построить форму слова
        if (gram.equals("")) {
            form = word;
        } else {
            if (aot_id == -1) {
                return null;
            } else {
                form = MorphAn.constructForm(aot_id, gram, gramCut(gram), sameGen);
                if (form.equals("")) {
                    return null;
                }
            }
        }
        return new Word
                (word, id, aot_id, splitWord, rs.getInt("part"), gen, num, anim, form, gram, rootId, morphList);

    }

    private int getRootId(ArrayList<Morph> morphIdList) throws Exception {
        if (morphIdList.isEmpty()) {
            return -1;
        }
        for (Morph m : morphIdList) {
            if (m.type == GramDecoder.ROOT) {
                return m.id_m;
            }
        }
        throw new Exception("No root found");
    }

    /**
     * возвращает неполную строку грамм. параметров (выбрасывается род)
     * @param gram
     * @return
     */
    private String gramCut(String gram) {
        String gramCut = "";
        StringTokenizer tok = new StringTokenizer(gram, ",");
        String gr;

        while (tok.hasMoreTokens()) {
            gr = tok.nextToken();
            if (!gr.equals("мр")&&!gr.equals("жр")&&!gr.equals("ср")&&!gr.equals("мр-жр")) {
                gramCut = gramCut.concat(gr + ",");
            }

        }
        return gramCut;
    }

    public Paronym makeParonym(Word qword, ResultSet rs, QueryParameters queryParams) throws Exception {
        Paronym p;

        int num = rs.getObject("num") == null ? -1 : rs.getInt("num");
        if (!queryParams.enablePlural && (num == GramDecoder.PLUR)) {
            return null;
        }
        double q = (rs.getObject("q") == null) ? -1 : rs.getDouble("q");
        byte dlcode = rs.getObject("dlcode") == null ? -1 : rs.getByte("dlcode");
        int dmcode = rs.getObject("dmcode") == null ? -1 : rs.getInt("dmcode");


        ParonymParameters params;

        Word w = makeWord(rs, qword.gram, queryParams.gen, queryParams.num);
        if (w == null) {
            return null;
        }
        params = ParonymCoder.decode(dlcode, dmcode);

        //пересчер ред. расстояния при переходе к словоформам
        if (queryParams.enableForms) {
            params.curDl = Distance.countDistance(qword.form, w.form, queryParams.enableDl2);
            params.q = Distance.countFormula(qword.form, w.form, params.curDl);
        } else {
            params.q = q;
            params.curDl = queryParams.enableDl2 ? params.dl2 : params.dl;
        }



        checkParonymParameters(params, queryParams);

//        System.out.println(w.word);
//        params.dump();
//        System.out.println("");

        //проверка условий для буквенных паронимов
        if (params.l_ok) {
            if (params.q > queryParams.q) {
                params.l_ok = false;
            }
        }


        //проверка условий для морфемных паронимов
         if (params.m_ok) {

             if (params.q > queryParams.q) {
                 params.m_ok = false;
             }
        }
        p = w.toParonym(params);

        return p;
    }

    /**
     * Осуществляет проверку соответствия параметров паронимов параметрам запроса.
     * По результатам проверки в p заполняются поля l_ok и m_ok
     * @param p Параметры паронимической пары
     * @param qp Параметры запроса
     */
    private void checkParonymParameters(ParonymParameters p, QueryParameters qp) {
        //проверка для буквенных паронимов
        p.l_ok = p.dlDef && (qp.maxdl == -1 ||  (p.curDl) <= qp.maxdl);

        //проверка для морфемных паронимов

        //проверить, разобрано ли слово по составу
        p.m_ok = true;

        if (!p.dmDef) {
            p.m_ok = false;
            return;
        }
        if (qp.maxdm != -1) {
            //проверить dm
            if (p.dm > qp.maxdm) {
                p.m_ok = false;
            }
        }

        //проверить dr
        if ((qp.maxdr != -1) && (p.dr > qp.maxdr)) {
            p.m_ok = false;
        }

         //проверить  pref
        if (!qp.diffPref && p.pref) {
            p.m_ok = false;
        }
         //проверить  root
        if (!qp.diffRoot && p.root) {
            if (!p.relRoots) {
                p.m_ok = false;
            } else if (!qp.relativeRoots) {
                p.m_ok = false;
            }
        }
         //проверить  suff
        if (!qp.diffSuff && p.suff) {
            p.m_ok = false;
        }
         //проверить  end
        if ((!qp.diffEnding && p.end)) {
            p.m_ok = false;
        }
    }




    private boolean checkRoot(Word paronym, Word qword, QueryParameters queryParams) throws SQLException {
        int rootId1 = paronym.rootId;
        int rootId2 = qword.rootId;
        if (!queryParams.diffRoot && queryParams.relativeRoots) {
            //если слово не разобрано по составу, его rootId== -1
            if ((rootId1 != rootId2) && (!db.queryResult("select * from roots where " +
                    "(id1=" + rootId1 + ") and(id2=" + rootId2 + ")and(part=" + paronym.part + ") union " +
                    "select * from roots where (id1=" + rootId2 + ") " +
                    "and(id2=" + rootId1 + ")and(part=" + paronym.part + ")").next())) {

                return false;
            }
        }
        return true;
    }

    public String splitToMorphs(ArrayList<Morph> morphIdList){
        String splitWord = "";
        //@todo проверять порядок следования морфов
        for(Morph m: morphIdList) {
            switch (m.type) {
                case 0:
                    splitWord = splitWord.concat("-");
                    break;
                case 1:
                    splitWord = splitWord.concat("+");
                    break;
                case 2:
                    splitWord = splitWord.concat("^");
                    break;
                case 3:
                    splitWord = splitWord.concat("*");
                    break;
            }
            splitWord = splitWord.concat(m.morph);
        }
        return splitWord;
    }

    public ArrayList<CompareForms> executeCompareQuery(String form1, String form2) throws Exception {

        ArrayList<CompareForms> compareList = new ArrayList<CompareForms>();
        ArrayList<Paradigm> paradigms1;
        ArrayList<Paradigm> paradigms2;
        ArrayList<Word> list1;
        ArrayList<Word> list2;
        ResultSet rs;
        int dl;
        int dm;

        paradigms1 = MorphAn.analyzeForm(form1);
        paradigms2 = MorphAn.analyzeForm(form2);

        if (paradigms1.isEmpty() || paradigms2.isEmpty()) {
            compareList.add(new CompareForms(form1,form2));
        } else {

            list1 = composeWordList(paradigms1);
            list2 = composeWordList(paradigms2);

            for (Word w1 : list1) {
                for (Word w2 : list2) {
                    rs = db.queryResult
                            ("select * " +
                                    "from paronyms d " +
                                    "where (d.id1= " + w1.id + ")and(d.id2 = "+ w2.id +")"+
                                    "union " +
                                    "select * " +
                                    "from paronyms d  " +
                                    "where (d.id1=" + w2.id + ")and(d.id2 = "+ w1.id +")");
                    if (rs.next()) {
                        dl = rs.getObject("dl") == null ? -1 : rs.getInt("dl");
                        dm = rs.getObject("dm") == null ? -1 : rs.getInt("dm");
                        compareList.add(new CompareForms(w1, w2, dl, dm));
                    }
                }
            }
        }

        return compareList;
    }

    private ArrayList<Word> composeWordList(ArrayList<Paradigm> paradigms) throws Exception {
        ArrayList<Word> list = new ArrayList<Word>();
        ResultSet rs;
        Word w;

        for (Paradigm p : paradigms) {
            //поиск слова в бд по aot_id
            rs = db.queryResult(
                    "select * from words where (aot_id =" + p.aot_id + ")and(word='" + p.norm + "')");
            if (!rs.next()) {
                continue;
            }
            //для каждого набора граммем (напр. с различием в падеже)
            // создается свой объект Word и свой список паронимов
            for (String gram : p.paramForms) {
                list.add(makeWord(rs, gram, true, true));
            }
        }
        return list;
    }

    //тест на массовые запросы

    public static void massQuery(Dictionary dict) throws Exception {
        Date time1;
        Date time2;
        ResultSet rs;
        int max = 100;
        String[] words = new String[max];


        System.out.println("test1");
        time1 = new Date();

        for (int i = 0; i < max; i++) {
            dict.findWordFormParonyms("вода", DEFAULT_QUERY_PARAMETERS);
        }
        time2 = new Date();
        System.out.println((double)(time2.getTime() - time1.getTime())/1000);
        System.out.println((double)1000 * max / (double)(time2.getTime() - time1.getTime()) + " sigle queries per second");

        System.out.println("test2");
        rs = dict.db.queryResult("select limit 0 " + max + " * from words");
        int i = 0;
        while (rs.next()) {
            words[i] = rs.getString("word");
            i++;
        }
        time1 = new Date();
        for (i = 0; i < max; i++) {
            dict.findWordFormParonyms(words[i], DEFAULT_QUERY_PARAMETERS);
        }
        time2 = new Date();
        System.out.println((double)(time2.getTime() - time1.getTime())/1000);
        System.out.println((double)1000 * max / (double)(time2.getTime() - time1.getTime()) + " average queries per second");

    }

    public static void main(String[] args) throws Exception {
        Database db = new Database("db\\mdb\\mdb");
        Dictionary dict = new Dictionary(db);
        try {
            massQuery(dict);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}
