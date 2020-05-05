package src.compiler;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public class Tokenizer {
    public static void main(String[] args) throws IOException {
        assert args.length == 1;
        String outFileName = args[0].replace(".jack", "T.gen.xml");

        Tokenizer tokenizer = new Tokenizer(new FileReader(args[0]));

        FileWriter writer = new FileWriter(outFileName);

        writer.write("<tokens>\n");
        while (true) {

            TokenType type = tokenizer.advance();
            if (type == null)
                break;
            writer.write("\t");
            writer.write(tokenizer.getCurrentAsXML());
            writer.write("\n");
        }
        writer.write("</tokens>\n");
        writer.close();
    }

    private Reader in;
    private int lineNumber;
    private int nextHead;
    private int numRead;
    private TokenType currentType;
    private KeyWord currentKeyword;
    private String currentSymbol;
    private int currentInt;
    private String currentString;
    private String currentIdentifier;

    Tokenizer(Reader in) throws IOException {
        this.in = in;
        lineNumber = 1;
        numRead = 0;
        nextHead = read();
    }

    public TokenType advance() throws IOException {
        int lastNumRead = -1;
        currentType = null;
        while (this.nextHead >= 0 && currentType == null) {
            if (lastNumRead == numRead)
                throw new IOException(String.format("token parser error at line %d", lineNumber));
            lastNumRead = numRead;
            int c = this.nextHead;
            if (isNumber(c)) {
                currentInt = parseInt();
                currentType = TokenType.INT_CONST;
            } else if (c == '/') {
                nextHead = read();
                if (nextHead == '/')
                    skipLineComment();
                else if (nextHead == '*') {
                    skipBlockComment();
                } else {
                    currentSymbol = String.valueOf((char) c);
                    currentType = TokenType.SYMBOL;
                }
            } else if (isSymbol(c)) {
                currentSymbol = String.valueOf((char) c);
                currentType = TokenType.SYMBOL;
                nextHead = read();
            } else if (c == '"') {
                currentString = parseString();
                currentType = TokenType.STRING_CONST;
            } else if (isSpace(c)) {
                skipSpace(false);
            } else if (c == '\n') {
                lineNumber++;
                nextHead = read();
            } else if (c == '\r') {
                nextHead = read();
            } else {
                currentIdentifier = parseIdentifier();
                boolean isKeyword = false;
                for (KeyWord key : KeyWord.values()) {
                    if (currentIdentifier.toUpperCase().equals(key.toString().toUpperCase())) {
                        isKeyword = true;
                        currentKeyword = key;
                        currentType = TokenType.KEYWORD;
                        break;
                    }
                }
                if (!isKeyword) {
                    currentType = TokenType.IDENTIFIER;
                }
            }

        }
        return currentType;
    }

    public TokenType tokenType() {
        return currentType;
    }

    public KeyWord keyWord() {
        return currentKeyword;
    }

    public String symbol() {
        return currentSymbol;
    }

    public String identifier() {
        return currentIdentifier;
    }

    public int intVal() {
        return currentInt;
    }

    public String stringVal() {
        return currentString;
    }

    public String getCurrentAsXML() {
        TokenType type = currentType;
        if (type == null)
            assert false;
        StringBuffer buf = new StringBuffer();
        switch (type) {
            case KEYWORD:
                buf.append("<keyword> ");
                buf.append(keyWord().toString().toLowerCase());
                buf.append(" </keyword>");
                break;
            case SYMBOL:
                buf.append("<symbol> ");
                buf.append(symbol().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
                buf.append(" </symbol>");
                break;
            case INT_CONST:
                buf.append("<integerConstant> ");
                buf.append(Integer.valueOf(intVal()).toString());
                buf.append(" </integerConstant>");
                break;
            case STRING_CONST:
                buf.append("<stringConstant> ");
                buf.append(stringVal());
                buf.append(" </stringConstant>");
                break;
            case IDENTIFIER:
                buf.append("<identifier> ");
                buf.append(identifier());
                buf.append(" </identifier>");
                break;
            default:
                assert false;
        }
        return buf.toString();
    }

    private int read() throws IOException {
        int res = in.read();
        if (res >= 0)
            this.numRead++;
        return res;
    }

    // nextHead is second '/' of "//"
    private void skipLineComment() throws IOException {
        assert nextHead == '/';
        do {
            nextHead = read();
        } while (nextHead != '\n' && nextHead >= 0);
    }

    // nextHead is * of "/*"
    private void skipBlockComment() throws IOException {
        assert nextHead == '*';
        do {
            nextHead = read();
            if (nextHead == '*') {
                nextHead = read();
                if (nextHead == '/') {
                    nextHead = read();
                    break;
                }
            }
        } while (nextHead >= 0);
    }

    private void skipSpace(boolean allowEmpty) throws IOException {
        if (isSpace(nextHead)) {
            do {
                nextHead = read();
            } while (isSpace(nextHead));
        } else
            assert allowEmpty;
    }

    private int parseInt() throws IOException {
        StringBuffer buf = new StringBuffer();
        assert isNumber(nextHead);
        while (isNumber(nextHead)) {
            buf.append((char) nextHead);
            nextHead = read();
        }
        return Integer.parseInt(buf.toString());
    }

    private String parseIdentifier() throws IOException {
        StringBuffer buf = new StringBuffer();
        assert canUseAsSymbolHead(nextHead);
        do {
            buf.append((char) nextHead);
            nextHead = read();
        } while (canUseAsSymbol(nextHead));
        return buf.toString();
    }

    private String parseString() throws IOException {
        StringBuffer buf = new StringBuffer();
        assert nextHead == '"';
        nextHead = read();
        while (nextHead != '"') {
            assert canUseAsStringConstant(nextHead);
            buf.append((char) nextHead);
            nextHead = read();
        }
        nextHead = read();
        return buf.toString();
    }

    private boolean canUseAsSymbolHead(int c) {
        return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
    }

    private boolean canUseAsStringConstant(int c) {
        return c != '"' && c != '\r' && c != '\n';
    }

    private boolean canUseAsSymbol(int c) {
        return this.canUseAsSymbolHead(c) || this.isNumber(c);
    }

    private boolean isNumber(int c) {
        return '0' <= c && c <= '9';
    }

    private boolean isSymbol(int c) {
        return "{}()[].,;+-*/&|<>=~".indexOf(c) >= 0;
    }

    private boolean isSpace(int c) {
        return c == ' ' || c == '\t';
    }
}