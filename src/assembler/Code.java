package src.assembler;

import java.text.ParseException;

class Code {
    public int dest(String dest) throws ParseException {
        if (dest == null)
            return 0b000;
        int res = 0;
        boolean is_number = false;
        for (char c : dest.toCharArray()) {
            if (c == 'M')
                res |= 0b001;
            else if (c == 'D')
                res |= 0b010;
            else if (c == 'A')
                res |= 0b100;
            else if (isNumber(c)) {
                is_number = true;
            } else
                throw new ParseException("invalid dest: " + dest, 0);
        }
        if (is_number && res == 0) {
            res = Integer.parseInt(dest);
            if (res < 0 || 0b111 < res)
                throw new ParseException("invalid dest: " + dest, 0);
        } else if (is_number) {
            throw new ParseException("invalid dest: " + dest, 0);
        }
        return res;
    }

    public int comp(String comp) throws ParseException {
        switch (comp) {
            case "0":
                return 0b0101010;
            case "1":
                return 0b0111111;
            case "-1":
                return 0b0111010;
            case "D":
                return 0b0001100;
            case "A":
                return 0b0110000;
            case "!D":
                return 0b0001101;
            case "!A":
                return 0b0110001;
            case "-D":
                return 0b0001111;
            case "-A":
                return 0b0110011;
            case "D+1":
                return 0b0011111;
            case "A+1":
                return 0b0110111;
            case "D-1":
                return 0b0001110;
            case "A-1":
                return 0b0110010;
            case "D+A":
                return 0b0000010;
            case "D-A":
                return 0b0010011;
            case "A-D":
                return 0b0000111;
            case "D&A":
                return 0b0000000;
            case "D|A":
                return 0b0010101;
            //
            case "M":
                return 0b1110000;
            case "!M":
                return 0b1110001;
            case "-M":
                return 0b1110011;
            case "M+1":
                return 0b1110111;
            case "M-1":
                return 0b1110010;
            case "D+M":
                return 0b1000010;
            case "D-M":
                return 0b1010011;
            case "M-D":
                return 0b1000111;
            case "D&M":
                return 0b1000000;
            case "D|M":
                return 0b1010101;
            default:
                int res = Integer.parseInt(comp);
                if (res < 0 || 0b1111111 < res)
                    throw new ParseException("invalid comp: " + comp, 0);
                return res;
        }
    }

    public int jump(String jump) throws ParseException {
        if (jump == null)
            return 0b000;
        switch (jump) {
            case "JGT":
                return 0b001;
            case "JEQ":
                return 0b010;
            case "JGE":
                return 0b011;
            case "JLT":
                return 0b100;
            case "JNE":
                return 0b101;
            case "JLE":
                return 0b110;
            case "JMP":
                return 0b111;
            default:
                int res = Integer.parseInt(jump);
                if (res < 0b000 || 0b111 < res)
                    throw new ParseException("invalid jump: " + jump, 0);
                return res;
        }
    }

    private boolean isNumber(int c) {
        return '0' <= c && c <= '9';
    }

}