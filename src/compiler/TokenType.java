package src.compiler;

public enum TokenType {
    KEYWORD("KEYWORD"), SYMBOL("SYMBOL"), IDENTIFIER("IDENTIFIER"), INT_CONST("INT_CONST"),
    STRING_CONST("STRING_CONST");

    private final String text;

    private TokenType(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}