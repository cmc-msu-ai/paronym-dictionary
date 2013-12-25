package dict;

/**
 * Created by IntelliJ IDEA. User: Таня Date: 04.04.2009 Time: 8:25:03 To change
 * this template use File | Settings | File Templates.
 */
public class Paronym extends Word {
    public ParonymParameters params;

    public Paronym(Word w, ParonymParameters params) {
        super(w.word, w.id, w.aot_id, w.splitWord, w.part, w.gen, w.num,
                w.anim, w.form, w.gram, w.rootId, w.morphList);
        this.params = params;
    }
}
