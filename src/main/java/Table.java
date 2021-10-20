import java.util.*;

@SuppressWarnings("unused")
public class Table {
    private int rows;
    private int columns;
    private Cell[][] spreadsheet;
    private String[][] evaluations;
    private Map<Cell, Set<Cell>> references;
    private Table prevTable;
    private Table savedTable;

    /**
     * The method creates a spreadsheet with users input of rows and columns.
     * @param rows - rows amount
     * @param columns - columns amount
     */
    public Table(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        spreadsheet = new Cell[rows][columns];
        evaluations = new String[rows][columns];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                spreadsheet[i][j] = new Cell(this, i, j);
        references = new HashMap<>();
        prevTable = null;
    }

    public Map<Cell, Set<Cell>> getReferences() {
        return references;
    }

    public void setReferences(Map<Cell, Set<Cell>> references) {
        this.references = references;
    }

    public String[][] getEvaluations() {
        return evaluations;
    }

    public void setEvaluations(String[][] evaluations) {
        this.evaluations = evaluations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                if (!this.getSpreadsheet()[i][j].getInfo().equals(table.getSpreadsheet()[i][j].getInfo()))
                    return false;
                if (!this.getSpreadsheet()[i][j].getEvaluation().equals(table.getSpreadsheet()[i][j].getEvaluation()))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rows, columns, references);
        result = 31 * result + Arrays.deepHashCode(spreadsheet);
        return result;
    }

    /**
     * The method copies the previous table and saves it to the saved variable;
     * sets the prevTable variable to the current spreadsheet.
     */
    public void save() {
        if (prevTable == null) prevTable = new Table(this.getRows(), this.getColumns());
        if (savedTable == null) savedTable = new Table(this.getRows(), this.getColumns());
        this.savedTable = copyTable(this.prevTable);
        this.prevTable = copyTable(this);
    }

    /**
     * The method sets information to the specific cell.
     * @param x - row index
     * @param y - column index
     * @param input - cells information
     */
    public void setCell(int x, int y, String input) {
        spreadsheet[x][y].setInfo(input.toUpperCase());
        evaluations[x][y] = spreadsheet[x][y].getEvaluation();
        if (!this.equals(prevTable)) {
            save();
        }
    }

    /**
     * The method copies entries from the table.
     * @param table - table to copy
     * @return table with the same entries.
     */
    public Table copyTable(Table table) {
        Table newTable = new Table(table.getRows(), table.getColumns());
        for (int i = 0; i < newTable.rows; i++) {
            for (int j = 0; j < newTable.columns; j++) {
                newTable.getSpreadsheet()[i][j].setInfo(table.getSpreadsheet()[i][j].getInfo());
                newTable.getSpreadsheet()[i][j].setEvaluation(table.getSpreadsheet()[i][j].getEvaluation());
                newTable.getEvaluations()[i][j] = newTable.getSpreadsheet()[i][j].getEvaluation();
            }
        }
        for (Cell key : table.getReferences().keySet()) {
            newTable.getReferences().put(key, table.getReferences().get(key));
        }
        newTable.prevTable = table.prevTable;
        newTable.savedTable = table.savedTable;
        return newTable;
    }

    public Table getPrevTable() {
        return prevTable;
    }

    public void setPrevTable(Table prevTable) {
        this.prevTable = prevTable;
    }

    public Table getSavedTable() {
        return savedTable;
    }

    public void setSavedTable(Table savedTable) {
        this.savedTable = savedTable;
    }

    public Cell getCell(int x, int y) {
        return spreadsheet[x][y];
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public Cell[][] getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(Cell[][] spreadsheet) {
        this.spreadsheet = spreadsheet;
    }
}
