package gui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 10.02.2009
 * Time: 15:49:20
 * To change this template use File | Settings | File Templates.
 */
public class AotGUItest {

    JPanel mainPanel;
    private GuiCtrl ctrl;
    private JTextField wordField;
    private JTextPane gramstrPane;
    private JTextPane textPane1;
    private JButton goButton;
    private JTextField reverseField;


    public AotGUItest(GuiCtrl control) {

        ctrl = control;

        ctrl.regAotGUItest(this);


        goButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                aot_gramParams();
            }
        });
        wordField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                aot_gramParams();
            }
        });


        reverseField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String word;


                textPane1.setText("");
                gramstrPane.setText("");
                try {
                    word = ctrl.executeReverseButton(reverseField.getText());
                    if (word.isEmpty()) {
                        throw new Exception("No suitable word found");
                    }
                    gramstrPane.setText(word);

                } catch (Exception ex) {
                    textPane1.setText("Could not process your query: " +
                            wordField.getText() + '\n' + ex.getMessage());

                }
            }
        });
    }

    public void aot_gramParams() {
        String aotstr;
        String gramstr = "";
        StringTokenizer tok;
        textPane1.setText("");
        gramstrPane.setText("");
        try {
            aotstr = ctrl.executeGoButton(wordField.getText()).toLowerCase();
            tok = new StringTokenizer(aotstr, " ;");
            while (tok.hasMoreTokens()) {
                gramstr = gramstr.concat(tok.nextToken() + '\n');
            }
            gramstrPane.setText(gramstr);
        } catch (Exception ex) {
            textPane1.setText("Could not process your query: " +
                    wordField.getText());

        }
    }

}
