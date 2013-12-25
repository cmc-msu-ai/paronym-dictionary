package dict;

import lingv.GramDecoder;

import java.util.ArrayList;

public class Word {
    public String word;
    public String splitWord;
    public String gramParams = "";  //строка грам. параметров слова
    public int aot_id = -1; //id парадигмы в АОТ
    public int id;

    public int part;
    public String partStr;
    public int gen;
    public int num;
    public int anim;

    //word form info
    public String form; //словоформа
    public String gram; //анкод (грам. хар-ки словоформы в кодировке АОТа)

    public int rootId;
    public ArrayList<Morph> morphList;


    public Word
            (String word,
             int id,
             int aot_id,
             String splitword,
             int part,
             int gen,
             int num,
             int anim,
             String form,
             String gram,
             int rootId,
             ArrayList<Morph> morphList) {

        this.word = word;
        this.id = id;
        this.aot_id = aot_id;
        this.splitWord = splitword;
        this.part = part;
        this.partStr = GramDecoder.partIntToStr(part);
        this.gen = gen;
        this.num = num;
        this.anim = anim;
        this.gramParams = gramParams.concat(partStr + " " +
                GramDecoder.getGenStr(gen) + " " +
                GramDecoder.getNumStr(num) + " " +
                GramDecoder.getAnimStr(anim));
        this.form = form;
        this.gram = gram;
        this.rootId = rootId;
        this.morphList = morphList;

    }



    public Paronym toParonym(ParonymParameters params) {
        return new Paronym(this, params);

    }



    public String getWordInfo() {
        return (splitWord.isEmpty() ?  this.gramParams : splitWord + "\n" + gramParams) + "\n";
    }


}
