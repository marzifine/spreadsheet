import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Cell {
    private String info;
    private String evaluation;
    private String temp;
    private Table spreadsheet;
    private int x;
    private int y;
    private boolean sum;
    private boolean avg;

    public Cell(Table spreadsheet, int x, int y) {
        info = null;
        evaluation = null;
        temp = null;
        this.spreadsheet = spreadsheet;
        this.x = x;
        this.y = y;
        sum = false;
        avg = false;
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
        return x == cell.x && y == cell.y && Objects.equals(info, cell.info) && spreadsheet.equals(cell.spreadsheet);
    }

    public Table getSpreadsheet() {
        return spreadsheet;
    }

    public void setSpreadsheet(Table spreadsheet) {
        this.spreadsheet = spreadsheet;
    }

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
            } else if (temp.matches("(([A-Z]+)(\\d+))")) {
                handleRef();
            } else if (temp.matches("(.*)(([A-Z]+)(\\d+))(.*)")) {
                handleRef();
                handleExpression();
            } else handleExpression();
        }
        evaluation = temp;
    }

    private void function() {
        String[] edges = parseRef();
        List<String> evaluations = new LinkedList<>();
        List<Cell> cells = new LinkedList<>();
        double result = 0.0;
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
                cells.add(referee);
                evaluations.add(referee.getEvaluation());
            }
        }
        if (edges.length == 3) {
            int x = getX(edges[2]);
            int y = getY(edges[2]);
            Cell referee = spreadsheet.getCell(x, y);
            addReferences(referee);
            cells.add(referee);
            evaluations.add(referee.getEvaluation());
        }
        for (String evaluation : evaluations) {
            if (evaluation.equals("#Ref!")) {
                compromiseCells(this);
                temp = "#Ref!";
                break;
            }
            if (evaluation.matches("((\\d+)(.*))")) {
                result += Double.parseDouble(evaluation);
            }
        }
        if (avg) {
            result = result / evaluations.size();
        }
        if ((result % 1) == 0)
            temp = String.valueOf((int) result);
        else temp = String.valueOf(result);
    }

    //handle math equation
    private void handleExpression() {
        if (temp.equals("#Ref!") || temp.equals("#Val!"))
            return;
        else {
            double result = Calculator.eval(temp);
            if ((result % 1) == 0)
                temp = String.valueOf((int) result);
            else temp = String.valueOf(result);
        }
    }

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
            if (sum || avg) {}
            else addReferences(spreadsheet.getCell(x, y));
        }
        return matches;
    }

    private void addReferences(Cell referee) {
        if (!spreadsheet.getReferences().containsKey(referee))
            spreadsheet.getReferences().put(referee, new HashSet<>());
        if (referee.equals(this) || spreadsheet.getReferences().get(referee).contains(this)) {
            compromiseCells(this);
        }
        spreadsheet.getReferences().get(referee).add(this);
    }

    private void compromiseCells(Cell root) {
        if (!spreadsheet.getReferences().containsKey(root)) return;
        for (Cell reference : spreadsheet.getReferences().get(root)) {
            reference.setEvaluation("#Ref!");
        }
    }

    private int getX(String location) {
        String row = Pattern.compile("(\\d+)")
                .matcher(location)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList())
                .get(0);
        return Integer.parseInt(row) - 1;
    }

    private int getY(String location) {
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

    //consists of letters at the beginning and digits at the end
    private void handleRef() {
        String[] matches = parseRef();
        String[] converted = new String[matches.length];
        for (int i = 0; i < matches.length; i++) {
            int columnIndex = getX(matches[i]);
            int rowIndex = getY(matches[i]);

            converted[i] = getSpreadsheet().getCell(columnIndex, rowIndex).getEvaluation();
        }

        //replace every reference with its numeric value
        for (int i = 0; i < matches.length; i++) {
            if (converted[i].matches("([A-Z]+)")) {
                temp = "#Val!";
                break;
            }
            if (converted[i].equals("#Ref!")) {
                temp = "#Ref!";
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

    public void setInfo(String info) {
        this.info = info;
        handleInfo();
        updateCells(this);
    }

    private void updateCells(Cell root) {
        if (!spreadsheet.getReferences().containsKey(root)) return;
        for (Cell reference : spreadsheet.getReferences().get(root)) {
            reference.handleInfo();
        }
    }
}
