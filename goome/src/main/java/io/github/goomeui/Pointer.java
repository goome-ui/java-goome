package io.github.goomeui;

/** Where the mouse is and which buttons are down, from Goome.pointer(). */
public final class Pointer {
    /** Logical position. */
    public final double x, y;
    /** Bits of the buttons held. */
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
