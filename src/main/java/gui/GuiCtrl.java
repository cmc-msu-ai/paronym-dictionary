package gui;

import aot.MorphAn;
import aot.Paradigm;
import dict.*;

import javax.swing.*;
import java.util.ArrayList;

import db.Database;


/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 10.02.2009
 * Time: 20:19:40
 * To change this template use File | Settings | File Templates.
 */
public class GuiCtrl {
    Database db;
    Dictionary dictionary;
    GuiFrame frame;

    AotGUItest aotGUItestForm;
    DictionaryGui dictionaryGuiForm;

    public void createGuiFrame() {

       try {
          frame = new GuiFrame(this);

       } catch (Exception ex) {
           ex.printStackTrace();
       }
    }

    public void regAotGUItest(AotGUItest r) {
        aotGUItestForm = r;
    }

    public void regDictionaryGui(DictionaryGui r) {
        dictionaryGuiForm = r;
    }

    public void init(String dbname) throws Exception {
        db = new Database(dbname);
        dictionary = new Dictionary(db);
    }

    public void uninit() throws Exception {
        MorphAn.unInit();
        db.shutdown();
    }


    public String executeGoButton(String word) throws Exception {
        ArrayList<Paradigm> paradigms;
        paradigms = MorphAn.analyzeForm(word);
        if (paradigms.isEmpty()) {
            throw new Exception("Empty input");
        }
        return paradigms.get(0).params;
    }

    public String executeReverseButton(String word) throws Exception{

        return "";
    }


    //DictionaryGuiForm
    //@todo add queryParams (class QueryParamsSet ?)
    public ArrayList<ParonymList> executeFindParonymsButton(String word, QueryParameters params)
            throws Exception {
        return dictionary.executeQuery(word,params);
    }

    public static void main(String[] args) {
        try {
            final GuiCtrl ctrl = new GuiCtrl();
//            ctrl.init(args[0]);
            ctrl.init("db/paronym/paronym");

            //ctrl.init_aotGuiTest();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ctrl.createGuiFrame();
                }
            });
            //ctrl.uninit();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }


    public ArrayList<CompareForms> executeCompareAction(String form1, String form2) throws Exception {
        return dictionary.executeCompareQuery(form1, form2);
    }
}
