package dict;

import static lingv.GramDecoder.*;
import db.Query;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 12.04.2009
 * Time: 9:37:12
 * To change this template use File | Settings | File Templates.
 */
public class Morph {


    public int id_m;
    public int type;
    public String morph;
    public int weight;
    public int len;

    public Morph(int id_m, int type, String morph) {
        this.id_m = id_m;
        this.type = type;
        this.morph = morph;
        this.len = morph.length();
        switch (type) {
            case PREF:
                this.weight = 2;
                break;
            case ROOT:
                this.weight = 5;
                break;
            case SUFF:
                this.weight = 2;
                break;
            case ENDING:
                this.weight = 2;
                break;
            default:
                //@todo throw exception
        }
    }
}
