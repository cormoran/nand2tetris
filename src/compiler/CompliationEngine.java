package src.compiler;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CompliationEngine {

    public static void main(String[] args) throws Exception {
        assert args.length == 1;
        String outFileName = args[0].replace(".jack", ".gen.xml");

        CompliationEngine engine = new CompliationEngine(new FileReader(args[0]), new FileWriter(outFileName));
        engine.compileClass();
        engine.write();
    }

    Tokenizer tokenizer;
    Writer out;
    ArrayList<String> buffer;
    SymbolTable symbolTable;
    VMWriter vmwriter;

    CompliationEngine(Reader in, Writer out) throws Exception {
        tokenizer = new Tokenizer(in);
        tokenizer.advance();
        this.out = out;
        this.buffer = new ArrayList<String>();
        this.symbolTable = new SymbolTable();
        this.vmwriter = new VMWriter();
    }

    private String getXml() {
        StringBuilder builder = new StringBuilder();
        int depth = 0;
        for (String s : buffer) {
            if (s.startsWith("</"))
                depth--;
            for (int i = 0; i < depth; i++)
                builder.append("\t");
            if (s.startsWith("!")) {
                builder.append(s.substring(1));
            } else {
                builder.append(s);
            }
            builder.append("\n");
            if (!s.startsWith("</") && s.startsWith("<"))
                depth++;
        }
        return builder.toString();
    }

    private void write() throws Exception {
        String xml = getXml();
        out.write(xml);
        out.close();
    }

    private boolean pushStack(String key) {
        buffer.add(key);
        return true;
    }

    private boolean popStack() {
        buffer.remove(buffer.size() - 1);
        return false;
    }

    private <T> T ensure(T val) {
        assert val != null;
        return val;
    }

    // ------------------------------------------------------------------------------------

    public boolean compileClass() throws Exception {
        pushStack("<class>");
        if (compileKeyWord(KeyWord.CLASS) == null)
            return popStack();
        String className = ensure(compileIdentifier());
        assert compileSymbol("{") != null;

        while (compileClassVarDec())
            ;
        while (compileSubroutineDec(className))
            ;
        assert compileSymbol("}") != null;

        return pushStack("</class>");
    }

    // OK
    public boolean compileClassVarDec() throws Exception {
        pushStack("<classVarDec>");
        KeyWord kind = compileKeyWord(KeyWord.STATIC, KeyWord.FIELD);
        if (kind == null)
            return popStack();
        String type = ensure(compileType());
        String name = ensure(compileVarName());
        symbolTable.define(name, type, kind == KeyWord.STATIC ? SymbolKind.STATIC : SymbolKind.FIELD);
        while (compileSymbol(",") != null) {
            name = ensure(compileVarName());
            symbolTable.define(name, type, kind == KeyWord.STATIC ? SymbolKind.STATIC : SymbolKind.FIELD);
        }
        assert compileSymbol(";") != null;
        return pushStack("</classVarDec>");
    }

    // OK
    public boolean compileSubroutineDec(String className) throws Exception {
        pushStack("<subroutineDec>");
        KeyWord keyword = compileKeyWord(KeyWord.CONSTRUCTOR, KeyWord.FUNCTION, KeyWord.METHOD);
        if (keyword == null)
            return popStack();
        symbolTable.startSubroutine();
        boolean isVoidFunction = compileKeyWord(KeyWord.VOID) != null;
        if (!isVoidFunction)
            assert compileType() != null;
        String subroutineName = ensure(compileSubroutineName());
        assert compileSymbol("(") != null;
        assert compileParameterList();
        assert compileSymbol(")") != null;
        int varCount = symbolTable.varCount(SymbolKind.VAR);
        switch (keyword) {
            case CONSTRUCTOR:
                vmwriter.writeFunction(className + "." + subroutineName, varCount);
                // allocate new object
                vmwriter.writePush(SegmentType.CONST, symbolTable.varCount(SymbolKind.FIELD));
                vmwriter.writeCall("Memory.alloc", 1);
                vmwriter.writePop(SegmentType.POINTER, 0); // set this
                break;
            case METHOD:
                vmwriter.writeFunction(className + "." + subroutineName, varCount + 1); // self
                vmwriter.writePush(SegmentType.ARG, 0); // this
                vmwriter.writePop(SegmentType.POINTER, 0); // set this
                break;
            case FUNCTION:
                vmwriter.writeFunction(className + "." + subroutineName, varCount);
                break;
            default:
                assert false;
        }

        vmwriter.writeFunction(className + "." + subroutineName, varCount);
        vmwriter.writePop(SegmentType.POINTER, 0); // POP THIS
        assert compileSubroutineBody();
        if (isVoidFunction) {
            vmwriter.writePush(SegmentType.CONST, 0); // return 0 if void
        }
        return pushStack("</subroutineDec>");
    }

    // OK
    public boolean compileParameterList() throws Exception {
        pushStack("<parameterList>");
        String type = compileType();
        if (type != null) {
            String name = ensure(compileVarName());
            symbolTable.define(name, type, SymbolKind.ARG);
            while (compileSymbol(",") != null) {
                type = ensure(compileType());
                name = ensure(compileVarName());
                symbolTable.define(name, type, SymbolKind.ARG);
            }
        }
        return pushStack("</parameterList>");
    }

    // OK
    public boolean compileVarDec() throws Exception {
        pushStack("<varDec>");
        if (compileKeyWord(KeyWord.VAR) == null)
            return popStack();

        String type = ensure(compileType());
        String name = ensure(compileVarName());
        symbolTable.define(name, type, SymbolKind.VAR);
        while (compileSymbol(",") != null) {
            name = ensure(compileVarName());
            symbolTable.define(name, type, SymbolKind.VAR);
        }
        assert compileSymbol(";") != null;
        return pushStack("</varDec>");
    }

    // OK
    public boolean compileStatements() throws Exception {
        pushStack("<statements>");
        while (compileStatement())
            ;
        return pushStack("</statements>");
    }

    // OK
    public boolean compileDo() throws Exception {
        pushStack("<doStatement>");
        if (compileKeyWord(KeyWord.DO) == null)
            return popStack();
        assert compileSubroutineCall();
        assert compileSymbol(";") != null;
        vmwriter.writePop(SegmentType.TEMP, 0); // pop returned value
        return pushStack("</doStatement>");
    }

    // OK
    public boolean compileLet() throws Exception {
        pushStack("<letStatement>");
        if (compileKeyWord(KeyWord.LET) == null)
            return popStack();
        String varName = ensure(compileVarName());
        boolean isArray = false;
        if (compileSymbol("[") != null) {
            assert compileExpression();
            assert compileSymbol("]") != null;
            isArray = true;
        }
        assert compileSymbol("=") != null;
        assert compileExpression();
        assert compileSymbol(";") != null;

        if (isArray) {
            switch (symbolTable.kindOf(varName)) {
                case STATIC:
                    vmwriter.writePush(SegmentType.STATIC, symbolTable.indexOf(varName));
                    break;
                case FIELD:
                    vmwriter.writePush(SegmentType.THIS, symbolTable.indexOf(varName));
                    break;
                case ARG:
                    assert false;
                    break;
                case VAR:
                    vmwriter.writePush(SegmentType.LOCAL, symbolTable.indexOf(varName));
                    break;
                default:
                    assert false;
            }
            vmwriter.writeArithmetic(CommandType.ADD); // base + index
            vmwriter.writePop(SegmentType.POINTER, 1); // set that
            vmwriter.writePop(SegmentType.THAT, 0);
        } else {
            switch (symbolTable.kindOf(varName)) {
                case STATIC:
                    vmwriter.writePop(SegmentType.STATIC, symbolTable.indexOf(varName));
                    break;
                case FIELD:
                    vmwriter.writePop(SegmentType.THIS, symbolTable.indexOf(varName));
                    break;
                case ARG:
                    assert false;
                    break;
                case VAR:
                    vmwriter.writePop(SegmentType.LOCAL, symbolTable.indexOf(varName));
                    break;
                default:
                    assert false;
            }
        }
        return pushStack("</letStatement>");
    }

    // OK
    public boolean compileWhile() throws Exception {
        pushStack("<whileStatement>");
        if (compileKeyWord(KeyWord.WHILE) == null)
            return popStack();
        String label = UUID.randomUUID().toString();
        vmwriter.writeLabel(label + "-while-start");
        assert compileSymbol("(") != null;
        assert compileExpression();
        assert compileSymbol(")") != null;
        vmwriter.writeIf(label + "while-end");
        assert compileSymbol("{") != null;
        assert compileStatements();
        assert compileSymbol("}") != null;
        vmwriter.writeGoto(label + "-while-start");
        vmwriter.writeLabel(label + "-while-end");
        return pushStack("</whileStatement>");
    }

    // OK
    public boolean compileReturn() throws Exception {
        pushStack("<returnStatement>");
        if (compileKeyWord(KeyWord.RETURN) == null)
            return popStack();
        boolean isVoidReturn = !compileExpression();
        assert compileSymbol(";") != null;
        if (isVoidReturn)
            vmwriter.writePush(SegmentType.CONST, 0);
        vmwriter.writeReturn();
        return pushStack("</returnStatement>");
    }

    // OK
    public boolean compileIf() throws Exception {
        pushStack("<ifStatement>");
        if (compileKeyWord(KeyWord.IF) == null)
            return popStack();
        assert compileSymbol("(") != null;
        assert compileExpression();
        assert compileSymbol(")") != null;
        String label = UUID.randomUUID().toString();
        vmwriter.writeIf(label + "-else");
        assert compileSymbol("{") != null;
        assert compileStatements();
        assert compileSymbol("}") != null;
        vmwriter.writeGoto(label + "-endif");
        vmwriter.writeLabel(label + "-else");
        if (compileKeyWord(KeyWord.ELSE) != null) {
            assert compileSymbol("{") != null;
            assert compileStatements();
            assert compileSymbol("}") != null;
        }
        vmwriter.writeLabel(label + "-endif");
        return pushStack("</ifStatement>");
    }

    // OK
    public boolean compileExpression() throws Exception {
        pushStack("<expression>");
        if (!compileTerm())
            return popStack();
        String op = compileOp();
        while (op != null) {
            assert compileTerm();
            switch (op) {
                // +-*/&|<>=
                case "+":
                    vmwriter.writeArithmetic(CommandType.ADD);
                    break;
                case "-":
                    vmwriter.writeArithmetic(CommandType.SUB);
                    break;
                case "*":
                    vmwriter.writeCall("Math.multiply", 2);
                    break;
                case "/":
                    vmwriter.writeCall("Math.divide", 2);
                    break;
                case "&":
                    vmwriter.writeArithmetic(CommandType.AND);
                    break;
                case "|":
                    vmwriter.writeArithmetic(CommandType.OR);
                    break;
                case "<":
                    vmwriter.writeArithmetic(CommandType.LT); // TODO: OK?
                    break;
                case ">":
                    vmwriter.writeArithmetic(CommandType.GT); // TODO: OK?
                    break;
                case "=":
                    vmwriter.writeArithmetic(CommandType.EQ); // TODO: OK?
                    break;
                default:
                    assert false;
            }
            op = compileOp();
        }
        return pushStack("</expression>");
    }

    // OK
    public boolean compileTerm() throws Exception {
        pushStack("<term>");
        boolean ok = false;
        if (compileSymbol("(") != null) {
            assert compileExpression();
            assert compileSymbol(")") != null;
            ok = true;
        }
        if (!ok) {
            String op = compileUnaryOp();
            if (op != null) {
                assert compileTerm();
                switch (op) {
                    case "-":
                        vmwriter.writeArithmetic(CommandType.NEG);
                        break;
                    case "~":
                        vmwriter.writeArithmetic(CommandType.NOT);
                        break;
                    default:
                        assert false;
                }
                ok = true;
            }
        }
        if (!ok) {
            ok = compileIntegerConstant() || compileStringConstant() || compileKeywordConstant();
        }
        if (!ok) {
            String name = compileIdentifier();
            if (name != null) {
            // @formatter:off
            //
            // varName | (varName [ expression ]) | subroutinecall
            // subroutinecall = subroutineName (...) | (className | varName) . subroutineName (...)
            //                  
            // -> expand
            // 1. varName
            // 2. varName [ expression ]
            // 3. subroutineName ( expressionList )
            // 4. (className | varName) . subroutineName ( expressionList )
            //
            // @formatter:on
                if (compileSymbol("[") != null) { // 2
                    assert compileExpression();
                    assert compileSymbol("]") != null;

                    SegmentType segment = ensure(symbolTable.kindOf(name) == SymbolKind.ARG ? SegmentType.ARG
                            : symbolTable.kindOf(name) == SymbolKind.FIELD ? SegmentType.THIS
                                    : symbolTable.kindOf(name) == SymbolKind.STATIC ? SegmentType.STATIC
                                            : symbolTable.kindOf(name) == SymbolKind.VAR ? SegmentType.LOCAL : null);
                    vmwriter.writePush(segment, symbolTable.indexOf(name));
                    vmwriter.writeArithmetic(CommandType.ADD);
                    vmwriter.writePop(SegmentType.POINTER, 1);
                    vmwriter.writePush(SegmentType.THAT, 0);
                } else if (compileSymbol("(") != null) { // 3
                    int nArgs = compileExpressionList();
                    assert compileSymbol(")") != null;
                    vmwriter.writeCall(name, nArgs);
                } else if (compileSymbol(".") != null) { // 4
                    String name2 = ensure(compileIdentifier()); // subroutineName
                    assert compileSymbol("(") != null;
                    int nArgs = compileExpressionList();
                    assert compileSymbol(")") != null;
                    if (symbolTable.contains(name))
                        vmwriter.writeCall(symbolTable.typeOf(name) + "." + name2, nArgs); // varName
                    else
                        vmwriter.writeCall(name + "." + name2, nArgs); // className
                } else {
                    SegmentType segment = ensure(symbolTable.kindOf(name) == SymbolKind.ARG ? SegmentType.ARG
                            : symbolTable.kindOf(name) == SymbolKind.FIELD ? SegmentType.THIS
                                    : symbolTable.kindOf(name) == SymbolKind.STATIC ? SegmentType.STATIC
                                            : symbolTable.kindOf(name) == SymbolKind.VAR ? SegmentType.LOCAL : null);
                    vmwriter.writePush(segment, symbolTable.indexOf(name));
                    // 1
                }
                ok = true;
            }
        }
        if (!ok)
            return popStack();
        return pushStack("</term>");
    }

    // OK
    public int compileExpressionList() throws Exception {
        int numExpression = 0;
        pushStack("<expressionList>");
        if (compileExpression()) {
            numExpression++;
            while (compileSymbol(",") != null) {
                assert compileExpression();
                numExpression++;
            }
        }
        pushStack("</expressionList>");
        return numExpression;
    }

    // OK
    private boolean compileStatement() throws Exception {
        return compileLet() || compileIf() || compileWhile() || compileDo() || compileReturn();
    }

    private SegmentType symbolKind2SegmentType(SymbolKind kind) {
        switch (kind) {
            case STATIC:
                return SegmentType.STATIC;
            case FIELD:
                return SegmentType.THAT;
            case ARG:
                return SegmentType.ARG;
            case VAR:
                return SegmentType.LOCAL;
            default:
                assert false;
        }
        return null;
    }

    // OK
    private boolean compileSubroutineCall() throws Exception {
        String name = compileIdentifier();
        if (name == null)
            return false; // subroutineName, className or varName
        boolean pushSelf = false;
        if (compileSymbol(".") != null) {
            String subroutineName = ensure(compileSubroutineName()); // 1st identifier is className or varName
            if (symbolTable.contains(name)) {
                SegmentType segment = symbolKind2SegmentType(symbolTable.kindOf(name));
                vmwriter.writePush(segment, symbolTable.indexOf(name)); // self
                pushSelf = true;
                name = symbolTable.typeOf(name) + "." + subroutineName; // varName
            } else {
                name = name + "." + subroutineName; // className
            }
        }
        assert compileSymbol("(") != null;
        int nArgs = compileExpressionList() + (pushSelf ? 1 : 0);
        assert compileSymbol(")") != null;
        vmwriter.writeCall(name, nArgs);
        return true;
    }

    // OK
    private boolean compileSubroutineBody() throws Exception {
        pushStack("<subroutineBody>");
        if (compileSymbol("{") == null)
            return popStack();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == KeyWord.VAR) {
            assert compileVarDec();
        }
        compileStatements();
        assert compileSymbol("}") != null;
        return pushStack("</subroutineBody>");
    }

    // -------------------------------------------------------------------------------------------------

    private String compileType() throws Exception {
        KeyWord w = compileKeyWord(KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN);
        if (w != null)
            return w.toString();
        return compileClassName();
    }

    private String compileClassName() throws Exception {
        return compileIdentifier();
    }

    private String compileSubroutineName() throws Exception {
        return compileIdentifier();
    }

    private String compileVarName() throws Exception {
        return compileIdentifier();
    }

    private String compileOp() throws Exception {
        return compileSymbol("+-*/&|<>=");
    }

    private String compileUnaryOp() throws Exception {
        return compileSymbol("-~");
    }

    private boolean compileKeywordConstant() throws Exception {
        KeyWord word = compileKeyWord(KeyWord.TRUE, KeyWord.FALSE, KeyWord.NULL, KeyWord.THIS);
        if (word == null)
            return false;
        switch (word) {
            case TRUE:
                vmwriter.writePush(SegmentType.CONST, 1);
                vmwriter.writeArithmetic(CommandType.NEG);
                break;
            case FALSE:
            case NULL:
                vmwriter.writePush(SegmentType.CONST, 0);
                break;
            case THIS:
                vmwriter.writePush(SegmentType.POINTER, 0); // TODO: あってる？
                break;
            default:
                assert false;
        }
        return true;
    }

    private KeyWord compileKeyWord(KeyWord... availableKeywords) throws Exception {
        if (tokenizer.tokenType() != TokenType.KEYWORD)
            return null;
        boolean ok = false;
        for (KeyWord key : availableKeywords) {
            ok |= key.equals(tokenizer.keyWord());
        }
        if (!ok)
            return null;
        KeyWord res = tokenizer.keyWord();
        pushStack("!<keyword> " + res.toString().toLowerCase() + " </keyword>");
        tokenizer.advance();
        return res;
    }

    private String compileSymbol(String availableSimbols) throws Exception {
        if (tokenizer.tokenType() != TokenType.SYMBOL)
            return null;
        if (availableSimbols.indexOf(tokenizer.symbol()) < 0)
            return null;
        String res = tokenizer.symbol();
        pushStack("!<symbol> " + res.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + " </symbol>");
        tokenizer.advance();
        return res;
    }

    private String compileIdentifier() throws Exception {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER)
            return null;
        String res = tokenizer.identifier();
        pushStack("!<identifier> " + res + " </identifier>");
        tokenizer.advance();
        return res;
    }

    private boolean compileIntegerConstant() throws Exception {
        if (tokenizer.tokenType() != TokenType.INT_CONST)
            return false;
        Integer res = tokenizer.intVal();
        vmwriter.writePush(SegmentType.CONST, res);
        pushStack("!<integerConstant> " + res.toString() + " </integerConstant>");
        tokenizer.advance();
        return true;
    }

    private boolean compileStringConstant() throws Exception {
        if (tokenizer.tokenType() != TokenType.STRING_CONST)
            return false;
        String res = tokenizer.stringVal();
        vmwriter.writePush(SegmentType.CONST, res.length());
        vmwriter.writeCall("String.new", 1);
        for (char c : res.toCharArray()) {
            vmwriter.writePush(SegmentType.CONST, c);
            vmwriter.writeCall("String.appendChar", 2);
        }
        pushStack("!<stringConstant> " + res + " </stringConstant>");
        tokenizer.advance();
        return true;
    }
}