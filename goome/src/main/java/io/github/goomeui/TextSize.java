package io.github.goomeui;

public final class TextSize {
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
