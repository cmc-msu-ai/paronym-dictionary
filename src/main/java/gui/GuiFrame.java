package gui;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA. User: Таня Date: 10.02.2009 Time: 21:08:03 To
 * change this template use File | Settings | File Templates.
 */
public class GuiFrame {
    GuiCtrl ctrl;
    JFrame frame;
    AotGUItest aotGUItestForm;
    DictionaryGui dictionaryGuiForm;

    public GuiFrame(GuiCtrl control) {
        try {
            ctrl = control;
            frame = new JFrame("Словарь паронимов");

            // aotGuiTestFormInit();
            dictionaryGuiFormInit();

            frame.pack();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

    }

    public void aotGuiTestFormInit() throws Exception {
        aotGUItestForm = new AotGUItest(ctrl);
        frame.setContentPane(aotGUItestForm.mainPanel);
    }

    public void dictionaryGuiFormInit() throws Exception {
        dictionaryGuiForm = new DictionaryGui(ctrl);
        frame.setContentPane(dictionaryGuiForm.rootPanel);
    }
}
