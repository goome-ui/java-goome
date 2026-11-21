package io.github.goomeui;

/** Width and height of measured text. */
public final class TextSize {
    /** Width and height. */
    public final double w, h;

    TextSize(double w, double h) {
        this.w = w;
        this.h = h;
    }

    @Override
    public String toString() {
        return "TextSize(" + w + " x " + h + ")";
    }
}
