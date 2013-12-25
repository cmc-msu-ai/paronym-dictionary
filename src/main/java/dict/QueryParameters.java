package dict;

/*
 * Параметры поиска паронимов
 */
public class QueryParameters {
    public int maxdl;           //максимальное буквенное расстояние
    public int maxdm;           //максимальное морфемное расстояние
    public int maxdr;           //максимальное буквенное расстояние между корнями
    public double q;               //пороговое значение степени искажения
    public boolean enableForms; //искать слово как словоформу
    public boolean part;        //совпадение части речи
    public boolean gen;         //совпадение рода
    public boolean num;         //совпадение числа
    public boolean relativeRoots;//учитывать чередование корней
    public boolean diffPref;
    public boolean diffRoot;
    public boolean diffSuff;
    public boolean diffEnding;
    public boolean enableDl2;

    public boolean includeNotFound; //включать слова, не распознанные морф. анализатором
    public boolean enablePlural;
    public int setPart;

    public QueryParameters(int maxdl, int maxdm, int maxdr, double q,
                           boolean enableForms,
                           boolean part, boolean gen, boolean num,
                           boolean relativeRoots,
                           boolean diffPref, boolean diffRoot, boolean diffSuff, boolean diffEnding,
                           boolean enableDl2,
                           boolean includeNotFound,
                           boolean enablePlural, int setPart) {
        this.maxdl = maxdl;
        this.maxdm = maxdm;
        this.maxdr = maxdr;
        this.q = q;
        this.enableForms = enableForms;
        this.part = part;
        this.gen = gen;
        this.num = num;
        this.relativeRoots = relativeRoots;
        this.diffPref = diffPref;
        this.diffRoot = diffRoot;
        this.diffSuff = diffSuff;
        this.diffEnding = diffEnding;
        this.enableDl2 = enableDl2;
        this.includeNotFound = includeNotFound;
        this.enablePlural = enablePlural;
        this.setPart = setPart;
    }
}
