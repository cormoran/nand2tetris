package src.vmtranslator;

public enum CommandType {
    C_ARITHMETIC("ARITHMETIC"), C_PUSH("PUSH"), C_POP("POP"), C_LABEL("LABEL"), C_GOTO("GOTO"), C_IF("IF"),
    C_FUNCTION("FUNCTION"), C_RETURN("RETURN"), C_CALL("CALL"), C_NULL("NULL");

    private final String text;

    private CommandType(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}