import java.util.*;

public class Table {
    private int rows;
    private int columns;
    private Cell[][] spreadsheet;
    private String[][] evaluations;
    private Map<Cell, Set<Cell>> references;

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

    public Table() {
        rows = 5;
        columns = 5;
        spreadsheet = new Cell[rows][columns];
        evaluations = new String[rows][columns];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                spreadsheet[i][j] = new Cell(this, i, j);
        references = new HashMap<>();
    }

    public Table(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        spreadsheet = new Cell[rows][columns];
        evaluations = new String[rows][columns];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                spreadsheet[i][j] = new Cell(this, i, j);
        references = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return rows == table.rows && columns == table.columns && Arrays.equals(spreadsheet, table.spreadsheet) && Objects.equals(references, table.references);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rows, columns, references);
        result = 31 * result + Arrays.hashCode(spreadsheet);
        return result;
    }

    public void setCell(int x, int y, String input) {
        spreadsheet[x][y].setInfo(input.toUpperCase());
        evaluations[x][y] = spreadsheet[x][y].getEvaluation();
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
