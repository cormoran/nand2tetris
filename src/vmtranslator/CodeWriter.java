package src.vmtranslator;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CodeWriter {
    String filename;
    Writer out;
    int lineNumber;
    int callCnt;

    public CodeWriter(Writer out) throws IOException {        
        this.out = out;
        this.lineNumber = 0;
        this.callCnt = 0;
    }

    public void close() throws IOException {
        this.out.close();
    }

    public void setFileName(String fileName) throws IOException {
        this.filename = fileName;
        this.write(String.format("// %s", fileName));
    }
    private int write(String line) throws IOException {
        this.out.write(line);
        this.out.write("\n");        
        return this.lineNumber++;
    }

    public void writeInit() throws IOException {
        // SP
        this.write("@256");
        this.write("D=A");
        this.write("@SP");
        this.write("M=D");

        this.write("@256");
        this.write("D=A");
        this.write("@THIS");
        this.write("M=D");

        this.write("@256");
        this.write("D=A");
        this.write("@THAT");
        this.write("M=D");
                
        this.writeCall("Sys.init", 0);
    }

    public void writeArithmetic(String command) throws IOException {
        String code;
        switch (command) {
            case "add":
            case "sub":
            case "and":
            case "or":
                code = command.equals("add") ? "+" : command.equals("sub") ? "-" : command.equals("and") ? "&" : "|";
                this.write("@SP");
                this.write("M=M-1"); // SP=*SP-1
                this.write("A=M"); // A=*SP
                this.write("D=M"); // arg1
                this.write("@SP");
                this.write("M=M-1"); // SP=*SP-1
                this.write("A=M"); // arg2
                this.write(String.format("D=M%sD", code)); // D=arg1+arg2
                this.write("@SP");
                this.write("A=M");
                this.write("M=D");
                this.write("@SP");
                this.write("M=M+1");
                break;
            case "eq":
            case "gt":
            case "lt":
                code = command.equals("eq") ? "JEQ" : command.equals("gt") ? "JGT" : "JLT";
                this.write("@SP");
                this.write("M=M-1"); // SP=*SP-1
                this.write("A=M"); // A=*SP
                this.write("D=M"); // arg1
                this.write("@SP");
                this.write("M=M-1"); // SP=*SP-1
                this.write("A=M");
                this.write("D=M-D");
                this.write(String.format("@__JUMP_TRUE_%d__", callCnt));
                this.write(String.format("D; %s", code));
                // false
                this.write("@0");
                this.write("D=A");
                this.write("@SP");
                this.write("A=M");
                this.write("M=D");
                this.write("@SP");
                this.write("M=M+1");
                this.write(String.format("@__JUMP_END_%d__", callCnt));
                this.write("0;JMP");
                // true
                this.write(String.format("(__JUMP_TRUE_%d__)", callCnt));
                this.write("@0");
                this.write("D=A-1");
                this.write("@SP");
                this.write("A=M");
                this.write("M=D");
                this.write("@SP");
                this.write("M=M+1");
                this.write(String.format("(__JUMP_END_%d__)", callCnt++));
                break;
            case "neg":
            case "not":
                code = command.equals("neg") ? "-" : "!";
                this.write("@SP");
                this.write("A=M-1"); // A=*SP
                this.write(String.format("M=%sM", code)); // !arg1
                break;
            default:
                assert false;
        }
    }

    public void writePushPop(CommandType cType, String segment, int index) throws IOException {
        String base;
        switch (segment) {
            case "constant":
                switch (cType) {
                    case C_PUSH:
                        this.write(String.format("@%d", index)); // @index
                        this.write("D=A");
                        this.write("@SP");
                        this.write("A=M");
                        this.write("M=D");
                        this.write("@SP");
                        this.write("M=M+1");
                        break;
                    case C_POP:
                        assert false;
                        break;
                    default:
                        assert false;
                }
                break;
            case "pointer":
            case "temp":
                base = segment.equals("pointer") ? "@R3" : "@R5";
                switch (cType) {
                    case C_PUSH:
                        this.write(base);
                        this.write("D=A");
                        this.write(String.format("@%d", index)); // @index
                        this.write("A=D+A"); // pointer of local[index]
                        this.write("D=M"); // value of local[index]
                        this.write("@SP");
                        this.write("A=M");
                        this.write("M=D");
                        this.write("@SP");
                        this.write("M=M+1");
                        break;
                    case C_POP:
                        this.write(base);
                        this.write("D=A");
                        this.write(String.format("@%d", index)); // @index
                        this.write("D=D+A"); // pointer of local[index]
                        this.write("@R13");
                        this.write("M=D"); // RAM5 = pointer of local[index]
                        this.write("@SP");
                        this.write("M=M-1");
                        this.write("A=M");
                        this.write("D=M"); // pop value to D
                        this.write("@R13");
                        this.write("A=M"); // pointer of local[index]
                        this.write("M=D");
                        break;
                    default:
                        assert false;
                }
                break;
            case "local":
            case "argument":
            case "this":
            case "that":
                base = segment.equals("local") ? "@LCL"
                        : segment.equals("argument") ? "@ARG" : segment.equals("this") ? "@THIS" : "@THAT";
                switch (cType) {
                    case C_PUSH:
                        this.write(base);
                        this.write("D=M");
                        this.write(String.format("@%d", index)); // @index
                        this.write("A=D+A"); // pointer of local[index]
                        this.write("D=M"); // value of local[index]
                        this.write("@SP");
                        this.write("A=M");
                        this.write("M=D");
                        this.write("@SP");
                        this.write("M=M+1");
                        break;
                    case C_POP:
                        this.write(base);
                        this.write("D=M");
                        this.write(String.format("@%d", index)); // @index
                        this.write("D=D+A"); // pointer of local[index]
                        this.write("@R13");
                        this.write("M=D"); // RAM5 = pointer of local[index]
                        this.write("@SP");
                        this.write("M=M-1");
                        this.write("A=M");
                        this.write("D=M"); // pop value to D
                        this.write("@R13");
                        this.write("A=M"); // pointer of local[index]
                        this.write("M=D");
                        break;
                    default:
                        assert false;
                }
                break;
            case "static":
                switch (cType) {
                    case C_PUSH:
                        this.write(String.format("@%s.%d", filename, index));
                        this.write("D=M");
                        this.write("@SP");
                        this.write("A=M");
                        this.write("M=D");
                        this.write("@SP");
                        this.write("M=M+1");
                        break;
                    case C_POP:
                        this.write("@SP");
                        this.write("M=M-1");
                        this.write("A=M");
                        this.write("D=M"); // pop value to D
                        this.write(String.format("@%s.%d", filename, index));
                        this.write("M=D");
                        this.write("@SP");
                        break;
                    default:
                        assert false;
                }
                break;
            default:
                assert false;
        }

    }

    public void writeLabel(String label) throws IOException {
        // TODO: validate label string
        this.write(String.format("(%s)", label));
    }

    public void writeGoto(String label) throws IOException {
        this.write(String.format("@%s", label));
        this.write("0;JMP");
    }

    public void writeIf(String label) throws IOException {
        this.write("@SP");
        this.write("M=M-1");
        this.write("A=M");
        this.write("D=M");
        this.write(String.format("@%s", label));
        this.write("D;JNE");
    }

    public void writeCall(String functionName, int numArgs) throws IOException {
        String returnAddress = String.format("__RETURN__%s__", callCnt++);
        // save state
        this.write(String.format("@%s", returnAddress));
        this.write("D=A");
        this.write("@SP");
        this.write("A=M");
        this.write("M=D");
        this.write("@SP");
        this.write("M=M+1");

        for (String key : new String[] {"LCL", "ARG", "THIS", "THAT" }) {
            this.write(String.format("@%s", key));            
            this.write("D=M");
            this.write("@SP");
            this.write("A=M");
            this.write("M=D");
            this.write("@SP");
            this.write("M=M+1");
        }
        // set arg
        this.write("@SP");
        this.write("D=M");
        this.write("@5");
        this.write("D=D-A");
        this.write(String.format("@%d", numArgs));
        this.write("D=D-A");
        this.write("@ARG");
        this.write("M=D");
        // set lcl
        this.write("@SP");
        this.write("D=M");
        this.write("@LCL");
        this.write("M=D");        
        // go
        this.write(String.format("@%s", functionName));
        this.write("0; JMP");
        this.write(String.format("(%s)", returnAddress));
    }

    public void writeReturn() throws IOException {
        this.write("@LCL");
        this.write("D=M");
        this.write("@FRAME");
        this.write("M=D");

        // backup return address (may be overriten when set ret val)
        this.write("@FRAME");
        this.write("D=M");
        this.write("@5");
        this.write("A=D-A");
        this.write("D=M");
        this.write("@RET");
        this.write("M=D");

        // set ret val
        this.write("@SP");
        this.write("M=M-1");        
        this.write("A=M");
        this.write("D=M"); // D = return
        this.write("@ARG");
        this.write("A=M");
        this.write("M=D"); // *ARG = return
        // set sp
        this.write("@ARG");        
        this.write("D=M+1");
        this.write("@SP");
        this.write("M=D");
        //         
        
        for (String key : new String[] { "THAT", "THIS", "ARG", "LCL"}) {
            this.write("@FRAME");
            this.write("M=M-1");
            this.write("A=M");
            this.write("D=M");
            this.write(String.format("@%s", key));
            this.write("M=D");            
        }
        this.write("@RET");    
        this.write("A=M");
        this.write("0; JMP");        
    }

    public void writeFunction(String functionName, int numLocals) throws IOException {
        this.write(String.format("(%s)", functionName));
        for (int i = 0; i < numLocals; i++) { // set 0
            this.write("@SP");
            this.write("A=M");
            this.write("M=0");
            this.write("@SP");
            this.write("M=M+1");
        }
    }
}