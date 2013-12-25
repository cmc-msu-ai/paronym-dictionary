package gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import dict.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import lingv.GramDecoder;

/**
 * Created by IntelliJ IDEA. User: Таня Date: 03.03.2009 Time: 9:23:42 To change
 * this template use File | Settings | File Templates.
 */
public class DictionaryGui {
    GuiCtrl ctrl;
    JPanel rootPanel;
    private JPanel mainPanel;
    private JTextField wordField;
    private JButton findParonymsButton;
    private JTextPane errorPane;
    private JTable letParTable;
    private JTable morphParTable;
    private JTextPane letParInfoPane;
    private JTextPane morphParInfoPane;
    private JTabbedPane letTabbedPane;
    private JTabbedPane morphTabbedPane;

    QueryParameters queryParams;

    // query Parameters JComponents
    private JCheckBox partBox;
    private JCheckBox genBox;
    private JCheckBox numBox;
    private JComboBox dlBox;
    private JComboBox dmBox;
    private JComboBox drBox;
    private JCheckBox formsBox;
    private JCheckBox rootsRelatBox;
    private JToggleButton paramsButton;
    private JSplitPane splitPane;
    private JCheckBox hideBox;
    private JCheckBox prefBox;
    private JCheckBox rootBox;
    private JCheckBox suffBox;
    private JCheckBox endingBox;
    private JComboBox qBox;
    private JPanel dictPanel;
    private JPanel comparePanel;

    private JPanel choicePanel;
    private JToggleButton dictChoice;
    private JToggleButton compareChoice;
    private JTextField compField1;
    private JTextField compField2;
    private JButton compareButton;
    private JTextPane word1Pane;
    private JTextPane word2Pane;
    private JTextPane comparePane;
    private JCheckBox includeNotFoundBox;
    private JCheckBox enablePluralBox;
    private JCheckBox includeManualBox;
    private JComboBox partList;
    private JCheckBox enableDl2Box;

    // query Parameters variables
    boolean enableForms = false;

    int maxdl = -1;
    int maxdm = -1;
    int maxdr = -1;

    double q = Double.MAX_VALUE;

    // константы для работы с таблицами
    final boolean LETTER = true;
    final boolean MORPHEM = false;

    // возможно пригодится для истории запросов
    ArrayList<ParonymTableModel> modelList = new ArrayList<ParonymTableModel>();

    ParonymTableModel letModel = new ParonymTableModel(this);
    ParonymTableModel morphModel = new ParonymTableModel(this);

    private TableRowSorter<ParonymTableModel> letSorter = new TableRowSorter<ParonymTableModel>(
            letModel);
    private TableRowSorter<ParonymTableModel> morphSorter = new TableRowSorter<ParonymTableModel>(
            morphModel);

