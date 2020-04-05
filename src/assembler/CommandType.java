package src.assembler;

public enum CommandType {
    A_COMMAND("A"), C_COMMAND("C"), L_COMMAND("L"), EMPTY("NULL");

    private final String text;

    private CommandType(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}