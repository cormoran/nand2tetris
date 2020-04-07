package src.vmtranslator;

import java.io.IOException;
import java.io.Writer;

public class CodeWriter {
    Writer out;

    public CodeWriter(Writer out) {
        this.out = out;
    }

    public void close() throws IOException {
        this.out.close();
    }

    // public void setFileName(String fileName) {

    // }

    public void writeArithmetic(String command) {

    }

    public void writePushPop(CommandType cType, String segment, int index) {
        switch (cType) {
            case C_PUSH:
            case C_POP:
        }
    }
}