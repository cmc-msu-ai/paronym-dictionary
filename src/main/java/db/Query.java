package db;

import db.Database;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;

import dict.Morph;

/*
 * Класс, осуществляющий типовые запросы к БД словаря
 */
public class Query {

    //получить из базы данных строковое предст. корня. корень задается своим id_m
    public static String getRoot(Database db, int rootId) throws SQLException {
        String root = "";
        ResultSet rs;

        rs = db.queryResult("select morph from\n" +
                "morphs where id_m =" + rootId);
        if (rs.next()) {
            root = rs.getString("morph");
        }
        return root;
    }

    //возвращает строку - разбор слова по составу
    public static String splitToMorphs(Database db, int id) throws SQLException {
        String splitWord = "";
        ResultSet rs;

        rs = db.queryResult("select m.* from\n" +
                "words w\n" +
                "join words_morphs wm on (w.id=wm.id)\n" +
                "join morphs m on (m.id_m=wm.id_m)\n" +
                "where (w.id=" + id + ")");
        //@todo проверять порядок следования морфов
        while (rs.next()) {
            switch (rs.getInt("type")) {
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
            splitWord = splitWord.concat(rs.getString("morph"));
        }
        return splitWord;

    }


    public static int getRootId(Database db, int id) throws SQLException {

        ResultSet rs = db.queryResult("select m.id_m from\n" +
                "words_morphs wm\n" +
                "join morphs m on (m.id_m=wm.id_m) \n" +
                "where (wm.id =" + id + ")and(m.type=1)");
        return rs.next()? rs.getInt("id_m") : -1;
    }

    //построить список id морфов, составляющих слово (слово задается своим id)
    public static ArrayList<Morph> constructMorphList(Database db, int id) throws SQLException {
        ArrayList<Morph> morphList = new ArrayList<Morph>();
        ResultSet rs;
        int morphId;

        rs = db.queryResult("select * from\n" +
                "words_morphs wm join morphs m on wm.id_m = m.id_m where wm.id =" + id);
        //@todo проверять порядок следования морфов
        while (rs.next()) {
            morphId = rs.getInt("id_m");
            morphList.add(new Morph(morphId, rs.getInt("type"), rs.getString("morph")));
        }


        return morphList;
    }

    public static int getMorphType(Database db, int morphId) throws SQLException {
        ResultSet rs;
        rs = db.queryResult("select type from morphs where id_m = " + morphId);
        rs.next();
        return rs.getInt("type");
    }


}
