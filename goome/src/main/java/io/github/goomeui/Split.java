package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** State of a resizable split, pass it to Ui.splitBegin(). */
public final class Split {
    private final Cell c = new Cell(this, 8);
    /** True for a vertical split. */
    public boolean vertical;

    /** A split that starts in the middle. */
    public Split() {
        this(0.5, false);
    }

    /** A split with a starting position, frac is how much the first pane gets. */
    public Split(double frac, boolean vertical) {
        frac(frac);
        this.vertical = vertical;
    }

    long ptr() {
        return c.p;
    }

    /** How much of the space the first pane gets, 0 to 1. */
    public double frac() {
        return N.getDouble(c.p);
    }

    /** Sets how much of the space the first pane gets. */
    public void frac(double v) {
        N.putDouble(c.p, v);
    }

    @Override
    public String toString() {
        return "Split(" + frac() + (vertical ? ", vertical" : "") + ")";
    }
}
