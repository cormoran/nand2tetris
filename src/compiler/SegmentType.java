package src.compiler;

public enum SegmentType {
    CONST("CONST"), ARG("ARG"), LOCAL("LOCAL"), STATIC("STATIC"), THIS("THIS"), THAT("THAT"), POINTER("POINTER"),
    TEMP("TEMP");

    private final String text;

    private SegmentType(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }
}