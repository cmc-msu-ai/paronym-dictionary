package db;

import dict.Morph;

import java.util.ArrayList;
import lingv.GramDecoder;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 01.11.2008
 * Time: 15:11:00
 * To change this template use File | Settings | File Templates.
 */
public class WordFromFile {
    public String word;
    public int id;

    public String splitWord; //слово, разобранное по составу
    public ArrayList<Morph> morphList = new ArrayList<Morph>();
    public Morph root;

    public int aot_id = -1;

    //grammatical parameters
    public int part = -1;
    public int gen = -1;
    public int num = -1;
    public int anim = -1;
    public int asp = -1;

    //for MorphAn
    //flag values
    public static final int NOTSET = 0;
    public static final int SET = 1;
    public static final int FINAL  = 2;

    //flags
    public int partflag = NOTSET;
    public int genflag = NOTSET;
    public int numflag = NOTSET;
    public int animflag = NOTSET;
    public int aspflag = NOTSET;

    public WordFromFile(){
        word = "";
    }

    public WordFromFile(String s){
        word = s;
    }

    public WordFromFile(String s, int part){
        word = s;
        this.part = part;
    }

    public WordFromFile(int id, String word, int part) {
        this.id = id;
        this.word = word;
        this.part = part;
    }

    public WordFromFile(int id, String word, int part, ArrayList<Morph> morphList) {
        this.id = id;
        this.word = word;
        this.part = part;
        this.morphList = morphList;
        for (Morph m : morphList) {
            if (m.type == GramDecoder.ROOT) {
                this.root = m;
            }
        }
    }

    public WordFromFile(int id, String word, int part, int num, ArrayList<Morph> morphList) {
        this.id = id;
        this.word = word;
        this.part = part;
        this.num = num;
        this.morphList = morphList;
        for (Morph m : morphList) {
            if (m.type == GramDecoder.ROOT) {
                this.root = m;
            }
        }
    }
}
