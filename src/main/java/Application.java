import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Application {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("Shmexel Spreadsheet");
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

//        JOptionPane.showMessageDialog(frame,"Hello, Welcome to Javatpoint.");

        //Name the columns
        Table spreadsheet = new Table();
        String[] columns = new String[spreadsheet.getColumns()];
        char letter = 'A';
        for (int i = 0; i < spreadsheet.getColumns(); i++) {
            columns[i] = String.valueOf(letter);
            letter++;
        }

        //Create a table with named columns
        JTable table = new JTable(spreadsheet.getEvaluations(), columns);
        table.setBounds(30,40,300,400);
        table.setSelectionBackground(Color.blue);
        //Create a container
        JScrollPane sp = new JScrollPane(table);
        frame.add(sp, BorderLayout.NORTH);
        frame.setSize(300,400);

        //Create timer
//        Timer t = new Timer(10, new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                String data = "";
//                int row = table.getRowCount();
//                int columns = table.getColumnCount();
//                for (int i = 0; i < row; i++) {
//                    for (int j = 0; j < columns; j++) {
//                        if (i == table.getSelectedRow() && j == table.getSelectedColumn()) {
//                            table.setValueAt(spreadsheet.getCell(i, j).getInfo(), i, j);
//                            continue;
//                        }
//                        data = spreadsheet.getCell(i, j).getEvaluation();
//                        System.out.println("EVAL " + data);
//                        table.setValueAt(data, i, j);
//                    }
//                }
//            }
//        });
//        t.start();

        //Show grid lines
        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setGridColor(Color.black);

        //Enable selection
        table.setCellSelectionEnabled(true);
        ListSelectionModel select = table.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        select.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                String data = "";
                int row = table.getRowCount();
                int columns = table.getColumnCount();
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
//                    System.out.println("SET CELL : " + x + " " + y + " : " + data);
                    spreadsheet.setCell(x, y, data);
                    System.out.println(spreadsheet.getCell(x, y).getEvaluation());
                }
            }
        };

        table.getModel().addTableModelListener(tl);

        //TODO: handle save/load file

        JButton button = new JButton("save");
        JTextField text = new JTextField("Enter the file name: ", 20);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = text.getText();
                value = value.substring(value.indexOf(":") + 2);
                //save file
                if(save(table, spreadsheet, value))
                    JOptionPane.showMessageDialog(frame,"Your file has been successfully saved to " + value+ ".shm" + " .");
            }
        });

        frame.add(text, BorderLayout.CENTER);
        frame.add(button, BorderLayout.SOUTH);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static boolean save(JTable table, Table spreadsheet, String name) {
        name = name + ".shm";
        int columns = table.getColumnCount();
        try {
            FileWriter fw = new FileWriter(name);
            PrintWriter printWriter = new PrintWriter(fw);
            //write column headers
            for (int i = 0; i < columns; i++) {
                printWriter.print(table.getModel().getColumnName(i) + " ");
            }
            printWriter.print(System.lineSeparator());
            printWriter.print("rows=" + spreadsheet.getRows() + " columns=" + spreadsheet.getColumns());
            printWriter.print(System.lineSeparator());
            for (int i = 0; i < spreadsheet.getRows(); i++) {
                for (int j = 0; j < spreadsheet.getColumns(); j++) {
                    printWriter.print(spreadsheet.getCell(i, j).getInfo() + " ");
                }
                printWriter.print(System.lineSeparator());
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
