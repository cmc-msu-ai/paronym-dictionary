package aot;

/**
 * Класс предоставляет функции морфологического анализа,
 *  используется модуль АОТ "Диалинг. Русская морфология".
 * Обращение к модулю производится через вспомогательную библиотеку NativeAot.dll (технология JNI)
 */
public class NativeAot {

    /**
     * Указатель на анализатор
     */
    int lemmInt;

    /**
     * Указатель на таблицу граммем
     */
    int gramTabInt;

    /**
     * Инициализация морфоанализатора, получение указателей на анализатор и таблицу граммем.
     */
    public NativeAot() { //@todo throws Exception
        init();
        lemmInt = initLemmatizer();
        gramTabInt = initAgramtab();
    }

    private native void init();
    private native void uninit();
    private native int initLemmatizer();
    private native int initAgramtab();

    private native String paradigmsFromNorm(int lemmInt, int gramTabInt, String jword);

    /**
     * Получить коллекцию парадигм для заданной канонической формы
     * @param word каноническая словоформа
     * @return строка, содержащая набор парадигм
     * "id1:pos1:params1:params11;id2:pos2:params2:params22;...;idn:posn:paramsn:paramsnn",
     * где id - id парадигмы, pos - часть речи, paramsX - параметры всей парадигмы, params XX - параметры словоформы,
     * парадигмы отделяются друг от друга точкой с запятой,
     * если ни одной парадигмы не найдено, возвращается пустая строка.
     */
    public String getParadigmsFromNorm(String word) {
        return paradigmsFromNorm(lemmInt, gramTabInt, word.toUpperCase());
    }


    private native String paradigmsFromForm(int lemmInt, int gramTabInt, String jword);

    /**
     * Получить коллекцию парадигм для заданной произвольной словоформы
     * @param word словоформа
     * @return строка, содержащая набор парадигм
     * "id1:pos1:normalform1:params1:params11:params12;id2:pos2:normalform2:params2:params21:params22;...",
     * где id - id парадигмы, pos - часть речи, normalform - нормальная (каноническая) форма,
     * paramsX - параметры всей парадигмы, params XX - параметры словоформы,
     * парадигмы отделяются друг от друга точкой с запятой,
     * если ни одной парадигмы не найдено, возвращается пустая строка.
     */
    public String getParadigmsFromForm(String word) {
        return paradigmsFromForm(lemmInt, gramTabInt, word.toUpperCase());
    }

    private native String wordFromGram(int lemmInt, String word, String gram);

    /**
     * Получить словоформу по заданной канонической форме и набору морф. параметров
     * @param word каноническая словоформа
     * @param gram строка, содержащая набор морф. характеристик (граммем) искомой словоформы.
     * При поиске нужной словоформы ищется точное соответствие наборов граммем, сравнивается вся строка gram целиком.
     * Поэтому порядок следования граммем и форма их записи имеет значение.
     * @return искомая словоформа либо пустая строка
     */
    public String getWordFromGram(String word, String gram) {
        return wordFromGram(lemmInt, word, gram).toLowerCase();
    }

    private native String formFromParadigmIdAndGram
            (int lemmInt, int gramTabInt, int aot_id, String gram, String gramCut, boolean sameGen);

    /**
     * Поиск в заданной парадигме словоформы с заданным набором граммем.
     * Поиск производится просмотром всех словоформ, принадлежащих указанной парадигме и сравнением их
     * морфологических характеристик (граммем) с заданными.
     * @param aot_id Идентификатор парадигмы, которой должна принадлежать искомая словоформа
     * @param gram Набор граммем.
     * При поиске нужной словоформы ищется точное соответствие наборов граммем, сравнивается вся строка gram целиком.
     * Поэтому порядок следования граммем и форма их записи имеет значение.
     * @param gramCut Усеченный набор граммем, не содержащий граммем рода
     * @param sameGen Параметр, указывающий, необходимо ли при синтезе словоформы учитывать род.
     * Если истинен (учитывать род), то используется набор граммем gram,
     * если ложен (не учитывать род) - набор граммем gramCut
     * @return искомая словоформа, в случае неуспеха - пустая строка.
     */
    public String getFormFromParadigmIdAndGram(int aot_id, String gram, String gramCut, boolean sameGen) {
        return formFromParadigmIdAndGram(lemmInt, gramTabInt, aot_id, gram, gramCut, sameGen).toLowerCase();
    }

    /**
     * Завершение работы морфологического анализатора.
     */
    public void uninitialize() {
        uninit();
    }

    /**
     * Загрузка библиотеки NativeAot.dll
     */
    static {
        System.loadLibrary("NativeAot");
    }

    /**
     * Тестовый запуск морфологического анализатора
     * @param args не используются
     */
     public static void main(String args[]) {
        NativeAot aot = new NativeAot();
        aot.init();
        int lemmInt = aot.initLemmatizer();
        int gramTabInt = aot.initAgramtab();
        String result;
        result = aot.paradigmsFromForm(lemmInt, gramTabInt, "пекла");
        System.out.println(result);
        result = aot.paradigmsFromNorm(lemmInt, gramTabInt, "течь");
        System.out.println(result);
        aot.uninit();
    }
}
