package src.assembler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;

class Assembler {
    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
        assert args.length <= 2;
        String outFileName = args.length >= 2 ? args[1] : args[0].replace("asm", "hack");
        System.out.println("* input file: " + args[0]);
        System.out.println("* output file: " + outFileName);

        SymbolTable romSymbolTable = buildROMSymbolTable(new FileReader(args[0]));
        SymbolTable ramSymbolTable = buildRAMSymbolTable();

        Parser parser = new Parser(new FileReader(args[0]));
        Code code = new Code();

        FileWriter out = new FileWriter(outFileName);

        int ramVarAddress = 16;

        while (parser.hasMoreCommands()) {
            parser.advance();
            switch (parser.commandType()) {
                case A_COMMAND:
                    String symbol = parser.symbol();
                    if (parser.isValue(symbol)) {
                        int value = Integer.parseInt(symbol);
                        out.write(toBinaryString(value, 16));
                        out.write("\n");
                    } else if (romSymbolTable.contains(symbol)) {
                        int value = romSymbolTable.getAddress(symbol);
                        out.write(toBinaryString(value, 16));
                        out.write("\n");
                    } else {
                        if (!ramSymbolTable.contains(symbol))
                            ramSymbolTable.addEntry(symbol, ramVarAddress++);
                        int value = ramSymbolTable.getAddress(symbol);
                        out.write(toBinaryString(value, 16));
                        out.write("\n");
                    }
                    break;
                case C_COMMAND:
                    int comp = code.comp(parser.comp());
                    int dest = code.dest(parser.dest());
                    int jump = code.jump(parser.jump());
                    out.write(toBinaryString((0b111 << 13) | (comp << 6) | (dest << 3) | jump, 16));
                    out.write("\n");
                    break;
                case L_COMMAND:
                    // skip
                    break;
            }
        }
        out.close();
    }

    private static SymbolTable buildRAMSymbolTable() {
        SymbolTable st = new SymbolTable();
        st.addEntry("SP", 0);
        st.addEntry("LCL", 1);
        st.addEntry("ARG", 2);
        st.addEntry("THIS", 3);
        st.addEntry("THAT", 4);
        for (int i = 0; i <= 15; i++)
            st.addEntry(String.format("R%d", i), i);
        st.addEntry("SCREEN", 16384);
        st.addEntry("KBD", 24576);
        return st;
    }

    private static SymbolTable buildROMSymbolTable(Reader in) throws IOException {
        int romAddress = 0;
        SymbolTable st = new SymbolTable();

        Parser parser = new Parser(in);
        while (parser.hasMoreCommands()) {
            parser.advance();
            switch (parser.commandType()) {
                case A_COMMAND:
                    romAddress++;
                    break;
                case C_COMMAND:
                    romAddress++;
                    break;
                case L_COMMAND:
                    String symbol = parser.symbol();
                    if (!parser.isValue(symbol))
                        st.addEntry(symbol, romAddress);
                    break;
            }
        }
        return st;
    }

    private static String toBinaryString(int n, int bit) {
        StringBuffer res = new StringBuffer();
        while (n > 0 && bit > 0) {
            res.append(n % 2 == 0 ? '0' : '1');
            n /= 2;
            bit--;
        }
        assert (n == 0);
        while (bit-- > 0)
            res.append('0');
        return res.reverse().toString();
    }
}