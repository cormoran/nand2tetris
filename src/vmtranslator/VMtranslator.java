package src.vmtranslator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Stack;

import jdk.nashorn.internal.runtime.ParserException;

class VMTranslator { 
    CodeWriter writer;
    
    VMTranslator(String outFileName, boolean writeInit) throws IOException {
        writer = new CodeWriter(new FileWriter(outFileName));
        if(writeInit) writer.writeInit();
    }

    public void close() throws IOException {
        writer.close();
    }

    public void processDir(String dirName) throws IOException {
        FileSystem fs = FileSystems.getDefault();
        PathMatcher matcher = fs.getPathMatcher("glob:**/*.vm");
        Iterator<Path> itr = Files.walk(Paths.get(dirName)).filter(matcher::matches).iterator();
        while(itr.hasNext()) {            
            Path path = itr.next();            
            processFile(path.toFile());            
        }
    }

    public void processFile(File file) throws IOException {
        writer.setFileName(file.getName());
        Parser parser = new Parser(new FileReader(file));
        String currentFunction = "";
        while (parser.hasMoreCommands()) {
            parser.advance();
            CommandType cType = parser.commandType();
            switch (cType) {
                case C_PUSH:
                case C_POP:
                    writer.writePushPop(cType, parser.arg1(), parser.arg2());
                    break;
                case C_ARITHMETIC:
                    writer.writeArithmetic(parser.arg1());
                    break;
                case C_LABEL:
                    writer.writeLabel(currentFunction + parser.arg1());
                    break;
                case C_GOTO:
                    writer.writeGoto(currentFunction + parser.arg1());
                    break;
                case C_IF:
                    writer.writeIf(currentFunction + parser.arg1()); 
                    break;
                case C_FUNCTION:
                    writer.writeFunction(parser.arg1(), parser.arg2());
                    currentFunction = parser.arg1() + "$";
                    break;
                case C_RETURN:
                    writer.writeReturn();
                    break;
                case C_CALL:
                    writer.writeCall(parser.arg1(), parser.arg2());
                    break;
                default:
                    throw new ParserException("unsupported command: " + cType.toString());
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        assert args.length <= 3;

        String outFileName = args.length >= 2 ? args[1] : args[0].replace("vm", "asm");
        System.out.println("* output file: " + outFileName);

        boolean noWriteInit = args.length >= 3 && args[2].startsWith("1");

        VMTranslator v = new VMTranslator(outFileName, !noWriteInit);

        File input = new File(args[0]);    
        if(input.isDirectory()) {
            System.out.println("* input directory: " + args[0]);
            v.processDir(args[0]);
        } else if(input.isFile()) {
            System.out.println("* input file: " + args[0]);
            v.processFile(input);
        } else {
            System.out.println("* error: input file " + args[0]);
        }                            
        v.close();
    }
}