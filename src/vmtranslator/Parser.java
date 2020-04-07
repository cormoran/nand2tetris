package src.vmtranslator;

import java.io.IOException;
import java.io.Reader;

import jdk.nashorn.internal.runtime.ParserException;

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
        System.out.println(this.currentCommand);
    }

    public CommandType commandType() {
        return this.currentCommand.type;
    }

    public String arg1() {
        return this.currentCommand.arg1;
    }

    public int arg2() {
        return this.currentCommand.arg2;
    }

    private class Parser2 {
        Reader in;
        int lineNumber;
        int nextHead;
        int numRead;

        Parser2(Reader in) throws IOException {
            this.in = in;
            lineNumber = 1;
            numRead = 0;
            nextHead = read();
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
                    throw new IOException(String.format("vm parser error at line %d", lineNumber));
                lastNumRead = numRead;
                int c = this.nextHead;
                if (c == '/') {
                    this.parseComment();
                } else if (this.canUseAsSymbolHead(c)) {
                    return this.parseCommand();
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

        private Command parseCommand() throws IOException {
            Command res = new Command();
            String command = parseVar();
            switch (command) {
                case "push":
                    res.type = CommandType.C_PUSH;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    skipSpace(false);
                    res.arg2 = Integer.parseInt(parseValue());
                    break;
                case "pop":
                    res.type = CommandType.C_POP;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    skipSpace(false);
                    res.arg2 = Integer.parseInt(parseValue());
                    break;
                case "goto":
                    res.type = CommandType.C_GOTO;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    break;
                case "if-goto":
                    res.type = CommandType.C_IF;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    break;
                case "return":
                    res.type = CommandType.C_RETURN;
                    break;
                case "call":
                    res.type = CommandType.C_CALL;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    skipSpace(false);
                    res.arg2 = Integer.parseInt(parseValue());
                    break;
                case "function":
                    res.type = CommandType.C_FUNCTION;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    skipSpace(false);
                    res.arg2 = Integer.parseInt(parseValue());
                    break;
                case "label":
                    res.type = CommandType.C_LABEL;
                    skipSpace(false);
                    res.arg1 = parseSymbol();
                    break;
                case "add":
                case "sub":
                case "neg":
                case "eq":
                case "gt":
                case "lt":
                case "and":
                case "or":
                case "not":
                    res.type = CommandType.C_ARITHMETIC;
                    res.arg1 = command;
                    break;
                default:
                    throw new ParserException("unknown command: " + command);
            }
            skipSpace(true);
            assert nextHead == '/' || nextHead == '\r' || nextHead == '\n';
            return res;
        }

        private void parseComment() throws IOException {
            assert nextHead == '/';
            assert read() == '/';
            do {
                nextHead = read();
            } while (nextHead != '\n' && nextHead >= 0);
        }

        private String parseSymbol() throws IOException {
            if (isNumber(nextHead))
                return parseValue();
            return parseVar();
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

        private void skipSpace(boolean allowEmpty) throws IOException {
            if (isSpace(nextHead)) {
                do {
                    nextHead = read();
                } while (isSpace(nextHead));
            } else
                assert allowEmpty;
        }

        private boolean isSpace(int c) {
            return c == ' ' || c == '\t';
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
    }
}