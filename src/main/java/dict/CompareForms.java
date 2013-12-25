package dict;

import lingv.Distance;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA. User: Таня Date: 24.04.2009 Time: 9:52:30 To change
 * this template use File | Settings | File Templates.
 */
public class CompareForms {
    public String form1;
    public String form2;
    public Word word1 = null;
    public Word word2 = null;
    public int dl = -1;
    public int dm = -1;
    public ArrayList<ArrayList<Morph>> diff;
    public String diffStr = "";

    public CompareForms(String word1, String word2) {
        form1 = word1;
        form2 = word2;
        dl = Distance.countDistance(word1, word2, true);
    }

    public CompareForms(Word word1, Word word2, int dl, int dm) {
        this.word1 = word1;
        this.word2 = word2;
        this.form1 = word1.form;
        this.form2 = word2.form;
        this.dl = dl == -1 ? Distance.countDistance(form1, form2, false) : dl;
        this.dm = dm == -1 ? Distance.countDistance(word1.morphList,
                word2.morphList, false) : dm;
        diff = Distance.getDiffer(word1.morphList, word2.morphList);
        Morph m1;
        Morph m2;
        for (ArrayList<Morph> am : diff) {
            m1 = am.get(0);
            m2 = am.get(1);
            if ((m1 != null) && (m2 != null)) {
                diffStr += "\"" + m1.morph + "\" - \"" + m2.morph + "\"\n";
            } else if (m2 != null) {
                diffStr += "вставить \"" + m2.morph + "\"\n";
            } else {
                diffStr += "удалить \"" + m1.morph + "\"\n";
            }
        }

    }
}
