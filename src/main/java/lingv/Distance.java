package lingv;

import dict.Morph;

import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal;

public class Distance {

  //****************************
  // Get minimum of three values
  //****************************

  private static int Minimum (int a, int b, int c) {
  int mi;

    mi = a;
    if (b < mi) {
      mi = b;
    }
    if (c < mi) {
      mi = c;
    }
    return mi;

  }

  //*****************************
  // Compute Levenshtein distance
  //*****************************

    public static int countDistance(ArrayList<Morph> s, ArrayList<Morph> t, boolean dl2) {
        int sl = s.size();
        int tl = t.size();
        int[] ss = new int[sl];
        int[] tt = new int[tl];
        Morph m;

        if((sl==0) && (tl == 0)){
            return -1;
        }

        for (int i = 0; i < sl; i++) {
            m = s.get(i);
            if ((m.type == GramDecoder.ENDING) && (m.morph.isEmpty())) {
                continue;
            }
            ss[i] = m.id_m;
        }

        for (int i = 0; i < tl; i++) {
            m = t.get(i);
            if ((m.type == GramDecoder.ENDING) && (m.morph.isEmpty())) {
                continue;
            }
            tt[i] = m.id_m;
        }

        return countDistance(ss, tt, dl2);
    }

     public static int countDistance(String s, String t, boolean dl2) {

         int sl = s.length();
         int tl = t.length();
         int[] ss = new int[sl];
         int[] tt = new int[tl];

         if ((sl == 0) && (tl == 0)) {
             return -1;
         }

         for (int i = 0; i < sl; i++) {
             ss[i] = s.charAt(i);
         }

         for (int i = 0; i < tl; i++) {
             tt[i] = t.charAt(i);
         }

         return countDistance(ss, tt, dl2);
     }

    public static int countDistance(int[] s, int[] t, boolean dl2) {
        int n; // length of s
        int m; // length of t
        int d[][];
        // Step 1

        n = s.length;
        m = t.length;

        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = countDistanceMatrix(s, t, n, m, dl2);
        return getDistance(d);

    }

    public static int[][] countDistanceMatrix(int[] s, int[] t, int n, int m, boolean dl2) {
        int d[][]; // matrix
        int i; // iterates through s
        int j; // iterates through t
        int s_i; // ith element of s
        int t_j; // jth element of t
        int tempd;
        int cost; // cost


        d = new int[n + 1][m + 1];

        // Step 2

        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3

        for (i = 1; i <= n; i++) {

            s_i = s[i - 1];

            // Step 4

            for (j = 1; j <= m; j++) {

                t_j = t[j - 1];

                // Step 5

                if (s_i == t_j) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Step 6

                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
                if (dl2) {
                    if ((i > 1) && (j > 1) && (s_i == t[j - 2]) && ( s[i - 2]== t_j)) {
                        tempd = d[i-2][j-2] + cost;
                        d[i][j] = d[i][j] <= tempd ? d[i][j] : tempd;
                    }
                }
            }

        }
        return d;
    }

    public static int getDistance(int[][] d) {
        return d[d.length - 1][d[0].length - 1];
    }

    //вернуть элементы, которыми отличаются массивы s и t, по матрице расстояний. матрица дб не пустой.
    public static ArrayList<ArrayList<Object>> differentElements(int d[][], ArrayList<Object> s, ArrayList<Object> t) {
        int n = d.length;
        int m = d[0].length;
        Object s_i = null; // ith element of s
        Object t_j = null; // jth element of t
        int cost = 0;
        ArrayList<ArrayList<Object>> res = new ArrayList<ArrayList<Object>>();
        ArrayList<Object> pair;
        int i = n - 1;
        int j = m - 1;

        while(i >= 0) {
            if ((i == 0) && (j == 0)) {
                break;
            }
            if (i != 0) {
                s_i = s.get(i - 1);
            }
            while (j >= 0) {
                if ((i == 0) && (j == 0)) {
                    break;
                }
                if (i != 0) {
                    s_i = s.get(i - 1);
                }
                if (j != 0) {
                    t_j = t.get(j - 1);
                }
                if ((i != 0) && (j != 0)) {
                    cost = getCost(s_i, t_j);
                }
                if ((i != 0) && (d[i - 1][j] + 1 == d[i][j])) {
                    //вставка j-го элемента из t
                    pair = new ArrayList<Object>(2);
                    pair.add(s_i);
                    pair.add(null);
                    res.add(pair);

                    i--;
                } else if ((j != 0) && (d[i][j - 1] + 1 == d[i][j])) {
                    //удаление i-го элемента из s
                    pair = new ArrayList<Object>(2);
                    pair.add(null);
                    pair.add(t_j);
                    res.add(pair);

                    j--;
                } else if (d[i - 1][j - 1] + cost == d[i][j]) {
                    if (cost == 1) {
                        //замена j-го элемента из t на i-й элемент s
                        pair = new ArrayList<Object>(2);
                        pair.add(s_i);
                        pair.add(t_j);
                        res.add(pair);
                    }
                    i--;
                    j--;
                }
            }
        }

        return res;
    }

    private static int getCost(Object s_i, Object t_j) {
        if (s_i instanceof Morph) {
            Morph s = (Morph) s_i;
            Morph t = (Morph) t_j;
            return s.id_m == t.id_m ? 0 : 1;
        } else {
            Integer s = (Integer) s_i;
            Integer t = (Integer) t_j;
            return s.equals(t) ? 0 : 1;
        }

    }


