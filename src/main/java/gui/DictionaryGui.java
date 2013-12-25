package gui;

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
 * Created by IntelliJ IDEA.
 * User: Таня
 * Date: 03.03.2009
 * Time: 9:23:42
 * To change this template use File | Settings | File Templates.
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

    //query Parameters JComponents
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


    //query Parameters variables
    boolean enableForms = false;

    int maxdl = -1;
    int maxdm = -1;
    int maxdr = -1;

    double q = Double.MAX_VALUE;

    //константы для работы с таблицами
    final boolean LETTER = true;
    final boolean MORPHEM = false;


    //возможно пригодится для истории запросов
    ArrayList<ParonymTableModel> modelList = new ArrayList<ParonymTableModel>();

    ParonymTableModel letModel = new ParonymTableModel(this);
    ParonymTableModel morphModel = new ParonymTableModel(this);

    private TableRowSorter<ParonymTableModel> letSorter = new TableRowSorter<ParonymTableModel>(letModel);
    private TableRowSorter<ParonymTableModel> morphSorter = new TableRowSorter<ParonymTableModel>(morphModel);


    public DictionaryGui(GuiCtrl control) {


        ctrl = control;
        ctrl.regDictionaryGui(this);

        //tables
        //@todo закладка
        letParTable.setModel(letModel);
        morphParTable.setModel(morphModel);


        letParTable.setRowSorter(letSorter);
        morphParTable.setRowSorter(morphSorter);

        letTabbedPane.removeAll();
        morphTabbedPane.removeAll();

        //listeners
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

                letParInfoPane.setText(
                        letModel.getWordInfo(
                                letSorter.convertRowIndexToModel(letParTable.getSelectedRow()),
                                letTabbedPane.getSelectedIndex()));
            }
        });

        morphParTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                morphParInfoPane.setText(
                        morphModel.getWordInfo(
                                morphSorter.convertRowIndexToModel(morphParTable.getSelectedRow()),
                                morphTabbedPane.getSelectedIndex()));
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
                splitPane.setDividerLocation(paramsButton.isSelected() ? 250 : 0);
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
                compList = ctrl.executeCompareAction(f1,f2);
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
                comparePane.setText("Редакционное расстояние в буквах: " + compare.dl);

            } else {
                word1Pane.setText(wordInfo(compare.word1));
                word2Pane.setText(wordInfo(compare.word2));
                String dmStr = compare.dm == -1 ? "" : "Редакционное расстояние в морфах: " + compare.dm + "\n";
                comparePane.setText("Редакционное расстояние в буквах: " + compare.dl + "\n" +
                         dmStr +
                        "Различия: \n" + compare.diffStr);
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
        ArrayList<ParonymList> paronymLists;//cписок списков паронимов
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

            queryParams = new QueryParameters(
                    maxdl, maxdm, maxdr, q,
                    enableForms,
                    partBox.isSelected(), genBox.isSelected(), numBox.isSelected(),
                    rootsRelatBox.isSelected(),
                    prefBox.isSelected(), rootBox.isSelected(), suffBox.isSelected(), endingBox.isSelected(),
                    enableDl2Box.isSelected(),
                    false,
                    true, getListInt((String)partList.getSelectedItem()));
            paronymLists = ctrl.executeFindParonymsButton(word, queryParams);
            if (paronymLists.isEmpty()) {
                //@todo слово не найдено
                throw new ParonymsNotFoundException("Паронимов к слову «" + word + "» не найдено ");
            }

            //паронимы найдены. обновить данные
            letTabbedPane.removeAll();
            morphTabbedPane.removeAll();

            //загрузить данные о паронимах в модель

            letModel.loadData(paronymLists, LETTER);
            //отобразить данные по умолчанию (для 1ого варианта заданного слова)
            letModel.setDefaultData(LETTER);

            morphModel.loadData(paronymLists, MORPHEM);
            morphModel.setDefaultData(MORPHEM);

            //загрузка данных в инфо-панель
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
        tabbedPane.add(curWord.partStr + " (" + (let ?
                letModel.getParonymCount(tabbedPane.getTabCount()) :
                morphModel.getParonymCount(tabbedPane.getTabCount()))
                + ")", pane);

    }

    public String wordInfo(Word curWord) {
        String text = "";
        if (enableForms) {
            text = text.concat("Словоформа: " + curWord.form + "\n" + curWord.gram + "\n");
        }
        text = text.concat("Норм. форма: " + curWord.word + "\n" + curWord.getWordInfo());
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

}

class ParonymTableModel extends AbstractTableModel {
    ArrayList<TableData> paronymLists = new ArrayList<TableData>();
    //ArrayList<Word> modifiedParonymList;
    String[] columnNames = {"слово", "ч. р.", "ред. расст.", "q"};
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
        //@todo
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
            paronymLists.add(new TableData(list.getWord(), let ? list.getLparonyms() : list.getMparonyms()));

        }

    }

    public void setData(int listNumber, boolean let) {
        if (!paronymLists.isEmpty() && (listNumber >= 0)) {
           TableData list = paronymLists.get(listNumber);
            int i = 0;
            data = new Object[list.getSize()][4];
            for (Paronym p : list.getElements()) {
                //при изменении столбцов таблицы нужно изменить ListComparator
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

    //tabNumber - номер вкладки = номер списка паронимов
    // rowNumber - номер выделенной строки = номер слова в списке
    public String getWordInfo(int rowNumber, int tabNumber) {
        return dictGui.wordInfo(paronymLists.get(tabNumber).getElements().get(rowNumber));

    }

    public int getParonymCount(int listNumber) {
        return paronymLists.get(listNumber).getSize();
    }

    //возвращает число элементов в списке паронимов
    public int getCount(int listN) {
        return paronymLists.get(listN).getSize();
    }

    /*
    public void formatList(ArrayList<Word> list, int rowN) {
        Collections.sort(list, new ListComparator(rowN));
    }

    //уже не нужно
    private void clearParonymLists() {
        for (ArrayList<Word> paronymList : paronymLists) {
            paronymList.clear();
        }
    }

    class ListComparator implements Comparator {
        int column;

        public ListComparator(int n) {
            column = n;
        }

        public int compare(Object w1, Object w2) {
            Word word1 = (Word) w1;
            Word word2 = (Word) w2;
            switch (column) {
                case 0:
                    return word1.word.compareTo(word2.word);
                case 1:
                    return word1.part.compareTo(word2.part);
                case 2:
                    return word1.d > word2.d ? 1 : (word1.d < word2.d ? -1 : 0);
            }
            return 0;
        }
    }

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


