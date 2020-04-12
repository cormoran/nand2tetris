package src.vmtranslator;

import java.io.IOException;
import java.io.Writer;

public class CodeWriter {
    Writer out;
    int lineNumber;

    public CodeWriter(Writer out) throws IOException {
        this.out = out;
        this.lineNumber = 0;
        this.writeInit();
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
        this.write("@256");
        this.write("D=A");
        this.write("@SP");
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
                this.write("A=M");
                this.write(String.format("D=D%sM", code)); // D=arg1+arg2
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
                int l1 = this.write("D=D-M");
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
                this.write("@true");
                this.write("D=A");
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
                this.write("M=M-1"); // SP=*SP-1
                this.write("A=M"); // A=*SP
                this.write(String.format("M=%sM", code)); // !arg1
                this.write("@SP");
                this.write("M=M+1");
                break;
            default:
                assert false;
        }
    }

    public void writePushPop(CommandType cType, String segment, int index) throws IOException {
        switch (segment) {
            case "constant":
                switch (cType) {
                    case C_PUSH:
                        this.write(String.format("@%d", index)); // @index
                        this.write("D = A");
                        this.write("@SP");
                        this.write("A = M");
                        this.write("M = D");
                        this.write("D = A + 1");
                        this.write("@SP");
                        this.write("M = D");
                        break;
                    case C_POP:
                        assert false;
                        break;
                }
                break;
            default:
                assert false;
        }

    }
}