    public DictionaryGui(GuiCtrl control) {
        ctrl = control;
        ctrl.regDictionaryGui(this);

        dlBox.setModel(new DefaultComboBoxModel<String>(new String[]{"-",
                "1", "2"}));
        dmBox.setModel(new DefaultComboBoxModel<String>(new String[]{"-",
                "1", "2"}));
        drBox.setModel(new DefaultComboBoxModel<String>(new String[]{"-",
                "0", "1", "2", "3"}));
        qBox.setModel(new DefaultComboBoxModel<String>(new String[]{"-",
                "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"}));
        partList.setModel(new DefaultComboBoxModel<String>(new String[]{"-",
                "сущ", "гл", "прил", "нар"}));

        // tables
        // @todo закладка
        letParTable.setModel(letModel);
        morphParTable.setModel(morphModel);

        letParTable.setRowSorter(letSorter);
        morphParTable.setRowSorter(morphSorter);

        letTabbedPane.removeAll();
        morphTabbedPane.removeAll();

        // listeners
        findParonymsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                findParonymsAction();
            }
        });

        wordField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    findParonymsAction();
                }
            }
        });

        letTabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                letModel.setData(letTabbedPane.getSelectedIndex(), LETTER);
                letParInfoPane.setText("");
            }

        });

        morphTabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                morphModel.setData(morphTabbedPane.getSelectedIndex(), MORPHEM);
                morphParInfoPane.setText("");
            }

        });

        letParTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {

                letParInfoPane.setText(letModel.getWordInfo(letSorter
                        .convertRowIndexToModel(letParTable.getSelectedRow()),
                        letTabbedPane.getSelectedIndex()));
            }
        });

        morphParTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                morphParInfoPane.setText(morphModel.getWordInfo(
                        morphSorter.convertRowIndexToModel(morphParTable
                                .getSelectedRow()), morphTabbedPane
                        .getSelectedIndex()));
            }
        });

        formsBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableForms = ((JCheckBox) e.getSource()).isSelected();
            }
        });

        dlBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                maxdl = setItemInt(e);
            }
        });

        dmBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                maxdm = setItemInt(e);
            }
        });

        drBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                maxdr = setItemInt(e);
            }
        });

        qBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                q = setItemDouble(e);
            }
        });

        paramsButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                splitPane.setDividerLocation(paramsButton.isSelected() ? 250
                        : 0);
            }
        });

        paramsButton.setSelected(true);
        splitPane.setDividerLocation(0);

        hideBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (hideBox.isSelected()) {
                    paramsButton.setSelected(false);
                }
            }
        });

        compareChoice.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (compareChoice.isSelected()) {
                    CardLayout cl = (CardLayout) (mainPanel.getLayout());
                    cl.show(mainPanel, "comp");
                }
                dictChoice.setSelected(false);
            }
        });
        dictChoice.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (dictChoice.isSelected()) {
                    CardLayout cl = (CardLayout) (mainPanel.getLayout());
                    cl.show(mainPanel, "dict");
                }
                compareChoice.setSelected(false);
            }
        });
        compareButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                compareAction();
            }
        });

    }

    private void compareAction() {
        ArrayList<CompareForms> compList = new ArrayList<CompareForms>();
        String f1 = compField1.getText();
        String f2 = compField2.getText();
        if (!f1.isEmpty() && !f2.isEmpty()) {
            try {
                compList = ctrl.executeCompareAction(f1, f2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!compList.isEmpty()) {
            CompareForms compare = compList.get(0);
            Word w1 = compare.word1;
            Word w2 = compare.word2;
            if ((w1 == null) || w2 == null) {
                word1Pane.setText(compare.form1);
                word2Pane.setText(compare.form2);
                comparePane.setText("Редакционное расстояние в буквах: "
                        + compare.dl);

            } else {
                word1Pane.setText(wordInfo(compare.word1));
                word2Pane.setText(wordInfo(compare.word2));
                String dmStr = compare.dm == -1 ? ""
                        : "Редакционное расстояние в морфах: " + compare.dm
                        + "\n";
                comparePane.setText("Редакционное расстояние в буквах: "
                        + compare.dl + "\n" + dmStr + "Различия: \n"
                        + compare.diffStr);
            }

        } else {
            comparePane.setText("Невозможно обработать запрос");
        }
    }

    private int setItemInt(ActionEvent e) {
        JComboBox box = (JComboBox) e.getSource();
        String item = (String) box.getSelectedItem();
        return item.equals("-") ? -1 : new Integer(item);
    }

    private int getListInt(String s) {
        if (s.equals("сущ")) {
            return GramDecoder.NOUN;
        } else if (s.equals("гл")) {
            return GramDecoder.VERB;
        } else if (s.equals("прил")) {
            return GramDecoder.ADJECT;
        } else if (s.equals("нар")) {
            return GramDecoder.ADVERB;
        } else {
            return -1;
        }
    }

    private double setItemDouble(ActionEvent e) {
        JComboBox box = (JComboBox) e.getSource();
        String item = (String) box.getSelectedItem();
        return item.equals("-") ? Double.MAX_VALUE : new Double(item);
    }

    private void findParonymsAction() {
        ArrayList<ParonymList> paronymLists;// cписок списков паронимов
        String word;
        if (hideBox.isSelected()) {
            paramsButton.setSelected(false);
            mainPanel.repaint();
        }
        errorPane.setText("");
        letParInfoPane.setText("");
        morphParInfoPane.setText("");
        try {
            word = wordField.getText();
            if (word.equals("")) {
                throw new ParonymsNotFoundException("Слово не было введено");
            }
            if (word.length() > 64) {
                throw new ParonymsNotFoundException("Слишком длинное слово");
            }

            queryParams = new QueryParameters(maxdl, maxdm, maxdr, q,
                    enableForms, partBox.isSelected(), genBox.isSelected(),
                    numBox.isSelected(), rootsRelatBox.isSelected(),
                    prefBox.isSelected(), rootBox.isSelected(),
                    suffBox.isSelected(), endingBox.isSelected(),
                    enableDl2Box.isSelected(), false, true,
                    getListInt((String) partList.getSelectedItem()));
            paronymLists = ctrl.executeFindParonymsButton(word, queryParams);
            if (paronymLists.isEmpty()) {
                // @todo слово не найдено
                throw new ParonymsNotFoundException("Паронимов к слову «"
                        + word + "» не найдено ");
            }

            // паронимы найдены. обновить данные
            letTabbedPane.removeAll();
            morphTabbedPane.removeAll();

            // загрузить данные о паронимах в модель

            letModel.loadData(paronymLists, LETTER);
            // отобразить данные по умолчанию (для 1ого варианта заданного
            // слова)
            letModel.setDefaultData(LETTER);

            morphModel.loadData(paronymLists, MORPHEM);
            morphModel.setDefaultData(MORPHEM);

            // загрузка данных в инфо-панель
            for (ParonymList paronymList : paronymLists) {
                setWordInfo(letTabbedPane, paronymList.getWord(), true);
                setWordInfo(morphTabbedPane, paronymList.getWord(), false);

            }

        } catch (ParonymsNotFoundException ex1) {
            errorPane.setText(ex1.getMessage());
        } catch (Exception ex2) {
            ex2.printStackTrace();
        }
    }

    private void setWordInfo(JTabbedPane tabbedPane, Word curWord, boolean let) {
        JTextPane pane = new JTextPane();
        pane.setText(wordInfo(curWord));
        pane.setEditable(false);
        tabbedPane.add(
                curWord.partStr
                        + " ("
                        + (let ? letModel.getParonymCount(tabbedPane
                        .getTabCount()) : morphModel
                        .getParonymCount(tabbedPane.getTabCount()))
                        + ")", pane);

    }

    public String wordInfo(Word curWord) {
        String text = "";
        if (enableForms) {
            text = text.concat("Словоформа: " + curWord.form + "\n"
                    + curWord.gram + "\n");
        }
        text = text.concat("Норм. форма: " + curWord.word + "\n"
                + curWord.getWordInfo());
        return text;
    }

    public boolean getEnableForms() {
        return enableForms;
    }

    private void createUIComponents() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuService = new JMenu("Режим");
        menuBar.add(menuService);
        JMenuItem compareItem = new JMenuItem("2 словоформы");
        menuService.add(compareItem);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));
        rootPanel.setPreferredSize(new Dimension(750, 650));
        choicePanel = new JPanel();
        choicePanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 1, 1));
        choicePanel.setVisible(false);
        rootPanel.add(choicePanel, BorderLayout.NORTH);
        dictChoice = new JToggleButton();
        dictChoice.setHorizontalAlignment(0);
        dictChoice.setText("Словарь");
        dictChoice.setVisible(false);
        choicePanel.add(dictChoice, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(260, 25), null, 0, false));
        compareChoice = new JToggleButton();
        compareChoice.setText("Сравнение словоформ");
        compareChoice.setVisible(false);
        choicePanel.add(compareChoice, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(260, -1), null, 0, false));
        mainPanel = new JPanel();
        mainPanel.setLayout(new CardLayout(0, 0));
        rootPanel.add(mainPanel, BorderLayout.CENTER);
        dictPanel = new JPanel();
        dictPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        dictPanel.setPreferredSize(new Dimension(700, 549));
        mainPanel.add(dictPanel, "dict");
        splitPane = new JSplitPane();
        splitPane.setBackground(new Color(-6750157));
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(1);
        splitPane.setOneTouchExpandable(true);
        splitPane.setOrientation(0);
        dictPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 518), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(7, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setBackground(new Color(-61));
        splitPane.setLeftComponent(panel1);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Параметры поиска", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        partBox = new JCheckBox();
        partBox.setBackground(new Color(-52));
        partBox.setHorizontalAlignment(10);
        partBox.setText("части речи");
        panel1.add(partBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 32), null, 0, false));
        genBox = new JCheckBox();
        genBox.setBackground(new Color(-52));
        genBox.setText("рода");
        panel1.add(genBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 32), null, 0, false));
        numBox = new JCheckBox();
        numBox.setBackground(new Color(-52));
        numBox.setEnabled(true);
        numBox.setText("числа");
        panel1.add(numBox, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 22), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(10);
        label1.setText("Cовпадение");
        panel1.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 14), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(0);
        label2.setText("Максимальное расстояние");
        panel1.add(label2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(39, 14), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setHorizontalAlignment(11);
        label3.setHorizontalTextPosition(11);
        label3.setText("Буквенное");
        panel1.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 14), null, 0, false));
        dlBox = new JComboBox();
        panel1.add(dlBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, 24), null, 0, false));
        prefBox = new JCheckBox();
        prefBox.setBackground(new Color(-52));
        prefBox.setSelected(true);
        prefBox.setText("приставок");
        panel1.add(prefBox, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rootBox = new JCheckBox();
        rootBox.setBackground(new Color(-52));
        rootBox.setSelected(true);
        rootBox.setText("корней");
        panel1.add(rootBox, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Разрешить варьирование");
        panel1.add(label4, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rootsRelatBox = new JCheckBox();
        rootsRelatBox.setBackground(new Color(-52));
        rootsRelatBox.setEnabled(true);
        rootsRelatBox.setHorizontalAlignment(10);
        rootsRelatBox.setHorizontalTextPosition(11);
        rootsRelatBox.setText("Учитывать чередование корней");
        panel1.add(rootsRelatBox, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, 22), null, 2, false));
        suffBox = new JCheckBox();
        suffBox.setBackground(new Color(-52));
        suffBox.setSelected(true);
        suffBox.setText("суффиксов");
        panel1.add(suffBox, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        endingBox = new JCheckBox();
        endingBox.setBackground(new Color(-52));
        endingBox.setSelected(true);
        endingBox.setText("окончаний");
        panel1.add(endingBox, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        includeNotFoundBox = new JCheckBox();
        includeNotFoundBox.setBackground(new Color(-52));
        includeNotFoundBox.setText("Включать слова, не найденные в АОТ");
        includeNotFoundBox.setVisible(false);
        panel1.add(includeNotFoundBox, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(110, 22), null, 0, false));
        includeManualBox = new JCheckBox();
        includeManualBox.setBackground(new Color(-52));
        includeManualBox.setSelected(true);
        includeManualBox.setText("из исходных данных");
        includeManualBox.setVisible(false);
        panel1.add(includeManualBox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Степень искажения ");
        panel1.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        qBox = new JComboBox();
        panel1.add(qBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, 24), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setHorizontalAlignment(11);
        label6.setText("Буквенное между корнями");
        panel1.add(label6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 14), null, 0, false));
        drBox = new JComboBox();
        panel1.add(drBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, 24), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setHorizontalAlignment(4);
        label7.setText("Морфемное");
        panel1.add(label7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 14), null, 0, false));
        dmBox = new JComboBox();
        panel1.add(dmBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, 24), null, 0, false));
        enableDl2Box = new JCheckBox();
        enableDl2Box.setBackground(new Color(-52));
        enableDl2Box.setHorizontalAlignment(10);
        enableDl2Box.setHorizontalTextPosition(10);
        enableDl2Box.setText("Учитывать перестановку букв");
        panel1.add(enableDl2Box, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        formsBox = new JCheckBox();
        formsBox.setBackground(new Color(-52));
        formsBox.setHorizontalAlignment(2);
        formsBox.setHorizontalTextPosition(4);
        formsBox.setText("Словоформы");
        panel1.add(formsBox, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 22), null, 0, false));
        hideBox = new JCheckBox();
        hideBox.setBackground(new Color(-52));
        hideBox.setFont(new Font(hideBox.getFont().getName(), Font.ITALIC, 10));
        hideBox.setHorizontalAlignment(4);
        hideBox.setHorizontalTextPosition(10);
        hideBox.setSelected(true);
        hideBox.setText("Скрывать панель");
        panel1.add(hideBox, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 21), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setRightComponent(panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.setBackground(new Color(-26215));
        panel2.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(568, 391), null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null));
        final JLabel label8 = new JLabel();
        label8.setFont(new Font(label8.getFont().getName(), Font.BOLD, 15));
        label8.setHorizontalAlignment(0);
        label8.setText("Паронимы");
        panel3.add(label8, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(212, 22), null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setFont(new Font(label9.getFont().getName(), label9.getFont().getStyle(), 14));
        label9.setHorizontalAlignment(0);
        label9.setText("Морфемные");
        panel3.add(label9, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(129, 16), null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setFont(new Font(label10.getFont().getName(), label10.getFont().getStyle(), 14));
        label10.setHorizontalAlignment(0);
        label10.setHorizontalTextPosition(11);
        label10.setText("Буквенные");
        panel3.add(label10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(129, 16), null, 0, false));
        morphTabbedPane = new JTabbedPane();
        morphTabbedPane.setTabLayoutPolicy(1);
        panel3.add(morphTabbedPane, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, new Dimension(295, 105), new Dimension(-1, 120), 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setPreferredSize(new Dimension(295, 100));
        morphTabbedPane.addTab("Untitled", panel4);
        letTabbedPane = new JTabbedPane();
        letTabbedPane.setTabLayoutPolicy(1);
        panel3.add(letTabbedPane, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, new Dimension(295, 105), new Dimension(-1, 120), 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setPreferredSize(new Dimension(295, 100));
        letTabbedPane.addTab("Untitled", panel5);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setAutoscrolls(false);
        scrollPane1.setBackground(new Color(-52));
        scrollPane1.setEnabled(true);
        panel3.add(scrollPane1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(295, 154), null, 0, false));
        letParTable = new JTable();
        letParTable.setAutoCreateColumnsFromModel(true);
        letParTable.setBackground(new Color(-52));
        letParTable.setGridColor(new Color(-4868683));
        letParTable.setOpaque(true);
        letParTable.setPreferredScrollableViewportSize(new Dimension(295, 150));
        letParTable.setSelectionBackground(new Color(-103));
        scrollPane1.setViewportView(letParTable);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setBackground(new Color(-52));
        panel3.add(scrollPane2, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(295, 154), null, 0, false));
        morphParTable = new JTable();
        morphParTable.setBackground(new Color(-52));
        morphParTable.setPreferredScrollableViewportSize(new Dimension(295, 150));
        morphParTable.setSelectionBackground(new Color(-103));
        scrollPane2.setViewportView(morphParTable);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel3.add(scrollPane3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        letParInfoPane = new JTextPane();
        letParInfoPane.setEditable(false);
        letParInfoPane.setPreferredSize(new Dimension(295, 100));
        scrollPane3.setViewportView(letParInfoPane);
        final JScrollPane scrollPane4 = new JScrollPane();
        panel3.add(scrollPane4, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        morphParInfoPane = new JTextPane();
        morphParInfoPane.setEditable(false);
        morphParInfoPane.setPreferredSize(new Dimension(295, 100));
        scrollPane4.setViewportView(morphParInfoPane);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel6.setBackground(new Color(-26215));
        panel2.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(568, 10), null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null));
        wordField = new JTextField();
        wordField.setBackground(new Color(-52));
        wordField.setHorizontalAlignment(2);
        wordField.setText("");
        panel6.add(wordField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(279, 22), null, 0, false));
        findParonymsButton = new JButton();
        findParonymsButton.setText("Найти паронимы");
        panel6.add(findParonymsButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setHorizontalAlignment(4);
        label11.setText("Слово");
        panel6.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(15, 14), null, 0, false));
        paramsButton = new JToggleButton();
        paramsButton.setText("Параметры поиска");
        panel6.add(paramsButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        partList = new JComboBox();
        partList.setBackground(new Color(-52));
        panel6.add(partList, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        errorPane = new JTextPane();
        errorPane.setBackground(new Color(-6750157));
        errorPane.setEditable(false);
        errorPane.setForeground(new Color(-52));
        dictPanel.add(errorPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, 15), null, 0, false));
        comparePanel = new JPanel();
        comparePanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        comparePanel.setBackground(new Color(-26215));
        mainPanel.add(comparePanel, "comp");
        compField1 = new JTextField();
        comparePanel.add(compField1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(103, 22), null, 0, false));
        compField2 = new JTextField();
        comparePanel.add(compField2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(103, 22), null, 0, false));
        comparePane = new JTextPane();
        comparePane.setBackground(new Color(-52));
        comparePane.setEditable(false);
        comparePanel.add(comparePane, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 127), null, 0, false));
        compareButton = new JButton();
        compareButton.setText("Сравнить");
        comparePanel.add(compareButton, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        comparePanel.add(scrollPane5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(103, 141), null, 0, false));
        word1Pane = new JTextPane();
        word1Pane.setBackground(new Color(-52));
        word1Pane.setEditable(false);
        scrollPane5.setViewportView(word1Pane);
        final JScrollPane scrollPane6 = new JScrollPane();
        comparePanel.add(scrollPane6, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(6, 141), null, 0, false));
        word2Pane = new JTextPane();
        word2Pane.setBackground(new Color(-52));
        word2Pane.setEditable(false);
        scrollPane6.setViewportView(word2Pane);
        label3.setLabelFor(dlBox);
        label5.setLabelFor(qBox);
        label6.setLabelFor(drBox);
        label7.setLabelFor(dmBox);
        label9.setLabelFor(scrollPane2);
        label10.setLabelFor(scrollPane1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}

