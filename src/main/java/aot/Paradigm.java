package aot;

import java.util.ArrayList;

/**
 * Объект, создаваемый на основе работы морфологического анализатора.
 * Соответствует морфологической парадигме слова.
 */
public class Paradigm {

    /**
     * словоформа (исходная, по которой строилась парадигма)
     */
    public String form;

    /**
     * уникальный номер парадигмы в библиотеке АОТ
     */
    public int aot_id;

    /**
     * нормальная форма
     */
    public String norm;

    /**
     * часть речи (строка)
     */
    public String partAot;

    /**
     * обобщенная часть речи (число). Соответствие описано в MorphAn
     */
    public int part;

    /**
     * набор граммем, общих для всей парадигмы
     */
    public String params;

    /**
     * набор морф. параметров словоформ, например: {"мр, им, мн", "мр, вн, мн" }
     */
    public ArrayList<String> paramForms;     //

    /**
     * Распечатать парадигму в поток стандартного вывода.
     */
    public void dump() {
        System.out.println("form = " + form);
        System.out.println("partAot = " + partAot);
        System.out.println("params = " + params);
        System.out.println("norm = " + norm);
        for (String s : paramForms) {
            System.out.println("paramForm = " + s);
        }
    }
}
