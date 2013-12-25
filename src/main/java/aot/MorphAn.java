package aot;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.ArrayList;

import static lingv.GramDecoder.*;
import db.WordFromFile;
import static db.WordFromFile.FINAL;
import static db.WordFromFile.NOTSET;
import static db.WordFromFile.SET;

/**
 * Поддержка работы с морфологическим анализатором. Функции морфологического
 * анализа осуществляются обращением к методам класса NativeAot. Выполняется
 * разбор строк, возвращаемых морф. анализатором, и запись полученных данных в
 * структуру (список структур) Paradigm
 */
public class MorphAn {

    /**
     * Морфологический анализатор АОТ
     */
    static NativeAot aot = new NativeAot();

    /**
     * Тестовый запуск
     * 
     * @param args
     *            не используется
     */

    public static void main(String[] args) {

        try {
            ArrayList<Paradigm> wlist = MorphAn.analyzeForm("вода");
            for (Paradigm w : wlist) {
                w.dump();
                System.out.println("");
            }
            System.out.println("***");

            wlist = MorphAn.analyzeNorm("вести");
            for (Paradigm w : wlist) {
                w.dump();
                System.out.println("");
            }
        } catch (MorphAnException e) {
            e.printStackTrace();
        }
    }

    /**
     * Морфологический анализ входной словоформы, интерпретируемой как
     * существительное.
     * 
     * @param s
     *            Входная словоформа
     * @param asNorm
     *            Интерпретировать слово как нормальную форму (true)/
     *            произвольную словоформу (false)
     * @return Возвращается первая подходящая парадигма (объект типа Paradigm).
     *         В случае неуспеха возвращается null.
     * @throws MorphAnException
     *             Ошибка морфологического анализатора
     */
    private static Paradigm analyzeAsNoun(String s, boolean asNorm)
            throws MorphAnException {
        // найти все морфологические интерпретации словоформы
        ArrayList<Paradigm> paradigmsList = asNorm ? analyzeNorm(s)
                : analyzeForm(s);

        // искать среди найденных парадигм существительное
        for (Paradigm word : paradigmsList) {
            if (word.part == NOUN) {
                return word;
            }
        }
        return null;
    }

    /**
     * Морфологический анализ входной канонической словоформы, интерпретируемой
     * как существительное.
     * 
     * @param s
     *            Входная словоформа
     * @return Первая подходящая парадигма. В случае отсутствия подходящих
     *         парадигм - null.
     * @throws MorphAnException
     *             Ошибка морфологического анализатора
     */
    public static Paradigm analyzeAsNounNorm(String s) throws MorphAnException {
        return analyzeAsNoun(s, true);
    }

    /**
     * Морфологический анализ входной произвольной словоформы. Словоформа
     * интерпретируется как существительное.
     * 
     * @param s
     *            Входная словоформа
     * @return Первая подходящая парадигма. В случае отсутствия подходящих
     *         парадигм - null.
     * @throws MorphAnException
     *             Ошибка морфологического анализатора
     */
    public static Paradigm analyzeAsNounForm(String s) throws MorphAnException {
        return analyzeAsNoun(s, false);
    }

