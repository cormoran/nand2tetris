package src.compiler;

import java.io.IOException;
import java.io.Writer;

public class VMWriter {
    Writer out;

    VMWriter(Writer out) {
        this.out = out;
    }

    public void writePush(SegmentType segment, int index) throws IOException {
        this.out.write("\tpush " + segment.toString().toLowerCase() + " " + index + "\n");
    }

    public void writePop(SegmentType segment, int index) throws IOException {
        this.out.write("\tpop " + segment.toString().toLowerCase() + " " + index + "\n");
    }

    public void writeArithmetic(CommandType command) throws IOException {
        this.out.write("\t" + command.toString().toLowerCase() + "\n");
    }

    public void writeLabel(String label) throws IOException {
        this.out.write("label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        this.out.write("\tgoto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        this.out.write("\tif-goto " + label + "\n");
    }

    public void writeCall(String name, int nArgs) throws IOException {
        this.out.write("\tcall " + name + " " + nArgs + "\n");
    }

    public void writeFunction(String label, int nLocals) throws IOException {
        this.out.write("function " + label + " " + nLocals + "\n");
    }

    public void writeReturn() throws IOException {
        this.out.write("\treturn\n");
    }

    public void close() throws IOException {
        out.close();
    }
}