class ParonymTableModel extends AbstractTableModel {
    ArrayList<TableData> paronymLists = new ArrayList<TableData>();
    // ArrayList<Word> modifiedParonymList;
    String[] columnNames = { "слово", "ч. р.", "ред. расст.", "q" };
    Object[][] data = new Object[0][4];
    DictionaryGui dictGui;

    public ParonymTableModel(DictionaryGui dg) {
        dictGui = dg;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.length;
    }

    public String getColumnName(int i) {
        return columnNames[i];
    }

    public Class getColumnClass(int columnIndex) {
        // @todo
        switch (columnIndex) {
        case 0:
            return String.class;
        case 1:
            return String.class;
        case 2:
            return Integer.class;
        case 3:
            return Double.class;
        }
        return String.class;
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public void loadData(ArrayList<ParonymList> pLists, boolean let) {
        paronymLists.clear();
        for (ParonymList list : pLists) {
            paronymLists.add(new TableData(list.getWord(), let ? list
                    .getLparonyms() : list.getMparonyms()));

        }

    }

    public void setData(int listNumber, boolean let) {
        if (!paronymLists.isEmpty() && (listNumber >= 0)) {
            TableData list = paronymLists.get(listNumber);
            int i = 0;
            data = new Object[list.getSize()][4];
            for (Paronym p : list.getElements()) {
                // при изменении столбцов таблицы нужно изменить ListComparator
                data[i][0] = dictGui.getEnableForms() ? p.form : p.word;
                data[i][1] = p.partStr;
                data[i][2] = let ? p.params.curDl : p.params.dm;
                data[i][3] = p.params.q;
                i++;
            }
        }
        fireTableDataChanged();

    }

    public void setDefaultData(boolean let) {
        setData(0, let);
    }

    // tabNumber - номер вкладки = номер списка паронимов
    // rowNumber - номер выделенной строки = номер слова в списке
    public String getWordInfo(int rowNumber, int tabNumber) {
        return dictGui.wordInfo(paronymLists.get(tabNumber).getElements()
                .get(rowNumber));

    }

    public int getParonymCount(int listNumber) {
        return paronymLists.get(listNumber).getSize();
    }

    // возвращает число элементов в списке паронимов
    public int getCount(int listN) {
        return paronymLists.get(listN).getSize();
    }

    /*
     * public void formatList(ArrayList<Word> list, int rowN) {
     * Collections.sort(list, new ListComparator(rowN)); }
     * 
     * //уже не нужно private void clearParonymLists() { for (ArrayList<Word>
     * paronymList : paronymLists) { paronymList.clear(); } }
     * 
     * class ListComparator implements Comparator { int column;
     * 
     * public ListComparator(int n) { column = n; }
     * 
     * public int compare(Object w1, Object w2) { Word word1 = (Word) w1; Word
     * word2 = (Word) w2; switch (column) { case 0: return
     * word1.word.compareTo(word2.word); case 1: return
     * word1.part.compareTo(word2.part); case 2: return word1.d > word2.d ? 1 :
     * (word1.d < word2.d ? -1 : 0); } return 0; } }
     */

}

class TableData {
    private Word word;
    private ArrayList<Paronym> elements;
    private int size;

    public TableData(Word word, ArrayList<Paronym> elements) {
        this.word = word;
        this.elements = elements;
        size = elements.size();
    }

    public Word getWord() {
        return word;
    }

    public ArrayList<Paronym> getElements() {
        return elements;
    }

    public int getSize() {
        return size;
    }
}
