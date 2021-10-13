import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

        Table spreadsheet = new Table();
        String[] columns = {"A", "B", "C", "D"};

        //Create a table with named columns
        JTable table = new JTable(spreadsheet.getEvaluations(), columns);
        table.setBounds(30,40,200,300);
        JScrollPane sp=new JScrollPane(table);
        frame.add(sp);
        frame.setSize(300,400);
//        frame.setVisible(true);
        //Add the ubiquitous "Hello World" label.
//        JLabel label = new JLabel("Hello World");
//        frame.getContentPane().add(label);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
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
