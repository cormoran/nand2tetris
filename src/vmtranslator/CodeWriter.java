package src.vmtranslator;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;

public class CodeWriter {
    String filename;
    Writer out;
    int lineNumber;

    public CodeWriter(Writer out, String filename) throws IOException {
        this.filename = filename;
        this.out = out;
        this.lineNumber = 0;
        // this.writeInit();
    }

    public void close() throws IOException {
        this.out.close();
    }

    // public void setFileName(String fileName) {

    // }
    private int write(String line) throws IOException {
        this.out.write(line);
        this.out.write("\n");
        return this.lineNumber++;
    }

    private void writeInit() throws IOException {
        // SP
        this.write("@256");
        this.write("D=A");
        this.write("@SP");
        this.write("M=D");
        // local, argument, this, that
        this.write("@2048");
        this.write("D=A");
        this.write("@LCL");
        this.write("M=D");
        this.write("@ARG");
        this.write("M=D");
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
                int l1 = this.write("D=M-D");
                this.write(String.format("@%d", l1 + 12));
                this.write(String.format("D; %s", code));
                // false
                this.write("@0");
                this.write("D=A");
                this.write("@SP");
                this.write("A=M");
                this.write("M=D");
                this.write("@SP");
                int l2 = this.write("M=M+1");
                this.write(String.format("@%d", l2 + 10));
                this.write("0;JMP");
                // true
                this.write("@0");
                this.write("D=A-1");
                this.write("@SP");
                this.write("A=M");
                this.write("M=D");
                this.write("@SP");
                this.write("M=M+1");
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
                }
                break;
            default:
                assert false;
        }

    }
}