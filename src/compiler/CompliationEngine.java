package src.compiler;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Stack;

public class CompliationEngine {
    public static void main(String[] args) throws IOException {
        assert args.length == 1;
        String outFileName = args[0].replace(".jack", ".gen.xml");

        CompliationEngine engine = new CompliationEngine(new FileReader(args[0]), new FileWriter(outFileName));
        engine.compileClass();
        engine.write();
    }

    Tokenizer tokenizer;
    Writer out;
    ArrayList<String> buffer;

    CompliationEngine(Reader in, Writer out) throws IOException {
        tokenizer = new Tokenizer(in);
        tokenizer.advance();
        this.out = out;
        this.buffer = new ArrayList<String>();
    }

    private void write() throws IOException {
        int depth = 0;
        for (String s : buffer) {
            if (s.startsWith("</"))
                depth--;
            for (int i = 0; i < depth; i++)
                out.write("\t");
            if (s.startsWith("!")) {
                out.write(s.substring(1));
            } else {
                out.write(s);
            }
            out.write("\n");
            if (!s.startsWith("</") && s.startsWith("<"))
                depth++;
        }
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

    public boolean compileClass() throws IOException {
        pushStack("<class>");
        if (!compileKeyWord(KeyWord.CLASS))
            return popStack();
        assert compileIdentifier();
        assert compileSymbol("{");
        while (compileClassVarDec())
            ;
        while (compileSubroutineDec())
            ;
        assert compileSymbol("}");
        return pushStack("</class>");
    }

    public boolean compileClassVarDec() throws IOException {
        pushStack("<classVarDec>");
        if (!compileKeyWord(KeyWord.STATIC, KeyWord.FIELD))
            return popStack();
        assert compileType();
        assert compileVarName();
        while (compileSymbol(",")) {
            assert compileVarName();
        }
        assert compileSymbol(";");
        return pushStack("</classVarDec>");
    }

    public boolean compileSubroutineDec() throws IOException {
        pushStack("<subroutineDec>");
        if (!compileKeyWord(KeyWord.CONSTRUCTOR, KeyWord.FUNCTION, KeyWord.METHOD))
            return popStack();
        assert compileKeyWord(KeyWord.VOID) || compileType();
        assert compileSubroutineName();
        assert compileSymbol("(");
        assert compileParameterList();
        assert compileSymbol(")");
        assert compileSubroutineBody();
        return pushStack("</subroutineDec>");
    }

    public boolean compileParameterList() throws IOException {
        pushStack("<parameterList>");
        if (compileType()) {
            assert compileVarName();
            while (compileSymbol(",")) {
                assert compileType();
                assert compileVarName();
            }
        }
        return pushStack("</parameterList>");
    }

    public boolean compileVarDec() throws IOException {
        pushStack("<varDec>");
        if (!compileKeyWord(KeyWord.VAR))
            return popStack();
        assert compileType();
        assert compileVarName();
        while (compileSymbol(",")) {
            assert compileVarName();
        }
        assert compileSymbol(";");
        return pushStack("</varDec>");
    }

    public boolean compileStatements() throws IOException {
        pushStack("<statements>");
        while (compileStatement())
            ;
        return pushStack("</statements>");
    }

    public boolean compileDo() throws IOException {
        pushStack("<doStatement>");
        if (!compileKeyWord(KeyWord.DO))
            return popStack();
        assert compileSubroutineCall();
        assert compileSymbol(";");
        return pushStack("</doStatement>");
    }

    public boolean compileLet() throws IOException {
        pushStack("<letStatement>");
        if (!compileKeyWord(KeyWord.LET))
            return popStack();
        assert compileVarName();

        if (compileSymbol("[")) {
            assert compileExpression();
            assert compileSymbol("]");
        }
        assert compileSymbol("=");
        assert compileExpression();
        assert compileSymbol(";");
        return pushStack("</letStatement>");
    }

    public boolean compileWhile() throws IOException {
        pushStack("<whileStatement>");
        if (!compileKeyWord(KeyWord.WHILE))
            return popStack();
        assert compileSymbol("(");
        assert compileExpression();
        assert compileSymbol(")");
        assert compileSymbol("{");
        assert compileStatements();
        assert compileSymbol("}");
        return pushStack("</whileStatement>");
    }

    public boolean compileReturn() throws IOException {
        pushStack("<returnStatement>");
        if (!compileKeyWord(KeyWord.RETURN))
            return popStack();
        compileExpression(); // allow fail
        assert compileSymbol(";");
        return pushStack("</returnStatement>");
    }

    public boolean compileIf() throws IOException {
        pushStack("<ifStatement>");
        if (!compileKeyWord(KeyWord.IF))
            return popStack();
        assert compileSymbol("(");
        assert compileExpression();
        assert compileSymbol(")");
        assert compileSymbol("{");
        assert compileStatements();
        assert compileSymbol("}");
        if (compileKeyWord(KeyWord.ELSE)) {
            assert compileSymbol("{");
            assert compileStatements();
            assert compileSymbol("}");
        }
        return pushStack("</ifStatement>");
    }

    public boolean compileExpression() throws IOException {
        pushStack("<expression>");
        if (!compileTerm())
            return popStack();
        while (compileOp()) {
            assert compileTerm();
        }
        return pushStack("</expression>");
    }

    public boolean compileTerm() throws IOException {
        pushStack("<term>");
        if (compileSymbol("(")) {
            assert compileExpression();
            assert compileSymbol(")");
        } else if (compileUnaryOp()) {
            assert compileTerm();
        } else if (compileIntegerConstant()) {

        } else if (compileStringConstant()) {

        } else if (compileKeywordConstant()) {

        } else if (compileIdentifier()) {
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
            if (compileSymbol("[")) { // 2
                assert compileExpression();
                assert compileSymbol("]");
            } else if (compileSymbol("(")) { // 3
                assert compileExpressionList();
                assert compileSymbol(")");
            } else if (compileSymbol(".")) { // 4
                assert compileIdentifier(); // subroutineName
                assert compileSymbol("(");
                assert compileExpressionList();
                assert compileSymbol(")");
            } else {
                // 1
            }
        } else
            return popStack();
        return pushStack("</term>");
    }

    public boolean compileExpressionList() throws IOException {
        pushStack("<expressionList>");
        if (compileExpression()) {
            while (compileSymbol(",")) {
                assert compileExpression();
            }
        }
        return pushStack("</expressionList>");
    }

    //

    private boolean compileStatement() throws IOException {
        return compileLet() || compileIf() || compileWhile() || compileDo() || compileReturn();
    }

    private boolean compileSubroutineCall() throws IOException {
        if (!compileIdentifier())
            return false; // subroutineName, className or varName
        if (compileSymbol(".")) {
            assert compileSubroutineName(); // 1st identifier is className or varName
        }
        assert compileSymbol("(");
        assert compileExpressionList();
        assert compileSymbol(")");
        return true;
    }

    private boolean compileSubroutineBody() throws IOException {
        pushStack("<subroutineBody>");
        if (!compileSymbol("{"))
            return popStack();
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == KeyWord.VAR) {
            compileVarDec();
        }
        compileStatements();
        assert compileSymbol("}");
        return pushStack("</subroutineBody>");
    }