    /**
     * Морфологический анализ словоформы
     * 
     * @param s
     *            Словоформа
     * @param asNorm
     *            Интерпретировать слово как нормальную форму (true)/
     *            произвольную словоформу (false)
     * @return Все найденные парадигмы (список объектов Paradigm)
     * @throws MorphAnException
     *             Ошибка морфологического анализатора
     */
    private static ArrayList<Paradigm> analyzeWord(String s, boolean asNorm)
            throws MorphAnException {
        Paradigm w;
        String gramStr = "";
        StringTokenizer tokgram;
        StringTokenizer tok;
        ArrayList<Paradigm> paradigms = new ArrayList<Paradigm>();

        try {
            // получить данные морф. анализа из морф. анализатора АОТ
            gramStr = asNorm ? aot.getParadigmsFromNorm(s) : aot
                    .getParadigmsFromForm(s);

            // разобрать строку
            tok = new StringTokenizer(gramStr, ";");
            while (tok.hasMoreTokens()) {
                w = new Paradigm();
                w.form = s;
                tokgram = new StringTokenizer(tok.nextToken(), ":");

                // paradigmId
                w.aot_id = new Integer(tokgram.nextToken());

                // часть речи
                w.partAot = tokgram.nextToken();
                w.part = posAotToInt(w.partAot);

                // нормальная форма. в режиме нормальных форм в строке не
                // содержится
                w.norm = asNorm ? w.form : tokgram.nextToken().toLowerCase();

                // грамм. характеристики (граммемы)
                w.paramForms = new ArrayList<String>();
                w.params = "";
                if (tokgram.hasMoreTokens()) {
                    // граммемы парадигмы
                    w.params = tokgram.nextToken();

                    // граммемы словоформ
                    while (tokgram.hasMoreTokens()) {
                        w.paramForms.add(tokgram.nextToken());
                    }
                }
                paradigms.add(w);
            }
        } catch (NoSuchElementException e) {
            throw new MorphAnException(
                    "Invalid Aot string format. \n"
                            + "Required: id1:pos1:normalform1:params11:params12;id2:pos2:normalform2:params21:params22;...\n"
                            + "Found: " + gramStr);

        }
        return paradigms;
    }

    /**
     * Морфологический анализ словоформы, интерпретируемой как каноническая
     * форма.
     * 
     * @param s
     *            Входная словоформа
     * @return Список всех найденных парадигм
     * @throws MorphAnException
     *             Ошибка морфологического анализатора
     */
    public static ArrayList<Paradigm> analyzeNorm(String s)
            throws MorphAnException {
        return analyzeWord(s, true);
    }

    /**
     * Морфологический анализ словоформы, интерпретируемой как произвольная
     * словоформа.
     * 
     * @param s
     *            Входная словоформа
     * @return Список всех найденных парадигм
     * @throws MorphAnException
     *             Ошибка морфологического анализатора
     */
    public static ArrayList<Paradigm> analyzeForm(String s)
            throws MorphAnException {
        return analyzeWord(s, false);
    }

    /**
     * Синтез словоформы по набору граммем.
     * 
     * @param aot_id
     *            Идентификатор парадигмы, которой должна принадлежать искомая
     *            словоформа
     * @param gram
     *            Набор граммем
     * @param gramCut
     *            Усеченный набор граммем, не содержащий граммем рода
     * @param sameGen
     *            Параметр, указывающий, необходимо ли при синтезе словоформы
     *            учитывать род. Если истинен (учитывать род), то используется
     *            набор граммем gram, если ложен (не учитывать род) - набор
     *            граммем gramCut
     * @return Синтезированная словоформа, в случае неуспеха - пустая строка.
     */
    public static String constructForm(int aot_id, String gram, String gramCut,
            boolean sameGen) {
        return aot.getFormFromParadigmIdAndGram(aot_id, gram, gramCut, sameGen);
    }

