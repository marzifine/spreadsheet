package spreadsheet;

import spreadsheet.ui.Application;

public class Main {
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(Application::createAndShowGUI);
    }
}
