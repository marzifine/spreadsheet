public class Calculator {
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void toNextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean handle(int charToHandle) {
                while (ch == ' ') toNextChar();
                if (ch == charToHandle) {
                    toNextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                toNextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                while (true) {
                    if (handle('+')) x += parseTerm(); // addition
                    else if (handle('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    if (handle('*')) x *= parseFactor(); // multiplication
                    else if (handle('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (handle('+')) return parseFactor();
                if (handle('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (handle('(')) { // parentheses
                    x = parseExpression();
                    handle(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') toNextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') toNextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                if (handle('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }
}
