package db;

import dict.QueryParameters;
import dict.ParonymParameters;

import java.util.Formatter;

/**
 * Created by IntelliJ IDEA. User: Таня Date: 27.04.2009 Time: 12:43:48 To
 * change this template use File | Settings | File Templates.
 */
public class ParonymCoder {
    // dlCode
    private static final byte dlMask = Byte.valueOf("78", 16);
    private static final byte dl2Mask = Byte.valueOf("07", 16);

    private static final byte dlShift = 3;
    private static final byte dl2Shift = 0;

    // dmCode
    private static final int dmMask = Integer.valueOf("7800", 16);
    private static final int drMask = Integer.valueOf("0780", 16);
    private static final int prefMask = Integer.valueOf("0040", 16);
    private static final int rootMask = Integer.valueOf("0030", 16);
    private static final int suffMask = Integer.valueOf("0008", 16);
    private static final int endMask = Integer.valueOf("0004", 16);

    private static final int dmShift = 11;
    private static final int drShift = 7;
    private static final int prefShift = 6;
    private static final int rootShift = 4;
    private static final int suffShift = 3;
    private static final int endShift = 2;

    // private static final int dlDefMask = Integer.valueOf("4000", 16);
    // private static final int dlMask = Integer.valueOf("3000", 16);
    // private static final int dl2Mask = Integer.valueOf("0800", 16);
    // private static final int dmDefMask = Integer.valueOf("0400", 16);
    // private static final int dmMask = Integer.valueOf("0380", 16);
    // private static final int drMask = Integer.valueOf("0060", 16);
    // private static final int prefMask = Integer.valueOf("0010", 16);
    // private static final int rootMask = Integer.valueOf("000c", 16);
    // private static final int suffMask = Integer.valueOf("0002", 16);
    // private static final int endMask = Integer.valueOf("0001", 16);
    //
    // private static final int dlDefShift = 14;
    // private static final int dlShift = 12;
    // private static final int dl2Shift = 11;
    // private static final int dmDefShift = 10;
    // private static final int dmShift = 7;
    // private static final int drShift = 5;
    // private static final int prefShift = 4;
    // private static final int rootShift = 2;
    // private static final int suffShift = 1;
    // private static final int endShift = 0;

    public static void main(String[] args) {
        System.out.println(Integer.toBinaryString(30736));
    }

    /**
     * Расшифровка свойств пары паронимов
     * 
     * @return Возвращается структура, содержащая параметры пары паронимов
     * @param dlcode
     * @param dmcode
     */
    public static ParonymParameters decode(byte dlcode, int dmcode) {
        ParonymParameters p = new ParonymParameters();

        p.dlDef = dlcode != -1;
        if (p.dlDef) {
            p.dl = ((dlcode & dlMask) >> dlShift);
            p.dl2 = ((dlcode & dl2Mask));
        }

        p.dmDef = dmcode != -1;
        if (p.dmDef) {
            p.dm = ((dmcode & dmMask) >> dmShift);
            p.dr = (dmcode & drMask) >> drShift;
            p.pref = ((dmcode & prefMask) >> prefShift == 1);
            switch ((dmcode & rootMask) >> rootShift) {
            case 0:
                p.root = false;
                p.relRoots = false;
                break;
            case 1:
                p.root = true;
                p.relRoots = true;
                break;
            case 2:
                p.root = true;
                p.relRoots = false;
                break;
            }
            p.suff = ((dmcode & suffMask) >> suffShift == 1);
            p.end = ((dmcode & endMask) >> endShift == 1);
        }
        return p;
    }

    /**
     * Возвращает код параметров паронимического отношения, упакованный в int
     * 
     * @param dl
     *            расстояние в буквах
     * @param dl2
     *            расстояние в буквах, перестановка символов считается
     *            элементарной ред. операцией
     * 
     * @return код параметров паронимического отношения. В случае, если dl и dm
     *         одновременно равны -1, возвращается код 0.
     * @throws ParonymCodeException
     *             Недопустимые значения параметров.
     */
    public static byte generateDlCode(int dl, int dl2)
            throws ParonymCodeException {
        // Условия корректности кода:
        // dl 0-15
        // dl2 0-7

        String str;
        try {
            String code = "0";
            if (dl == -1) {
                return -1;
            } else {

                // dl
                if (dl < 16) {
                    str = Integer.toBinaryString(dl);
                    while (str.length() < 4) {
                        str = "0" + str;
                    }
                    code += str;
                } else {
                    throw new ParonymCodeException("Wrong dl value: " + dl);
                }

                // dl2
                if (dl2 < 8) {
                    str = Integer.toBinaryString(dl2);
                    while (str.length() < 3) {
                        str = "0" + str;
                    }
                    code += str;
                } else {
                    throw new ParonymCodeException("Wrong dl2 value: " + dl2);
                }
            }

            return Byte.valueOf(code, 2);
        } catch (ParonymCodeException e) {
            throw new ParonymCodeException("Could not generate Paronym Code. "
                    + e.getMessage());
        }
    }

    /**
     * @param dm
     *            расстояние в морфах
     * @param dr
     *            расстояние в буквах между корнями
     * @param pref
     *            различие в приставках
     * @param root
     *            различие в приставках
     * @param relRoots
     *            корни являются чередующими
     * @param suff
     *            различие в суффиксах
     * @param end
     *            различие в окончаниях
     * @return
     * @throws ParonymCodeException
     */
    public static int generateDmCode(int dm, int dr, boolean pref,
            boolean root, boolean relRoots, boolean suff, boolean end)
            throws ParonymCodeException {
        // dm 0-15
        // dr 0-15

        String code;
        String str;
        try {
            code = "0";
            if (dm == -1) {
                return -1;
            } else {
                // dm
                if (dm < 16) {
                    str = Integer.toBinaryString(dm);
                    while (str.length() < 4) {
                        str = "0" + str;
                    }
                    code += str;
                } else {
                    throw new ParonymCodeException("Wrong dm value: " + dm);
                }

                // dr
                if (dr < 16) {
                    str = Integer.toBinaryString(dr);
                    while (str.length() < 4) {
                        str = "0" + str;
                    }
                    code += str;
                } else {
                    throw new ParonymCodeException("Wrong dr value: " + dr);
                }

                code += pref ? "1" : "0";
                code += root ? (relRoots ? "01" : "10") : "00";
                code += suff ? "1" : "0";
                code += end ? "1" : "0";
                code += "00"; // неиспользуемые биты
            }
            return Integer.valueOf(code, 2);
        } catch (ParonymCodeException e) {
            throw new ParonymCodeException("Could not generate Paronym Code. "
                    + e.getMessage());
        }

    }

    public static int getDl(byte dlcode) {
        return (dlcode & dlMask) >> dlShift;
    }

    public static int getDm(int dmcode) {
        return (dmcode & dmMask) >> dmShift;
    }
}
