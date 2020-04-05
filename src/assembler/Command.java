package src.assembler;

public class Command {
    public CommandType type;
    public String symbol, dest, comp, jump;

    public Command() {
        type = CommandType.EMPTY;
    }

    @Override
    public String toString() {
        switch (type) {
            case C_COMMAND:
                return String.format("Command: type=%s dest=%s comp=%s jump=%s", type.toString(),
                        dest != null ? dest : "null", comp != null ? comp : "null", jump != null ? jump : "null");

            case A_COMMAND:
            case L_COMMAND:
                return String.format("Command: type=%s symbol=%s", type.toString(), symbol != null ? symbol : "null");

        }
        return "Command: ?";
    }
}