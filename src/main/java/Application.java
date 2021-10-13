import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
        //Create a container
        JScrollPane sp = new JScrollPane(table);
        frame.add(sp, BorderLayout.NORTH);
        frame.setSize(300,400);

        JButton button = new JButton("Save");
        JTextField text = new JTextField(20);
        button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String value = text.getText();
                //TODO: handle save file
            }
        });

        //Create timer
        Timer t = new Timer(300, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO:refresh data
            }
        });
        t.start();

        frame.add(text, BorderLayout.CENTER);
        frame.add(button, BorderLayout.SOUTH);

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
//                int[] row = table.getSelectedRows();
//                int[] columns = table.getSelectedColumns();
                int row = table.getRowCount();
                int columns = table.getColumnCount();
                for (int i = 0; i < row; i++) {
                    for (int j = 0; j < columns; j++) {
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
//                        e.getFirstRow();
                int y = table.getSelectedColumn();
//                        e.getColumn();

                String data = (String)table.getValueAt(x, y);
//                System.out.println(spreadsheet.getEvaluations()[x][y]);
                if (spreadsheet.getCell(x, y).getInfo().matches("(.*)(([A-Z]+)(\\d+))(.*)")) {
                    if (!data.equals(spreadsheet.getCell(x, y).getEvaluation())) {
                        spreadsheet.setCell(x, y, data);
                    }

                } else {
                    spreadsheet.setCell(x, y, data);
                }
            }
        };

        table.getModel().addTableModelListener(tl);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

//    private static void update(JTable table, Table spreadsheet) {
//        int[] row = table.getSelectedRows();
//        int[] columns = table.getSelectedColumns();
//        for (int i = 0; i < row.length; i++) {
//            for (int j = 0; j < columns.length; j++) {
//                int x = row[i];
//                int y = columns[j];
//                String data = spreadsheet.getCell(x, y).getInfo();
//                table.setValueAt(data, x, y);
//            }
//        }
//    }

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
