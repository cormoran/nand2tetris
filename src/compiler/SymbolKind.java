package src.compiler;

public enum SymbolKind {
    STATIC("STATIC"), FIELD("FIELD"), ARG("ARG"), VAR("VAR");

    private final String text;

    private SymbolKind(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}