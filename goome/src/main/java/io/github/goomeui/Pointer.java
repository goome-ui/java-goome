package io.github.goomeui;

public final class Pointer {
    public final double x, y;
    public final int buttons;

    Pointer(double x, double y, int buttons) {
        this.x = x;
        this.y = y;
        this.buttons = buttons;
    }

    @Override
    public String toString() {
        return "Pointer(" + x + ", " + y + ", buttons " + buttons + ")";
    }
}
