package src.assembler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

class Assembler {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        assert args.length == 1;
        Parser parser = new Parser(new FileReader(args[0]));
        System.out.println("* loading " + args[0]);
        while (parser.hasMoreCommands()) {
            parser.advance();
            System.out.println(parser.commandType());
        }
    }
}