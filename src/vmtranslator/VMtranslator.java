package src.vmtranslator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

import jdk.nashorn.internal.runtime.ParserException;

class VMTranslator {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        assert args.length <= 2;
        String outFileName = args.length >= 2 ? args[1] : args[0].replace("vm", "asm");
        System.out.println("* input file: " + args[0]);
        System.out.println("* output file: " + outFileName);

        Parser parser = new Parser(new FileReader(args[0]));
        CodeWriter writer = new CodeWriter(new FileWriter(outFileName), new File(outFileName).getName());

        Stack<String> scope = new Stack<String>();
        scope.push("");
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
                    writer.writeLabel(scope.peek() + parser.arg1());
                    break;
                case C_GOTO:
                    writer.writeGoto(scope.peek() + parser.arg1());
                    break;
                case C_IF:
                    writer.writeIf(scope.peek() + parser.arg1()); 
                    break;
                // case C_FUNCTION:
                //     writer.writeFunction(parser.arg1(), parser.arg2());
                //     scope.push(parser.arg1() + "$");
                //     break;
                // case C_RETURN:
                //     writer.writeReturn();
                //     scope.pop();
                //     break;
                // case C_CALL:
                //     writer.writeCall(parser.arg1(), parser.arg2());
                //     break;
                default:
                    throw new ParserException("unsupported command: " + cType.toString());
            }
        }
        writer.close();
    }
}