    public static ArrayList<ArrayList<Morph>> getDiffer(ArrayList<Morph> s, ArrayList<Morph> t) {
        ArrayList<Object> so = new ArrayList<Object>();
        for (Morph m : s) {
            so.add(m);
        }
        ArrayList<Object> to = new ArrayList<Object>();
        for (Morph m : t) {
            to.add(m);
        }
        ArrayList<ArrayList<Object>> resObj = getObjectDiffer(so, to);
        ArrayList<ArrayList<Morph>> res = new ArrayList<ArrayList<Morph>>();
        ArrayList<Morph> am;
        for (ArrayList<Object> arr : resObj) {
            am = new ArrayList<Morph>();
            am.add((Morph) arr.get(0));
            am.add((Morph) arr.get(1));
            res.add(am);
        }
        return res;
    }

    public static ArrayList<ArrayList<Integer>> getDiffer(String s, String t) {

        ArrayList<Object> so = new ArrayList<Object>();
        for (char c : s.toCharArray()) {
            so.add((int)c);
        }
        ArrayList<Object> to = new ArrayList<Object>();
        for (char c : t.toCharArray()) {
            to.add((int)c);
        }
        ArrayList<ArrayList<Object>> resObj = getObjectDiffer(so, to);
        ArrayList<ArrayList<Integer>> res = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> am;
        for (ArrayList<Object> arr : resObj) {
            am = new ArrayList<Integer>();
            am.add((Integer) arr.get(0));
            am.add((Integer) arr.get(1));
            res.add(am);
        }
        return res;
    }

    public static ArrayList<ArrayList<Object>> getObjectDiffer(ArrayList<Object> s, ArrayList<Object> t) {
        int sl = s.size();
        int tl = t.size();
        ArrayList<ArrayList<Object>> diff = new ArrayList<ArrayList<Object>>();
        ArrayList<Object> pair;
        int[][] d;
        int[] ss = new int[sl];
        int[] tt = new int[tl];

        if((sl==0) && (tl == 0)){
            return diff;
        }

        if (sl == 0) {
            pair = new ArrayList<Object>(2);
            for (Object m: t) {
                pair.add(null);
                pair.add(m);
            }
            diff.add(pair);
            return diff;
        }
        if (tl == 0) {
            pair = new ArrayList<Object>(2);
            for (Object m: s) {
                pair.add(m);
                pair.add(null);
            }
            diff.add(pair);
            return diff;
        }

        for (int i = 0; i < sl; i++) {
                    ss[i] = getElement(s.get(i));
                }

        for (int i = 0; i < tl; i++) {
            tt[i] = getElement(t.get(i));
        }

        d = countDistanceMatrix(ss, tt, sl, tl, false);

        return differentElements(d, s, t);
    }

    private static int getElement(Object o) {
        if (o instanceof Morph) {
            Morph m = (Morph) o;
            return m.id_m;
        } else {
            return (Integer) o;
        }
    }

    /* не работает
    //вычислить расстояние в буквах между строками, считая перестановку символов элементарной операцией
    public static int countDistance2(String s, String t) {
        return getDiffer2(s, t).size();
    }

    //построить матрицу преобразования s в t
    public static ArrayList<ArrayList<Integer>> getDiffer2(String s, String t) {
        ArrayList<ArrayList<Integer>> diff = getDiffer(s, t);
        ArrayList<ArrayList<Integer>> diff2 = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> sdiff = new ArrayList<Integer>();
        ArrayList<Integer> tdiff = new ArrayList<Integer>();
        ArrayList<Integer> chain = new ArrayList<Integer>();
        boolean[] indexes = new boolean[diff.size()];
        Arrays.fill(indexes, false);
        int head;
        int curend;
        int j;
        ArrayList<Integer> diff2Row;
        int chainFirst;


        for (ArrayList<Integer> pair : diff) {
            sdiff.add(pair.get(0));
            tdiff.add(pair.get(1));
        }
        //поиск замкнутых цепочек
        for (int i = 0; i < sdiff.size(); i++) {
            chain.clear();
            head = sdiff.get(i);
            curend = tdiff.get(i);
            j = i;
            while (true) {
                chain.add(j);
                if (curend == head) {
                    break;
                }
                if (sdiff.contains(curend)) {
                    j = sdiff.indexOf(curend);
                    curend = tdiff.get(j);
                } else {
                    break;
                }
            }
            if (chain.size() > 1) {
                for (int k : chain) {
                    indexes[k] = true;
                }
                chainFirst = chain.get(0);
                for (int k = 1; k < chain.size(); k++) {
                    diff2Row = new ArrayList<Integer>(2);
                    diff2Row.add(Character.toUpperCase(sdiff.get(chainFirst)));
                    diff2Row.add(Character.toUpperCase(sdiff.get(k)));
                    diff2.add(diff2Row);
                }
            }

        }
        for (int k = 0; k < indexes.length; k++) {
            if (!indexes[k]) {
                diff2.add(diff.get(k));
            }
        }
        return diff2;
    }
*/


    public static void main(String[] args) {
        int d = Distance.countDistance("ба", "аб", true);
        System.out.println(d);
    }

    public static double countFormula(String word1, String word2, int dl) {
        int l1 = word1.length();
        int l2 = word2.length();
        int len = l1 < l2 ? l1 : l2;
        return new BigDecimal((double)dl/len).setScale(2, java.math.RoundingMode.UP).doubleValue();
    }
}


