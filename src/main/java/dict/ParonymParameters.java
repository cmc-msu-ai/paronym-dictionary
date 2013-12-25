package dict;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 28.04.2009
 * Time: 11:41:51
 * To change this template use File | Settings | File Templates.
 */
public class ParonymParameters {
    public boolean l_ok;
    public boolean m_ok;
    public boolean dlDef;
    public boolean dmDef;
    public int dl ;
    public int dl2;
    public int curDl;
    public int dm ;
    public int dr;
    public boolean pref;
    public boolean root;
    public boolean relRoots;
    public boolean suff;
    public boolean end;
    public double q;

    public void dump() {
        System.out.println("dlDef = " + dlDef);
        System.out.println("dmDef = " + dmDef);
        System.out.println("dl = " + dl);
        System.out.println("dl2 = " + dl2);
        System.out.println("dm = " + dm);
        System.out.println("dr = " + dr);
        System.out.println("pref = " + pref);
        System.out.println("root = " + root);
        System.out.println("relRoots = " + relRoots);
        System.out.println("suff = " + suff);
        System.out.println("end = " + end);
        System.out.println("l_ok = " + l_ok);
        System.out.println("m_ok = " + m_ok);
    }
}
