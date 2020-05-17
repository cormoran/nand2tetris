package src.compiler;

public enum CommandType {
    ADD("ADD"), SUB("SUB"), NEG("NEG"), EQ("EQ"), GT("GT"), LT("LT"), AND("AND"), OR("OR"), NOT("NOT");

    private final String text;

    private CommandType(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}