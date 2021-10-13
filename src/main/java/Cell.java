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

    public static void main(String[] args) {
//        String pattern = "(([A-Z]+)(\\d+))";
//        System.out.print(Arrays.toString("A12+b30".split(pattern)));
//        System.out.println("AA".matches(pattern));
//        System.out.println("A2".matches(pattern));
//        System.out.println("AB33".matches(pattern));
//        System.out.println("11".matches(pattern));
//        List row = Pattern.compile("([A-Z]+)")
//                .matcher("AA345")
//                .results()
//                .map(MatchResult::group)
//                .collect(Collectors.toList());
//        System.out.println(row.get(0));

//        Table table = new Table();
//        table.setCell(0, 0, "1"); //A1
//        table.setCell(1, 0, "=2"); //B1
//        table.setCell(0, 1, "=A1+B1"); //A2
//        table.setCell(1, 1, "=SUM(A1:A2;B1)"); //B2
//        System.out.println("A1: " + table.getCell(0, 0).getEvaluation());
//        System.out.println("B1: " +  table.getCell(1, 0).getEvaluation());
//        System.out.println("A2: " +  table.getCell(0, 1).getEvaluation());
//        System.out.println("B2: " +  table.getCell(1, 1).getEvaluation());
//        table.setCell(1, 0, "=B1"); //B1
//        System.out.println("A1: " + table.getCell(0, 0).getEvaluation());
//        System.out.println("B1: " +  table.getCell(1, 0).getEvaluation());
//        System.out.println("A2: " +  table.getCell(0, 1).getEvaluation());
//        System.out.println("B2: " +  table.getCell(1, 1).getEvaluation());

//        System.out.println(table.getReferences().entrySet());
//        for (Map.Entry<Cell, Set<Cell>> entry : table.getReferences().entrySet()) {
//            System.out.println(entry.getKey().x + " " + entry.getKey().y);
//            for (Cell cell : entry.getValue()) {
//                System.out.println(cell.getInfo());
//            }
//        }
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

    private void handleInfo() {
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
        if (Math.round(result) == result) {
            temp = String.valueOf((int)result);
        } else temp = String.valueOf(result);
    }

    //handle math equation
    private void handleExpression() {
            if (temp.equals("#Ref!"))
                return;
            else
                temp = String.valueOf((int)Calculator.eval(temp));
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
            if (!(sum || avg))
                addReferences(spreadsheet.getCell(x, y));
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

    private int getY(String location) {
        String row = Pattern.compile("(\\d+)")
                .matcher(location)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList())
                .get(0);
        return Integer.parseInt(row) - 1;
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
