package spreadsheet.table;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "OptionalGetWithoutIsPresent"})
public class Cell {
    private final String VALUE_ERROR = "#Val!";
    private final String REFERENCE_ERROR = "#Ref!";
    private String info;
    private String evaluation;
    private String temp;
    private Table spreadsheet;
    private int x;
    private int y;
    private boolean sum;
    private boolean avg;
    private boolean min;
    private boolean max;

    public Cell(Table spreadsheet, int x, int y) {
        info = null;
        evaluation = null;
        temp = null;
        this.spreadsheet = spreadsheet;
        this.x = x;
        this.y = y;
        sum = false;
        avg = false;
        min = false;
        max = false;
    }

    /**
     *
     * @param location - location as string i.e. A1
     * @return int X position
     */
    public static int getX(String location) {
        String row = Pattern.compile("(\\d+)")
                .matcher(location)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList())
                .get(0);
        return Integer.parseInt(row) - 1;
    }

    /**
     *
     * @param location - location as string i.e. A1
     * @return int Y location
     */
    public static int getY(String location) {
        String column = Pattern.compile("([A-Z]+)")
                .matcher(location)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList())
                .get(0);
        int columnIndex = -26;
        for (int j = 0; j < column.length(); j++) {
            columnIndex += column.charAt(j);
            columnIndex -= 'A';
            columnIndex += 26;
        }
        return columnIndex;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return x == cell.x && y == cell.y && this.info.equals(cell.info) && this.evaluation.equals(cell.evaluation) && spreadsheet.equals(cell.spreadsheet);
    }

    public Table getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(Table spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

    /**
     * The method takes info of the cell
     * and handles it according to what the information contains:
     * either a function as SUM/AVERAGE/MIN/MAX
     * or a reference to another cell
     * or an expression containing a reference to another cell
     * or a math expression.
     *
     * The method sets an evaluation of the cell.
     */
    public void handleInfo() {
        temp = info;
        //handle math expression
        if (info.startsWith("=") || info.startsWith(" =")) {
            temp = temp.substring(temp.indexOf('=') + 1);
            //handle reference
            if (temp.startsWith("SUM")) {
                sum = true;
                function();
            } else if (temp.startsWith("AVERAGE")) {
                avg = true;
                function();
            } else if (temp.startsWith("MIN")) {
                min = true;
                function();
            } else if (temp.startsWith("MAX")) {
                max = true;
                function();
            } else if (temp.matches("(([A-Z]+)(\\d+))")) {
                handleRef();
            } else if (temp.matches("(.*)(([A-Z]+)(\\d+))(.*)")) {
                handleRef();
                handleExpression();
            } else handleExpression();
        }
        evaluation = temp;
    }

    /**
     * The method parses a function
     * and handles an input accordingly.
     */
    private void function() {
        List<String> evaluations = new LinkedList<>();
        List<Double> numericValues = new LinkedList<>();
        double result = 0.0;
        String[] references_split = temp.split(";");
        for (String s : references_split) {
            temp = s;
            String[] edges = parseRef();
            if (temp.equals(REFERENCE_ERROR)) return;
            if (s.contains(":")) {
                int x1 = getX(edges[0]);
                int y1 = getY(edges[0]);
                int x2 = getX(edges[1]);
                int y2 = getY(edges[1]);

                int startX = Math.min(x1, x2);
                int endX = Math.max(x1, x2);
                int startY = Math.min(y1, y2);
                int endY = Math.max(y1, y2);
                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        Cell referee = spreadsheet.getCell(x, y);
                        addReferences(referee);
                        evaluations.add(referee.getEvaluation());
                    }
                }
            } else {
                int x = getX(edges[0]);
                int y = getY(edges[0]);
                Cell referee = spreadsheet.getCell(x, y);
                addReferences(referee);
                evaluations.add(referee.getEvaluation());
            }
        }

        for (String evaluation : evaluations) {
            if (evaluation.equals(REFERENCE_ERROR)) {
                compromiseCells(this);
                temp = REFERENCE_ERROR;
                return;
            }
            if (evaluation.matches("((\\d+)(.*))")) {
                numericValues.add(Double.parseDouble(evaluation));
            }
        }
        if (sum) {
            result = numericValues.stream().mapToDouble(Double::doubleValue).sum();
        } else if (avg) {
            result = numericValues.stream().mapToDouble(Double::doubleValue).sum();
            result = result / evaluations.size();
        } else if (min) {
            result = numericValues.stream().mapToDouble(Double::doubleValue).min().getAsDouble();
        } else if (max) {
            result = numericValues.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
        }
        if ((result % 1) == 0)
            temp = String.valueOf((int) result);
        else temp = String.valueOf(result);
    }

    /**
     * The method handles a math expression
     * using the spreadsheet.table.Calculator class.
     */
    private void handleExpression() {
        if (!(temp.equals(REFERENCE_ERROR) || temp.equals(VALUE_ERROR))) {
            try {
                double result = Calculator.eval(temp);
                if ((result % 1) == 0)
                    temp = String.valueOf((int) result);
                else temp = String.valueOf(result);
            } catch (RuntimeException e) {
                temp = VALUE_ERROR;
            }
        }
    }

    /**
     * The method parses all the mentioned references in the temp variable.
     * @return String[] matches of all the references.
     */
    private String[] parseRef() {
        String pattern = "(([A-Z]+)(\\d+))";
        String[] matches = Pattern.compile(pattern)
                .matcher(temp)
                .results()
                .map(MatchResult::group)
                .toArray(String[]::new);
        for (String match : matches) {
            int x = getX(match);
            int y = getY(match);
            if (x >= spreadsheet.getRows() || y >= spreadsheet.getColumns())
                temp = REFERENCE_ERROR;
            else if (!(sum || avg || min || max))
                addReferences(spreadsheet.getCell(x, y));
        }
        return matches;
    }

    /**
     * The method adds references mentioned in the formula
     * to the referee's set of references.
     * @param referee - the cell to which add reference to the current cell
     */
    private void addReferences(Cell referee) {
        if (!spreadsheet.getReferences().containsKey(referee))
            spreadsheet.getReferences().put(referee, new HashSet<>());
        if (spreadsheet.getReferences().containsKey(this) && (referee.equals(this) || spreadsheet.getReferences().get(this).contains(referee)))
            compromiseCells(this);
        spreadsheet.getReferences().get(referee).add(this);
    }

    /**
     * The method compromises cell's references
     * when an Error Message occurs.
     * @param root - the cell from which start to compromise cells
     */
    private void compromiseCells(Cell root) {
        if (!spreadsheet.getReferences().containsKey(root)) return;
        for (Cell reference : spreadsheet.getReferences().get(root)) {
            reference.setEvaluation(REFERENCE_ERROR);
        }
    }

    /**
     * The method replaces every reference with its evaluation
     * in temp variable.
     */
    private void handleRef() {
        String[] matches = parseRef();
        if (temp.equals(REFERENCE_ERROR)) return;
        String[] converted = new String[matches.length];
        for (int i = 0; i < matches.length; i++) {
            int columnIndex = getX(matches[i]);
            int rowIndex = getY(matches[i]);

            converted[i] = getSpreadsheet().getCell(columnIndex, rowIndex).getEvaluation();
        }

        for (int i = 0; i < matches.length; i++) {
            if (converted[i].matches("([A-Z]+)")) {
                temp = VALUE_ERROR;
                break;
            }
            if (converted[i].equals(REFERENCE_ERROR)) {
                temp = REFERENCE_ERROR;
                break;
            }
            if (converted[i].equals(""))
                converted[i] = "0";
            temp = temp.replace(matches[i], converted[i]);
        }
    }

    public String getEvaluation() {
        return evaluation == null ? "" : evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public String getInfo() {
        return info == null ? "" : info;
    }

    /**
     * The method handles an input from user,
     * updates connected cells
     * and sets its evaluation in the spreadsheet.table.Table's evaluation array.
     * @param info - a user's input.
     */
    public void setInfo(String info) {
        this.info = info;
        handleInfo();
        updateCells(this);
        spreadsheet.getEvaluations()[x][y] = evaluation;
    }

    /**
     * The method updates connected cells when a cell's information
     * and therefore its evaluation changes.
     * @param root - from which cell start update cells.
     */
    private void updateCells(Cell root) {
        if (!spreadsheet.getReferences().containsKey(root)) return;
        for (Cell reference : spreadsheet.getReferences().get(root)) {
            reference.handleInfo();
        }
    }
}