    /**
     * Преобразование граммемы части речи, используемой в АОТ, в числовой код,
     * используемый в словаре паронимов. Помимо преобразования кода выполняется
     * обобщение частей речи. Коды частей речи должны соответствовать входному
     * файлу codes.txt
     * 
     * @param posAot
     *            символьный код части речи, используемый в АОТ
     * @return числовой код части речи, используемый в словаре паронимов, -1 в
     *         случае неуспеха
     */
    public static int posAotToInt(String posAot) {
        // posAot
        // 0 C мама существительное
        // 0 МС он местоимение-существительное

        // 1 ИНФИНИТИВ идти инфинитив
        // 1 Г идет глагол в личной форме

        // 2 ПРИЧАСТИЕ идущий причастие
        // 2 МС-П всякий местоименное прилагательное
        // 2 П красный прилагательное
        // 2 ЧИСЛ-П восьмой порядковое числительное

        // 3 ДЕЕПРИЧАСТИЕ идя деепричастие
        // 3 МС-ПРЕДК нечего местоимение-предикатив
        // 3 Н круто наречие
        // 3 ПРЕДК интересно предикатив

        // 2 КР_ПРИЛ красива краткое прилагательное
        // 2 КР_ПРИЧАСТИЕ построена краткое причастие

        // 4 ЧИСЛ восемь числительное (количественное)

        // 5 ПРЕДЛ под предлог
        // 5 СОЮЗ и союз
        // 5 МЕЖД ой междометие
        // 5 ЧАСТ же, бы частица
        // 5 ВВОДН конечно вводное слово

        if (posAot.equals("С") || posAot.equals("МС")) {
            return NOUN;
        } else if (posAot.equals("ИНФИНИТИВ") || posAot.equals("Г")) {
            return VERB;
        } else if (posAot.equals("ПРИЧАСТИЕ") || posAot.equals("МС-П")
                || posAot.equals("П") || posAot.equals("ЧИСЛ-П")) {
            return ADJECT;
        } else if (posAot.equals("ДЕЕПРИЧАСТИЕ") || posAot.equals("МС-ПРЕДК")
                || posAot.equals("Н") || posAot.equals("КР_ПРИЛ")
                || posAot.equals("КР_ПРИЧАСТИЕ") || posAot.equals("ПРЕДК")) {
            return ADVERB;
        } else if (posAot.equals("ЧИСЛ")) {
            return NUMER;
        } else if (posAot.equals("ПРЕДЛ") || posAot.equals("СОЮЗ")
                || posAot.equals("МЕЖД") || posAot.equals("ЧАСТ")
                || posAot.equals("ВВОДН")) {
            return FUNC;
        } else {
            if (!posAot.equals("ФРАЗ")) {
                System.out.println("posAotToInt: Unknown part of speech: "
                        + posAot);
            }
            return -1;
        }
    }

