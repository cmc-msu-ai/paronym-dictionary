package lingv;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 09.02.2009
 * Time: 15:26:13
 * To change this template use File | Settings | File Templates.
 */

import static db.WordFromFile.*;


import db.WordFromFile;

public class GramDecoder {

    //part
    public static final int NOUN = 0;  //сущ, мест-сущ
    public static final int VERB = 1;  //глагол
    public static final int ADJECT = 2; //прилаг, счетн числ, причастие, мест-прилаг
    public static final int ADVERB = 3;//наречие, дееприч
    public static final int NUMER = 4; //числит
    public static final int FUNC = 5; //служебное слово (предл, союз, межд, част, вводн)
    public static final int[] allParts = {NOUN, VERB, ADJECT, ADVERB, NUMER, FUNC};//number
    public static final int SING = 0;  //един
    public static final int PLUR = 1;  //множ
    public static final int SIPL = 2;  //ед + множ
    //gender
    public static final int F = 0;    //жен
    public static final int M = 1;    //муж
    public static final int S = 2;    //средн
    public static final int MF = 3;   //муж и жен
    //animated
    public static final int ANIM = 0; //одуш
    public static final int NANIM = 1;//неодуш
    public static final int ANBOTH = 2;//одуш + неодуш
    //aspect вид
    public static final int SOV = 0; //соверш
    public static final int NSOV = 1;//несоверш
    //morph types
    public static final int PREF = 0; //приставка
    public static final int ROOT = 1; //корень
    public static final int SUFF = 2; //суффикс
    public static final int ENDING = 3; //окончание




    //возвращает тип морфа (для работы с файлами aria)
    public static int getMorphType(char c) throws Exception {
        switch (c) {
            case 'a':
                return PREF;
            case 'r':
                return ROOT;
            case 's':
                return SUFF;
            case 'e':
                return ENDING;
            default:
                throw new Exception("unknown morph type: " + c);
        }
    }

    public static String partIntToStr(int p) {
        switch (p) {
            case NOUN:
                return "сущ";
            case VERB:
                return "гл";
            case ADJECT:
                return "прил";
            case ADVERB:
                return "нар";
            case NUMER:
                return "числ";
            case FUNC:
                return "служ";
            default:
                return "unknown";
        }
    }

    public static String getGenStr(int gen) {
        switch (gen) {
            case -1:
                return "";
            case 0:
                return "жр";
            case 1:
                return "мр";
            case 2:
                return "ср";
            case 3:
                return "мр-жр";
        }
        return "";
    }

    public static String getNumStr(int num) {
        switch (num) {
            case -1:
                return "";
            case 0:
                return "ед";
            case 1:
                return "мн";
            case 2:
                return "ед-мн";
        }
        return "";
    }

    public static String getAnimStr(int anim) {
        switch (anim) {
            case -1:
                return "";
            case 0:
                return "одуш";
            case 1:
                return "неодуш";
            case 2:
                return "од-неод";
        }
        return "";
    }


}
