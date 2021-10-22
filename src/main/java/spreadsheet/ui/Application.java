package spreadsheet.ui;

import spreadsheet.table.Cell;
import spreadsheet.table.Table;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

@SuppressWarnings("unchecked")
public class Application {

    private static Table spreadsheet;
    private static JFrame frame;
    private static JTable table;
    private static boolean reload = false;
    private static int lastSelectedX = -1;
    private static int lastSelectedY = -1;
    private static JFileChooser fileChooser;
    private static String pathToSave;
    private static String pathToLoad;
    private static JTextField inputField;
    private static int prevRow = -1;
    private static int prevCol = -1;
    private static boolean first = true;
    private static boolean undo = false;

    private static final int BOTTOM_PANEL_HEIGHT = 40;
    private static final int FRAME_MINIMUM_SIZE = 500;
    private static final int INPUT_FIELD_COLUMNS = 25;
    private static final int CELL_WIDTH = 30;
    private static final String REFERENCE_REGEX = "(([A-Z]+)(\\d+))";
    private static final String DEFAULT_AMOUNT = "20";

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public static void createAndShowGUI() {
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
            JTextField rowsAmount = new JTextField();
            JTextField columnsAmount = new JTextField();
            Object[] message = {
                    """
Please enter amount of rows and columns\s
(default values are 20)\s
""",
                    "Rows:", rowsAmount,
                    "Columns:", columnsAmount
            };
            int option = JOptionPane.showConfirmDialog(null, message,
                    "Set rows and columns", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                if (rowsAmount.getText() == null || !rowsAmount.getText().matches("\\d+")) {
                    rowsAmount.setText(DEFAULT_AMOUNT);
                }
                if (columnsAmount.getText() == null || !columnsAmount.getText().matches("\\d+")) {
                    columnsAmount.setText(DEFAULT_AMOUNT);
                }
                spreadsheet = new Table(Integer.parseInt(rowsAmount.getText()), Integer.parseInt(columnsAmount.getText()));
            } else {
                frame.dispose();
                return;
            }
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
        rowHeader.setFixedCellWidth(CELL_WIDTH);
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
        inputField = new JTextField(table.getWidth());

        inputField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                String selected = inputField.getSelectedText();
                if (lastSelectedX != -1 && lastSelectedY != -1) {
                    table.getColumnModel().getColumn(lastSelectedY)
                            .setCellRenderer(new ColumnColorRenderer(table.getBackground(), lastSelectedX));
                    table.updateUI();
                }
                if (selected != null) {
                    selected = selected.toUpperCase(Locale.ROOT);
                    if (selected.matches(REFERENCE_REGEX)) {
                        int xSelected = Cell.getX(selected);
                        int ySelected = Cell.getY(selected);
                        table.getColumnModel().getColumn(ySelected)
                                .setCellRenderer(new ColumnColorRenderer(Color.YELLOW, xSelected));
                        spreadsheet.getCell(xSelected, ySelected).getSpreadsheet().getActiveCells()
                                .add(spreadsheet.getCell(xSelected, ySelected));
                        table.updateUI();
                        lastSelectedX = xSelected;
                        lastSelectedY = ySelected;
                    }
                }
            }
        });
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String input = inputField.getText();
                    int x = table.getSelectedRow();
                    int y = table.getSelectedColumn();
                    table.setValueAt(input, x, y);
                    externalCellSelection();
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

        select.addListSelectionListener(e ->
                externalCellSelection());

        table.getModel().addTableModelListener(e -> {
            int x = table.getSelectedRow();
            int y = table.getSelectedColumn();

            if (x >= 0 && y >= 0) {
                String data = (String) table.getValueAt(x, y);
                if (spreadsheet.getCell(x, y).getInfo().startsWith("=")) {
                    if (!data.equals(spreadsheet.getCell(x, y).getInfo())
                            && !data.equals(spreadsheet.getCell(x, y).getEvaluation())) {
                        spreadsheet.setCell(x, y, data);
                    }
                } else if (!data.matches("Infinity"))
                    spreadsheet.setCell(x, y, data);
            }
        });

        fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Shmexel Spreadsheet document"
                , "shm"));

        //Save button
        JButton saveButton = new JButton("save");
        saveButton.addActionListener(e -> {
            //save file
            if (save(spreadsheet))
                JOptionPane.showMessageDialog(frame, "Your file has been successfully saved to " + pathToSave + ".");
            else
                JOptionPane.showMessageDialog(frame, "Save failed."
                        , "Alert", JOptionPane.WARNING_MESSAGE);
        });

        //Load button
        JButton loadButton = new JButton("load");
        loadButton.addActionListener(e -> {
            //load file
            if (load()) {
                JOptionPane.showMessageDialog(frame, "Your file has been successfully loaded from " + pathToLoad + ".");
                frame.setVisible(false);
                frame.dispose();
                reload = true;
                first = true;
                createAndShowGUI();
                spreadsheet.setPrevTable(null);
                spreadsheet.setSavedTable(null);
            } else
                JOptionPane.showMessageDialog(frame, "Load failed."
                        , "Alert", JOptionPane.WARNING_MESSAGE);
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
                undo = true;
                externalCellSelection();
                table.updateUI();
                table.selectAll();
                table.clearSelection();
            }
        });

        //Undo button
        JButton undoButton = new JButton("undo");
        undoButton.addActionListener(e -> {
            if (spreadsheet.getSavedTable() != null) spreadsheet = spreadsheet.getSavedTable();
            undo = true;
            externalCellSelection();
            table.updateUI();
            table.selectAll();
            table.clearSelection();
        });

        //Upper pane; for input text field, undo and reset buttons
        JPanel upperPanel = new JPanel();
        inputField.setColumns(INPUT_FIELD_COLUMNS);
        upperPanel.add(inputField, BorderLayout.WEST);
        upperPanel.add(undoButton);
        upperPanel.add(resetButton);
        upperPanel.setBackground(Color.WHITE);

        //Bottom panel for save and load buttons
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(saveButton);
        bottomPanel.add(loadButton);
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setPreferredSize(new Dimension(table.getWidth(), BOTTOM_PANEL_HEIGHT));

        frame.add(upperPanel, BorderLayout.NORTH);
        frame.add(sp, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setMinimumSize(new Dimension(FRAME_MINIMUM_SIZE, FRAME_MINIMUM_SIZE));

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static void externalCellSelection() {
        String data;
        int row = table.getRowCount();
        int columns1 = table.getColumnCount();
        int x = table.getSelectedRow();
        int y = table.getSelectedColumn();
        if (first || undo) {
            table.setCellSelectionEnabled(false);
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < columns1; j++) {
                    if (undo) {
                        if (i == table.getSelectedRow() && j == table.getSelectedColumn()) {
                            table.setValueAt(spreadsheet.getCell(i, j).getInfo(), i, j);
                            inputField.setText(spreadsheet.getCell(i, j).getInfo());
                            continue;
                        }
                        data = spreadsheet.getCell(i, j).getEvaluation();
                        table.setValueAt(data, i, j);
                    }
                    table.getColumnModel().getColumn(j)
                            .setCellRenderer(new ColumnColorRenderer(table.getBackground(), i));
                }
            }
            if (reload) {
                spreadsheet.setPrevTable(null);
                spreadsheet.setSavedTable(null);
            }
            undo = false;
            first = false;
            reload = false;
            table.setCellSelectionEnabled(true);
        }
        if (prevCol != -1 && prevRow != -1 && prevRow != x && prevCol != y) {
            table.getColumnModel().getColumn(prevCol)
                    .setCellRenderer(new ColumnColorRenderer(table.getBackground(), prevRow));
        }

        if (x >= 0 && y >= 0) {
            table.setValueAt(spreadsheet.getCell(x, y).getInfo(), x, y);
            inputField.setText(spreadsheet.getCell(x, y).getInfo());
            table.getColumnModel().getColumn(y)
                    .setCellRenderer(new ColumnColorRenderer(Color.GREEN, x));
            if (lastSelectedX != -1 && lastSelectedY != -1)
                table.getColumnModel().getColumn(lastSelectedY)
                        .setCellRenderer(new ColumnColorRenderer(table.getBackground(), lastSelectedX));
        }

        Set<Cell> cells = spreadsheet.getActiveCells();
        Cell[] cells1 = new Cell[cells.size()];
        int i = 0;
        for (Cell c : cells) cells1[i++] = c;
        for (Cell cell : cells1) {
            if (cell.getX() == x && cell.getY() == y) continue;
            data = cell.getEvaluation();
            table.setValueAt(data, cell.getX(), cell.getY());
            table.getColumnModel().getColumn(cell.getY())
                    .setCellRenderer(new ColumnColorRenderer(table.getBackground(), cell.getX()));

        }
    }

    /**
     * The method resets the table (can be undone with UNDO button)
     *
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
     *
     * @return true if the load was successful
     * else return false
     */
    private static boolean load() {
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            pathToLoad = fileChooser.getSelectedFile().getAbsolutePath();
            File file = new File(pathToLoad);
            if (!file.exists()) return false;
            try {
                List<String> strings = Files.readAllLines(Path.of(pathToLoad));

                int rows = Integer.parseInt(strings.get(0)
                        .substring(strings.get(0).indexOf("=") + 1, strings.get(0).indexOf(" ")));
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
        } else return false;
    }

    /**
     * The method saves a table;
     * if the line is empty starting from the beginning or a specific index -
     * write #.
     * Else replace an empty input with [-] sign.
     * Every cell's information is saves within [] brackets.
     *
     * @param spreadsheet - spreadsheet to save
     * @return true if the save was successful
     * else return false
     */
    private static boolean save(Table spreadsheet) {
        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (fileToSave.getAbsolutePath().endsWith(".shm"))
                pathToSave = fileToSave.getAbsolutePath();
            else
                pathToSave = fileToSave.getAbsolutePath() + ".shm";
            if (!fileToSave.getAbsolutePath().endsWith(".shm"))
                fileToSave = new File(pathToSave);
            if (fileToSave.exists()) {
                int n = JOptionPane.showConfirmDialog(
                        frame,
                        "This file does already exist.\n" +
                                "Are you sure you want to overwrite it?",
                        "File overwrite confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.NO_OPTION) return false;
            }
            try {
                FileWriter fw = new FileWriter(pathToSave);
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
        } else return false;
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

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected
                , boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            cell.setForeground(table.getForeground());
            if (prevRow == -1 && prevCol == -1) {
                prevRow = table.getSelectedRow();
                prevCol = table.getSelectedColumn();
            }
            if (row == this.row && backgroundColor.equals(Color.YELLOW)) {
                cell.setBackground(backgroundColor);
                if (isSelected && (prevRow != table.getSelectedRow() || prevCol != table.getSelectedColumn())) {
                    prevRow = table.getSelectedRow();
                    prevCol = table.getSelectedColumn();
                    externalCellSelection();
                }
            } else if (row == table.getSelectedRow() && column == table.getSelectedColumn()) {
                cell.setBackground(Color.GREEN);
                if (prevRow != row || prevCol != column) {
                    prevRow = row;
                    prevCol = column;
                    externalCellSelection();
                }
            } else
                cell.setBackground(table.getBackground());

            return cell;
        }
    }
}
