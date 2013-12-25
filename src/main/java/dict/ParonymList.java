package dict;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 04.04.2009
 * Time: 7:55:47
 * To change this template use File | Settings | File Templates.
 */
public class ParonymList {
    Word word;                     //исходное слово
    ArrayList<Paronym> paronyms; //список всех паронимов
    ArrayList<Paronym> lparonyms;//список буквенных паронимов
    ArrayList<Paronym> mparonyms;//список морфемных паронимов
    int size;
    int lsize;
    int msize;

    public ParonymList(Word word, ArrayList<Paronym> paronyms) {
        this.word = word;
        this.paronyms = paronyms;
        lparonyms = new ArrayList<Paronym>();
        mparonyms = new ArrayList<Paronym>();
        for (Paronym p : paronyms) {
            //@todo fileter by user's params
            if (p.params.l_ok) {
                lparonyms.add(p);
            }
            if (p.params.m_ok) {
                mparonyms.add(p);
            }
        }
        size = paronyms.size();
        lsize = lparonyms.size();
        msize = mparonyms.size();
    }

    public ArrayList<Paronym> getLparonyms() {
        return lparonyms;
    }

    public ArrayList<Paronym> getMparonyms() {
        return mparonyms;
    }

     public Word getWord() {
        return word;
    }
}