    /**
     * Добавление морфологических параметров слова, полученных из АОТ.
     * Производится сопоставление параметров из строки, выданной АОТом с
     * параметрами, уже приписанными слову.
     * 
     * @param w
     *            слово
     * @param paramStr
     *            строка морф. параметров, выданная АОТом
     * @return true , если в параметрах нет конфликтов, и false, если есть
     *         конфликты.
     */
    public static boolean decParam(WordFromFile w, String paramStr) {

        String param;
        StringTokenizer tok = new StringTokenizer(paramStr, ",");
        boolean genConflict;
        boolean numConflict;

        // Переменная animconf отражает наличие конфликтов в параметрах
        // одушевленности и
        // предотвращает конфликты вида: но,од + final од
        // значения переменной animconf:
        final int EMPTY = 0; // параметры не определены
        final int NOCONF = 1; // параметры определены, конфликтов нет
        final int CONF = 2; // параметры конфликтуют
        int animconf = EMPTY;

        // Сопоставление параметров производится последовательным просмотром
        // строки paramStr
        // и добавлением соответствующих параметров к слову w.
        while (tok.hasMoreTokens()) {
            genConflict = false;
            numConflict = false;
            param = tok.nextToken();

            // при сопоставлении параметров сначала запрашивается значение
            // соответствующего флага, оно может равняться:
            // FINAL - морф. параметр слова был определен до начала текущего
            // вызова метода decParam
            // SET - морф. параметр был приписан слову в результате анализа
            // предшествующих элементов строки paramStr
            // NOTSET - морф. параметр не определен
            // в зависимости от значения флага и текущих морф. параметров,
            // приписанных к слову,
            // сохраняется либо добавляется морф. параметр или сигнализируется
            // конфликт
            if (param.equals("мр")) {
                switch (w.genflag) {
                case FINAL:
                    if ((w.gen != M) && (w.gen != MF)) {
                        genConflict = true;
                    }
                    break;
                case NOTSET:
                    w.gen = M;
                    w.genflag = SET;
                    break;
                case SET:
                    if (w.gen == S) {
                        genConflict = true;
                        System.out.println("Conflict: мр " + w.gen + " "
                                + w.word + " " + param);
                    } else if (w.gen == F) {
                        w.gen = MF;
                    }
                }
            } else if (param.equals("жр")) {
                switch (w.genflag) {
                case FINAL:
                    if ((w.gen != F) && (w.gen != MF)) {
                        genConflict = true;
                    }
                    break;
                case NOTSET:
                    w.gen = F;
                    w.genflag = SET;
                    break;
                case SET:
                    if (w.gen == S) {
                        genConflict = true;
                        System.out.println("Conflict: жр " + w.gen + " "
                                + w.word + " " + param);
                    } else if (w.gen == M) {
                        w.gen = MF;
                    }
                }
            } else if (param.equals("ср")) {
                switch (w.genflag) {
                case FINAL:
                    if (w.gen != S) {
                        genConflict = true;
                    }
                    break;
                case NOTSET:
                    w.gen = S;
                    w.genflag = SET;
                    break;
                case SET:
                    if (w.gen != S) {
                        genConflict = true;
                        System.out.println("Conflict: cр " + w.gen + " "
                                + w.word + " " + param);
                    }
                }
            } else if (param.equals("мр-жр")) {
                switch (w.genflag) {
                case FINAL:
                    if (w.gen == S) {
                        genConflict = true;
                    }
                    break;
                case NOTSET:
                    w.gen = MF;
                    w.genflag = SET;
                    break;
                case SET:
                    if (w.gen == S) {
                        genConflict = true;
                        System.out.println("Conflict: мр-жр " + w.gen + " "
                                + w.word + " " + param);
                    }
                }
            } else if (param.equals("но")) {
                switch (w.animflag) {
                case FINAL:
                    if ((w.anim == NANIM) || (w.anim == ANBOTH)) {
                        animconf = NOCONF;
                    } else if (animconf == EMPTY) {
                        // w.anim == ANIM
                        animconf = CONF;
                    }
                    break;
                case NOTSET:
                    w.anim = NANIM;
                    w.animflag = SET;
                    break;
                case SET:
                    if (w.anim != NANIM) {
                        w.anim = ANBOTH;
                    }
                }
            } else if (param.equals("од")) {
                switch (w.animflag) {
                case FINAL:
                    if ((w.anim == ANIM) || (w.anim == ANBOTH)) {
                        animconf = NOCONF;
                    } else if (animconf == EMPTY) {
                        // w.anim == NANIM
                        animconf = CONF;
                    }
                    break;
                case NOTSET:
                    w.anim = ANIM;
                    w.animflag = SET;
                    break;
                case SET:
                    if (w.anim != ANIM) {
                        w.anim = ANBOTH;
                    }
                }
            } else if (param.equals("ед")) {
                switch (w.numflag) {
                case FINAL:
                    if (w.num != SING) {
                        numConflict = true;
                    }
                    break;
                case NOTSET:
                    w.num = SING;
                    w.numflag = SET;
                    break;
                case SET:
                    if (w.num != SING) {
                        w.num = SIPL;
                    }
                }
            } else if (param.equals("мн")) {
                switch (w.numflag) {
                case FINAL:
                    if (w.num != PLUR) {
                        numConflict = true;
                    }
                    break;
                case NOTSET:
                    w.num = PLUR;
                    w.numflag = SET;
                    break;
                case SET:
                    if (w.num != PLUR) {
                        w.num = SIPL;
                    }
                }

            }

            if (genConflict || numConflict || (animconf == CONF)) {
                w.numflag = NOTSET;
                w.genflag = NOTSET;
                w.animflag = NOTSET;
                return false;
            }
        }
        return true;
    }

    /**
     * Завершение работы морфологического анализатора АОТ
     */
    public static void unInit() {
        aot.uninitialize();
    }
}
