package src.assembler;

import java.io.Reader;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.IOException;

public class Parser {
    Command currentCommand;
    Command nextCommand;
    Parser2 parser;

    Parser(Reader in) throws IOException {
        this.parser = new Parser2(in);
        this.currentCommand = null;
        this.nextCommand = null;
    }

    public boolean hasMoreCommands() throws IOException {
        if (this.nextCommand != null)
            return true;
        this.nextCommand = this.parser.advance();
        return this.nextCommand != null;
    }

    public void advance() throws IOException {
        assert this.hasMoreCommands();
        this.currentCommand = this.nextCommand;
        this.nextCommand = null;
    }

    public CommandType commandType() {
        return this.currentCommand.type;
    }

    public String symbol() {
        return this.currentCommand.symbol;
    }

    public String dest() {
        return this.currentCommand.dest;
    }

    public String comp() {
        return this.currentCommand.comp;
    }

    public String jump() {
        return this.currentCommand.jump;
    }

    private class Parser2 {
        Reader in;
        int lineNumber;
        int nextHead;
        int numRead;

        Parser2(Reader in) throws IOException {
            this.in = in;
            this.lineNumber = 1;
            this.numRead = 0;
            this.nextHead = read();
        }

        private int read() throws IOException {
            int res = in.read();
            if (res >= 0)
                this.numRead++;
            return res;
        }

        public Command advance() throws IOException {
            int lastNumRead = -1;
            while (this.nextHead >= 0) {
                if (lastNumRead == numRead)
                    throw new IOException(String.format("assembler error at line %d", lineNumber));
                lastNumRead = numRead;
                int c = this.nextHead;
                if (c == '@') {
                    return this.parseACommand();
                } else if (c == '(') {
                    return this.parseLCommand();
                } else if (c == '/') {
                    this.parseComment();
                } else if (this.canUseAsSymbolHead(c) || this.isUnaryOperator(c) || this.isNumber(c)) {
                    return this.parseCCommand();
                } else if (c == '\n') {
                    lineNumber++;
                    nextHead = read();
                } else if (c == '\r') {
                    nextHead = read();
                } else {
                    skipSpace(true);
                }
            }
            return null; // end
        }

        private Command parseACommand() throws IOException {
            assert nextHead == '@';
            nextHead = read();
            Command result = new Command();
            result.symbol = parseSymbol();
            result.type = CommandType.A_COMMAND;
            return result;
        }

        private Command parseCCommand() throws IOException {
            Command result = new Command();
            String a = parseExpr();
            skipSpace(true);
            if (nextHead == '=') {
                result.dest = a;
                nextHead = read();
                skipSpace(true);
                result.comp = parseExpr();
                skipSpace(true);
            } else {
                result.comp = a;
            }
            if (nextHead == ';') {
                nextHead = read();
                skipSpace(true);
                result.jump = parseSymbol();

            }
            skipSpace(true);
            //
            result.type = CommandType.C_COMMAND;
            return result;
        }

        private Command parseLCommand() throws IOException {
            assert nextHead == '(';
            nextHead = read();
            Command result = new Command();
            result.symbol = parseSymbol();
            assert nextHead == ')';
            nextHead = read();
            result.type = CommandType.L_COMMAND;
            return result;
        }

        private void parseComment() throws IOException {
            assert nextHead == '/';
            assert read() == '/';
            do {
                nextHead = read();
            } while (nextHead != '\n' && nextHead >= 0);
        }

        private String parseValue() throws IOException {
            StringBuffer buf = new StringBuffer();
            assert isNumber(nextHead);
            while (isNumber(nextHead)) {
                buf.append((char) nextHead);
                nextHead = read();
            }
            return buf.toString();
        }

        private String parseVar() throws IOException {
            StringBuffer buf = new StringBuffer();
            assert canUseAsSymbolHead(nextHead);
            do {
                buf.append((char) nextHead);
                nextHead = read();
            } while (canUseAsSymbol(nextHead));
            return buf.toString();
        }

        // Symbol := Value | Var
        private String parseSymbol() throws IOException {
            if (isNumber(nextHead))
                return parseValue();
            return parseVar();
        }

        private String parseExpr() throws IOException {
            StringBuffer buf = new StringBuffer();
            assert (isUnaryOperator(nextHead) || canUseAsSymbolHead(nextHead) || isNumber(nextHead));
            do {
                if (!isSpace(nextHead))
                    buf.append((char) nextHead);
                nextHead = read();
            } while (isBinaryOperator(nextHead) || isUnaryOperator(nextHead) || canUseAsSymbol(nextHead)
                    || isNumber(nextHead) || isSpace(nextHead));
            return buf.toString();
        }

        private void skipSpace(boolean allowEmpty) throws IOException {
            if (isSpace(nextHead)) {
                do {
                    nextHead = read();
                } while (isSpace(nextHead));
            } else
                assert allowEmpty;
        }

        private boolean canUseAsSymbolHead(int c) {
            return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_' || c == '.' || c == '$' || c == ':';
        }

        private boolean canUseAsSymbol(int c) {
            return this.canUseAsSymbolHead(c) || this.isNumber(c);
        }

        private boolean isNumber(int c) {
            return '0' <= c && c <= '9';
        }

        private boolean isBinaryOperator(int c) {
            return c == '+' || c == '-' || c == '&' || c == '|';
        }

        private boolean isUnaryOperator(int c) {
            return c == '-' || c == '!';
        }

        private boolean isSpace(int c) {
            return c == ' ' || c == '\t';
        }

        public boolean isValue(String symbol) {
            boolean res = true;
            for (char c : symbol.toCharArray())
                res &= isNumber(c);
            return res;
        }
    }

    public boolean isValue(String symbol) {
        return parser.isValue(symbol);
    }

}
