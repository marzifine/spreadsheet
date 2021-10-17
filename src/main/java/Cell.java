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
    private boolean min;
    private boolean max;

    private final String VALUE_ERROR = "#Val!";
    private final String REFERENCE_ERROR = "#Ref!";

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

    private void function() {
//        String[] references = parseRef();
        List<String> evaluations = new LinkedList<>();
        List<Double> numericValues = new LinkedList<>();
        double result = 0.0;
        String[] references_split = temp.split(";");
        for (String s : references_split) {
            temp = s;
            String[] edges = parseRef();
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

//        if (references.length == 3) {
//            int x = getX(references[2]);
//            int y = getY(references[2]);
//            Cell referee = spreadsheet.getCell(x, y);
//            addReferences(referee);
//            evaluations.add(referee.getEvaluation());
//        }
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

    //handle math equation
    private void handleExpression() {
        if (temp.equals(REFERENCE_ERROR) || temp.equals(VALUE_ERROR))
            return;
        else {
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
        if (spreadsheet.getReferences().containsKey(this) && (referee.equals(this) || spreadsheet.getReferences().get(this).contains(referee)))
            compromiseCells(this);
        spreadsheet.getReferences().get(referee).add(this);
    }

    private void compromiseCells(Cell root) {
        if (!spreadsheet.getReferences().containsKey(root)) return;
        for (Cell reference : spreadsheet.getReferences().get(root)) {
            reference.setEvaluation(REFERENCE_ERROR);
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

    public void setInfo(String info) {
        this.info = info;
        handleInfo();
        updateCells(this);
        spreadsheet.getEvaluations()[x][y] = evaluation;
    }

    private void updateCells(Cell root) {
        if (!spreadsheet.getReferences().containsKey(root)) return;
        for (Cell reference : spreadsheet.getReferences().get(root)) {
            reference.handleInfo();
        }
    }
}
