package src.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Analyzer {
    Tokenizer tokenizer;
    CompliationEngine engine;

    Analyzer(String inputFilePath, String outFilePath) throws IOException {
        // tokenizer = new Tokenizer()
        // engine = new CompliationEngine(in, out)
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        assert args.length <= 2;

        String outFileName = args.length >= 2 ? args[1] : args[0].replace("jack", "vm");
        System.out.println("* output file: " + outFileName);

        // VMTranslator v = new VMTranslator(outFileName, !noWriteInit);

        // File input = new File(args[0]);
        // if (input.isDirectory()) {
        // System.out.println("* input directory: " + args[0]);
        // v.processDir(args[0]);
        // } else if (input.isFile()) {
        // System.out.println("* input file: " + args[0]);
        // v.processFile(input);
        // } else {
        // System.out.println("* error: input file " + args[0]);
        // }
        // v.close();
    }
}