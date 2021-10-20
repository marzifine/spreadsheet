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
                    // addition
                    if (handle('+')) x += parseTerm();
                    // subtraction
                    else if (handle('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    // multiplication
                    if (handle('*')) x *= parseFactor();
                    // division
                    else if (handle('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (handle('+')) return parseFactor();
                if (handle('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                // parentheses
                if (handle('(')) {
                    x = parseExpression();
                    handle(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') toNextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                    // functions
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') toNextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                // exponentiation
                if (handle('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}
