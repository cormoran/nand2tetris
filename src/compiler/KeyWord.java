package src.compiler;

public enum KeyWord {
    CLASS("CLASS"), METHOD("METHOD"), FUNCTION("FUNCTION"), CONSTRUCTOR("CONSTRUCTOR"), INT("INT"), BOOLEAN("BOOLEAN"),
    CHAR("CHAR"), VOID("VOID"), VAR("VAR"), STATIC("STATIC"), FIELD("FIELD"), LET("LET"), DO("DO"), IF("IF"),
    ELSE("ELSE"), WHILE("WHILE"), RETURN("RETURN"), TRUE("TRUE"), FALSE("FALSE"), NULL("NULL"), THIS("THIS");

    private final String text;

    private KeyWord(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}