    private boolean compileType() throws IOException {
        return compileKeyWord(KeyWord.INT, KeyWord.CHAR, KeyWord.BOOLEAN) || compileClassName();
    }

    private boolean compileClassName() throws IOException {
        return compileIdentifier();
    }

    private boolean compileSubroutineName() throws IOException {
        return compileIdentifier();
    }

    private boolean compileVarName() throws IOException {
        return compileIdentifier();
    }

    private boolean compileOp() throws IOException {
        return compileSymbol("+-*/&|<>=");
    }

    private boolean compileUnaryOp() throws IOException {
        return compileSymbol("-~");
    }

    private boolean compileKeywordConstant() throws IOException {
        return compileKeyWord(KeyWord.TRUE, KeyWord.FALSE, KeyWord.NULL, KeyWord.THIS);
    }

    private boolean compileKeyWord(KeyWord... availableKeywords) throws IOException {
        if (tokenizer.tokenType() != TokenType.KEYWORD)
            return false;
        boolean ok = false;
        for (KeyWord key : availableKeywords) {
            ok |= key.equals(tokenizer.keyWord());
        }
        if (!ok)
            return false;
        pushStack("!<keyword> " + tokenizer.keyWord().toString().toLowerCase() + " </keyword>");
        tokenizer.advance();
        return true;
    }

    private boolean compileSymbol(String availableSimbols) throws IOException {
        if (tokenizer.tokenType() != TokenType.SYMBOL)
            return false;
        if (availableSimbols.indexOf(tokenizer.symbol()) < 0)
            return false;
        pushStack("!<symbol> " + tokenizer.symbol().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                + " </symbol>");
        tokenizer.advance();
        return true;
    }

    private boolean compileIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER)
            return false;

        pushStack("!<identifier> " + tokenizer.identifier() + " </identifier>");
        tokenizer.advance();
        return true;
    }

    private boolean compileIntegerConstant() throws IOException {
        if (tokenizer.tokenType() != TokenType.INT_CONST)
            return false;
        pushStack("!<integerConstant> " + Integer.valueOf(tokenizer.intVal()).toString() + " </integerConstant>");
        tokenizer.advance();
        return true;
    }

    private boolean compileStringConstant() throws IOException {
        if (tokenizer.tokenType() != TokenType.STRING_CONST)
            return false;
        pushStack("!<stringConstant> " + tokenizer.stringVal() + " </stringConstant>");
        tokenizer.advance();
        return true;
    }
}