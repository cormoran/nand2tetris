package src.compiler;

import java.io.FileReader;
import java.io.FileWriter;

public class Compiler {
    public static void main(String[] args) throws Exception {
        assert args.length == 1;
        String outXmlFileName = args[0].replace(".jack", ".gen.xml");
        String outVmFileName = args[0].replace(".jack", ".vm");

        CompliationEngine engine = new CompliationEngine(new FileReader(args[0]), new FileWriter(outVmFileName),
                new FileWriter(outXmlFileName));
        engine.compileClass();
        engine.write();
    }
}