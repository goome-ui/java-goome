package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Split {
    private final Cell c = new Cell(this, 8);
    public boolean vertical;

    public Split() {
        this(0.5, false);
    }

    public Split(double frac, boolean vertical) {
        frac(frac);
        this.vertical = vertical;
    }

    long ptr() {
        return c.p;
    }

    public double frac() {
        return N.getDouble(c.p);
    }

    public void frac(double v) {
        N.putDouble(c.p, v);
    }

    @Override
    public String toString() {
        return "Split(" + frac() + (vertical ? ", vertical" : "") + ")";
    }
}
