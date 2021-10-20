import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class Application {

    private static Table spreadsheet;
    private static JFrame frame;
    private static JTable table;
    private static boolean reload = false;
    private static int lastSelectedX = -1;
    private static int lastSelectedY = -1;

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        frame = new JFrame("Shmexel Spreadsheet");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int i = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?");
                if (i == JOptionPane.YES_OPTION) {
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
        for (int i = 0; i < rows.length; i++) {
            rows[i] = String.valueOf(i + 1);
        }

        //Name the columns
        String[] columns = new String[spreadsheet.getColumns()];
        for (int i = 0; i < spreadsheet.getColumns(); i++) {
            if (i > ('Z' - 'A')) {
                String prevW = columns[i - 1];
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
            else columns[i] = String.valueOf((char) ('A' + i));
        }

        //Create a table with named columns
        table = new JTable(spreadsheet.getEvaluations(), columns);

        //Create a container
        JScrollPane sp = new JScrollPane(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        //Set row number header
        JList<String> rowHeader = new JList<>(rows);
        rowHeader.setFixedCellWidth(30);
        rowHeader.setFixedCellHeight(table.getRowHeight());
        rowHeader.setCellRenderer(new RowHeaderRenderer(table));
        sp.setRowHeaderView(rowHeader);

        //Show grid lines
        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setGridColor(Color.black);

        //Enable selection
        table.setCellSelectionEnabled(true);
        ListSelectionModel select = table.getSelectionModel();

        //Input text pane
        JTextField inputField = new JTextField(table.getWidth());

        inputField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                String selected = inputField.getSelectedText();
                if (selected != null && selected.matches("(([A-Z]+)(\\d+))")) {
                    int xSelected = Cell.getX(selected);
                    int ySelected = Cell.getY(selected);
                    lastSelectedX = xSelected;
                    lastSelectedY = ySelected;
                    table.getColumnModel().getColumn(ySelected).setCellRenderer(new ColumnColorRenderer(Color.YELLOW, xSelected));
                    table.updateUI();
                } else {
                    if (lastSelectedX != -1 && lastSelectedY != -1) {
                        table.getColumnModel().getColumn(lastSelectedY).setCellRenderer(new ColumnColorRenderer(table.getBackground(), lastSelectedX));
                        table.updateUI();
                    }
                }
            }
        });

        //Get an input from user and set it to a cell
        inputField.addActionListener(e -> {
            String input = inputField.getText();
            int x = table.getSelectedRow();
            int y = table.getSelectedColumn();
            table.setValueAt(input, x, y);
        });

        select.addListSelectionListener(e -> {
            String data;
            int row = table.getRowCount();
            int columns1 = table.getColumnCount();
            int x = table.getSelectedRow();
            int y = table.getSelectedColumn();
            if (x >= 0 && y >= 0)
                table.setValueAt(spreadsheet.getCell(x, y).getInfo(), x, y);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < columns1; j++) {
                    if (i == table.getSelectedRow() && j == table.getSelectedColumn()) {
                        table.setValueAt(spreadsheet.getCell(i, j).getInfo(), i, j);
                        inputField.setText(spreadsheet.getCell(i, j).getInfo());
                        continue;
                    }
                    data = spreadsheet.getCell(i, j).getEvaluation();
                    table.setValueAt(data, i, j);
                    table.getColumnModel().getColumn(j).setCellRenderer(new ColumnColorRenderer(table.getBackground(), i));
                }
            }
        });

        table.getModel().addTableModelListener(e -> {
            int x = table.getSelectedRow();
            int y = table.getSelectedColumn();

            if (x >= 0 && y >= 0) {
                String data = (String) table.getValueAt(x, y);
                if (spreadsheet.getCell(x, y).getInfo().matches("(.*)(([A-Z]+)(\\d+))(.*)")) {
                    if (!data.equals(spreadsheet.getCell(x, y).getInfo()) && !data.equals(spreadsheet.getCell(x, y).getEvaluation())) {
                        spreadsheet.setCell(x, y, data);
                    }
                } else if (!data.matches("Infinity"))
                    spreadsheet.setCell(x, y, data);
            }
        });

        JLabel label = new JLabel("Enter a file path and name (with extension \".shm\"):");
        JTextField text = new JTextField(25);

        //Save button
        JButton saveButton = new JButton("save");
        saveButton.addActionListener(e -> {
            //save file
            if (save(spreadsheet, text.getText())) {
                if (!text.getText().endsWith(".shm"))
                    JOptionPane.showMessageDialog(frame, "Your file has been successfully saved to " + text.getText() + ".shm .");
                else
                    JOptionPane.showMessageDialog(frame, "Your file has been successfully saved to " + text.getText() + " .");
            } else
                JOptionPane.showMessageDialog(frame, "Did not save the file. Please enter another file name.", "Alert", JOptionPane.WARNING_MESSAGE);
            text.setText("");
        });

        //Load button
        JButton loadButton = new JButton("load");
        loadButton.addActionListener(e -> {
            //load file
            if (load(text.getText())) {
                if (!text.getText().endsWith(".shm"))
                    JOptionPane.showMessageDialog(frame, "Your file has been successfully loaded from " + text.getText() + ".shm .");
                else
                    JOptionPane.showMessageDialog(frame, "Your file has been successfully loaded from " + text.getText() + " .");
                reload = true;
                createAndShowGUI();
            } else
                JOptionPane.showMessageDialog(frame, "Could not load the file. Please enter another file name.", "Alert", JOptionPane.WARNING_MESSAGE);
            text.setText("");
        });

        //Reset button
        JButton resetButton = new JButton("reset");
        resetButton.addActionListener(e -> {
            if (reset()) {
                spreadsheet.save();
                Table temp = new Table(spreadsheet.getRows(), spreadsheet.getColumns()).copyTable(spreadsheet);
                spreadsheet = new Table(spreadsheet.getRows(), spreadsheet.getColumns());
                spreadsheet.setSavedTable(temp.getSavedTable());
                spreadsheet.setPrevTable(temp.getPrevTable());
            }
            table.selectAll();
            table.clearSelection();
        });

        //Undo button
        JButton undoButton = new JButton("undo");
        undoButton.addActionListener(e -> {
            if (spreadsheet.getSavedTable() != null) spreadsheet = spreadsheet.getSavedTable();
            table.selectAll();
            table.clearSelection();
        });

        //Bottom panel for save/load/reset/undo buttons and an input file path text.
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(label);
        bottomPanel.add(text);
        bottomPanel.add(saveButton);
        bottomPanel.add(loadButton, BorderLayout.AFTER_LINE_ENDS);
        bottomPanel.add(resetButton);
        bottomPanel.add(undoButton);
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setPreferredSize(new Dimension(table.getWidth(), 100));

        frame.add(inputField, BorderLayout.NORTH);
        frame.add(sp, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setMinimumSize(new Dimension(500, 500));

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * The method resets the table (can be undone with UNDO button)
     * @return true if YES option was selected
     */
    private static boolean reset() {
        int n = JOptionPane.showConfirmDialog(
                frame,
                "Reset will erase all data from table\n" +
                        "Are you sure you want to reset it?",
                "Reset confirmation",
                JOptionPane.YES_NO_OPTION);
        return n != JOptionPane.NO_OPTION;
    }

    /**
     * Load a file from the given path and set entries' information accordingly.
     * @param path - input file path
     * @return true if the load was successful
     *         else return false
     */
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
                String[] rowInfo = strings.get(i).split("(] )");
                for (int j = 0; j < columns; j++) {
                    if (rowInfo[j].equals("#")) {
                        infos[i - 1][j] = "#";
                        break;
                    }
                    infos[i - 1][j] = rowInfo[j].substring(1);
                }
            }
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if (infos[i][j].equals("#")) break;
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

    /**
     * The method saves a table;
     * if the line is empty starting from the beginning or a specific index -
     * write #.
     * Else replace an empty input with [-] sign.
     * Every cell's information is saves within [] brackets.
     * @param spreadsheet - spreadsheet to save
     * @param path - local file path
     * @return true if the save was successful
     *         else return false
     */
    private static boolean save(Table spreadsheet, String path) {
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
            //Write how many rows and columns in table
            printWriter.println("rows=" + spreadsheet.getRows() + " columns=" + spreadsheet.getColumns());
            //Write table cells' info: # if the line is empty
            StringBuilder sb = new StringBuilder();
            boolean emptyLine = true;
            for (int i = 0; i < spreadsheet.getRows(); i++) {
                for (int j = spreadsheet.getColumns() - 1; j >= 0; j--) {
                    if (emptyLine && !spreadsheet.getCell(i, j).getInfo().equals("")) {
                        sb.append("#" + " ").append(new StringBuilder("[" + spreadsheet.getCell(i, j).getInfo() + "]")
                                .reverse()).append(" ");
                        emptyLine = false;
                    } else if (!emptyLine && spreadsheet.getCell(i, j).getInfo().equals(""))
                        sb.append(new StringBuilder("[" + "-" + "]").reverse()).append(" ");
                    else if (!emptyLine && !spreadsheet.getCell(i, j).getInfo().equals(""))
                        sb.append(new StringBuilder("[" + spreadsheet.getCell(i, j).getInfo() + "]").reverse()).append(" ");
                }
                if (sb.reverse().toString().startsWith(" ")) sb.deleteCharAt(0);
                if (emptyLine) printWriter.println("#");
                else {
                    printWriter.println(sb);
                    sb.delete(0, sb.toString().length());
                    emptyLine = true;
                }
            }
            printWriter.close();
            fw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * The row header renderer creates a row header
     */
    @SuppressWarnings("rawtypes")
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
     * The column color renderer changes background of specific cell
     */
    static class ColumnColorRenderer extends DefaultTableCellRenderer {
        Color backgroundColor;
        int row;

        public ColumnColorRenderer(Color backgroundColor, int row) {
            super();
            this.backgroundColor = backgroundColor;
            this.row = row;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setForeground(table.getForeground());
            if (row == this.row)
                cell.setBackground(backgroundColor);
            else if (row == table.getSelectedRow() && column == table.getSelectedColumn())
                cell.setBackground(Color.GREEN);
            else
                cell.setBackground(table.getBackground());
            return cell;
        }
    }
}
