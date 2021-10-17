import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Application {

    private static Table spreadsheet;
    private static String[] columns;
    private static JFrame frame;
    private static JTable table;
    private static JScrollPane sp;
    private static boolean reload = false;

    private static class RowHeaderRenderer extends JLabel implements ListCellRenderer {

        RowHeaderRenderer(JTable table) {
            //adjust cell size
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setFont(table.getTableHeader().getFont());
            setBackground(Color.white);
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */

    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        frame = new JFrame("Shmexel Spreadsheet");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(new ImageIcon("spreadsheet.png").getImage());
        frame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                int i = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?");
                if(i == JOptionPane.YES_OPTION){
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }
        });

        if (!reload) {

            String rowsAmount = JOptionPane.showInputDialog("Enter the rows amount (if not set it will be set to 20): ");
            if (rowsAmount == null || !rowsAmount.matches("\\d+")) {
                rowsAmount = "20";
            }

            String columnsAmount = JOptionPane.showInputDialog("Enter the columns amount (if not set it will be set to 20): ");
            if (columnsAmount == null || !columnsAmount.matches("\\d+")) {
                columnsAmount = "20";
            }

            spreadsheet = new Table(Integer.parseInt(rowsAmount), Integer.parseInt(columnsAmount));
        }
            //Name the rows
        String[] rows = new String[spreadsheet.getRows()];
        for (int i = 0; i < rows.length; i ++) {
            rows[i] = String.valueOf(i+1);
        }

        //Name the columns
        columns = new String[spreadsheet.getColumns()];
        for (int i = 0; i < spreadsheet.getColumns(); i++) {
            if (i > ('Z' - 'A')) {
                String prevW = columns[i-1];
                int temp = prevW.length() - 1;
                for (int j = prevW.length() - 1; j >= 0; j--) {
                    if (prevW.charAt(j) != 'Z') {
                        char[] chars = prevW.toCharArray();
                        chars[j] += 1;
                        for (j = j + 1; j <= prevW.length() - 1; j++) chars[j] = 'A';
                        columns[i] = String.valueOf(chars);
                        break;
                    }
                    temp = j;
                }
                if (temp <= 0) {
                    char[] chars = new char[prevW.length() + 1];
                    Arrays.fill(chars, 'A');
                    columns[i] = String.valueOf(chars);
                }
            } else if (i == 0) columns[i] = String.valueOf('A');
            else columns[i] = String.valueOf((char)('A' + i));
        }

        //Create a table with named columns
        table = new JTable(spreadsheet.getEvaluations(), columns);
        table.setSelectionBackground(Color.blue);

        //Create a container
        sp = new JScrollPane(table);
        frame.add(sp, BorderLayout.CENTER);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        //Set row number header
        JList rowHeader = new JList(rows);
        rowHeader.setFixedCellWidth(30);
        rowHeader.setCellRenderer(new RowHeaderRenderer(table));
        sp.setRowHeaderView(rowHeader);

        //Show grid lines
        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setGridColor(Color.black);

        //Enable selection
        table.setCellSelectionEnabled(true);
        ListSelectionModel select = table.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        select.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String data = "";
                int row = table.getRowCount();
                int columns = table.getColumnCount();
                int x = table.getSelectedRow();
                int y = table.getSelectedColumn();
                table.setValueAt(spreadsheet.getCell(x, y).getInfo(), x, y);
                for (int i = 0; i < row; i++) {
                    for (int j = 0; j < columns; j++) {
                        if (i == table.getSelectedRow() && j == table.getSelectedColumn()) {
                            table.setValueAt(spreadsheet.getCell(i, j).getInfo(), i, j);
                            continue;
                        }
                        data = spreadsheet.getCell(i, j).getEvaluation();
                        table.setValueAt(data, i, j);
                    }
                }
            }
        });

        TableModelListener tl = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int x = table.getSelectedRow();
                int y = table.getSelectedColumn();

                String data = (String)table.getValueAt(x, y);
                if (spreadsheet.getCell(x, y).getInfo().matches("(.*)(([A-Z]+)(\\d+))(.*)")) {
                    if (!data.equals(spreadsheet.getCell(x, y).getInfo()) && !data.equals(spreadsheet.getCell(x, y).getEvaluation())) {
                        spreadsheet.setCell(x, y, data);
                    }
                } else {
                    spreadsheet.setCell(x, y, data);
                }
            }
        };

        table.getModel().addTableModelListener(tl);

        //TODO: handle save/load file

        JButton saveButton = new JButton("save");
        JButton loadButton = new JButton("load");
        JLabel label = new JLabel("Enter a file path and name (with extension \".shm\"):");
        JTextField text = new JTextField("", 40);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //save file
                if(save(table, spreadsheet, text.getText())) {
                    if (!text.getText().endsWith(".shm")) JOptionPane.showMessageDialog(frame,"Your file has been successfully saved to " + text.getText() + ".shm .");
                    else JOptionPane.showMessageDialog(frame,"Your file has been successfully saved to " + text.getText() + " .");
                }
                else JOptionPane.showMessageDialog(frame,"Did not save the file. Please enter another file name.","Alert",JOptionPane.WARNING_MESSAGE);
            }
        });

        loadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //load file
                if(load(text.getText())) {
                    if (!text.getText().endsWith(".shm"))
                        JOptionPane.showMessageDialog(frame, "Your file has been successfully loaded from " + text.getText() + ".shm .");
                    else
                        JOptionPane.showMessageDialog(frame, "Your file has been successfully loaded from " + text.getText() + " .");
                reload = true;
                createAndShowGUI();
                }
                else JOptionPane.showMessageDialog(frame,"Could not load the file. Please enter another file name.","Alert",JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel MyPanel = new JPanel();
        MyPanel.add(label, "NORTH");
        MyPanel.add(text, "CENTER");
        MyPanel.add(saveButton, "EAST");
        MyPanel.add(loadButton, BorderLayout.AFTER_LINE_ENDS);
        MyPanel.setBackground(Color.WHITE);
        MyPanel.setPreferredSize(new Dimension(table.getWidth(), 100));

        frame.add(MyPanel, BorderLayout.SOUTH);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static boolean load(String path) {
        if (!path.endsWith(".shm")) path = path + ".shm";
        File file = new File(path);
        if (!file.exists()) return false;
        try {
            List<String> strings = Files.readAllLines(Path.of(path));

            int rows = Integer.parseInt(strings.get(0).substring(strings.get(0).indexOf("=") + 1, strings.get(0).indexOf(" ")));
            int columns = Integer.parseInt(strings.get(0).substring(strings.get(0).lastIndexOf("=") + 1));
            spreadsheet = new Table(rows, columns);

            String[][] infos = new String[rows][columns];
            for (int i = 1; i < strings.size(); i++) {
                String[] rowInfo = strings.get(i).split(" ");
                for (int j = 0; j < columns; j++) {
                    infos[i-1][j] = rowInfo[j];
                }
            }
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (infos[i][j].equals("-")) continue;
                    spreadsheet.setCell(i, j, infos[i][j]);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean save(JTable table, Table spreadsheet, String path) {
        if (!path.endsWith(".shm")) path = path + ".shm";
        File file = new File(path);
        if (file.exists()) {
            int n = JOptionPane.showConfirmDialog(
                    frame,
                    "This file does already exist.\n" +
                    "Are you sure you want to overwrite it?",
                    "File overwrite confirmation",
                    JOptionPane.YES_NO_OPTION);
            if (n == JOptionPane.NO_OPTION) return false;
        }
        try {
            FileWriter fw = new FileWriter(path);
            PrintWriter printWriter = new PrintWriter(fw);
            //write how many rows and columns in table
            printWriter.print("rows=" + spreadsheet.getRows() + " columns=" + spreadsheet.getColumns());
            printWriter.print(System.lineSeparator());
//            String[][] infos = new String[spreadsheet.getRows()][spreadsheet.getColumns()];
//            for (int i = 0; i < spreadsheet.getRows(); i++) {
//                for (int j = 0; j < spreadsheet.getColumns(); j++) {
//                    if (spreadsheet.getCell(i, j).getInfo().equals("")) infos[i][j] = "-";
//                    else infos[i][j] = spreadsheet.getCell(i, j).getInfo();
//                }
//            }
            StringBuilder sb = new StringBuilder();
            boolean emptyInfo = true;
            //write table cells' info
            for (int i = 0; i < spreadsheet.getRows(); i++) {
                for (int j = spreadsheet.getColumns() - 1; j >= 0; j--) {
                    if (spreadsheet.getCell(i, j).getInfo().equals("") && emptyInfo) continue;
                    else if (spreadsheet.getCell(i, j).getInfo().equals("") && !emptyInfo) sb.append("- ");
                    else if (!spreadsheet.getCell(i, j).getInfo().equals("")) {
                        if (j != spreadsheet.getColumns() - 1 && spreadsheet.getCell(i, j + 1).getInfo().equals("")) {
                            sb.append("#");
                            emptyInfo = false;
                        }
                        else sb.append(spreadsheet.getCell(i, j).getInfo());
                    }
                }
                System.out.println(sb);
                printWriter.println(sb.reverse());
            }
            printWriter.close();
            fw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
