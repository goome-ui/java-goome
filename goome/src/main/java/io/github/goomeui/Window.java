package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** Position, size and open state of a floating Ui.window(). The ui moves and resizes it when the user drags. */
public final class Window {
    private final Cell c = new Cell(this, 40);

    /** A window at this spot and size, logical px. */
    public Window(double x, double y, double w, double h) {
        x(x).y(y).w(w).h(h).open(true);
    }

    long x0() {
        return c.p;
    }

    long open0() {
        return c.p + 32;
    }

    /** Left edge. */
    public double x() {
        return N.getDouble(c.p);
    }

    /** Sets the left edge. */
    public Window x(double v) {
        N.putDouble(c.p, v);
        return this;
    }

    /** Top edge. */
    public double y() {
        return N.getDouble(c.p + 8);
    }

    /** Sets the top edge. */
    public Window y(double v) {
        N.putDouble(c.p + 8, v);
        return this;
    }

    /** Width. */
    public double w() {
        return N.getDouble(c.p + 16);
    }

    /** Sets the width. */
    public Window w(double v) {
        N.putDouble(c.p + 16, v);
        return this;
    }

    /** Height. */
    public double h() {
        return N.getDouble(c.p + 24);
    }

    /** Sets the height. */
    public Window h(double v) {
        N.putDouble(c.p + 24, v);
        return this;
    }

    /** False after the user hit the close button. */
    public boolean open() {
        return N.getInt(c.p + 32) != 0;
    }

    /** Opens or closes it from code. */
    public Window open(boolean v) {
        N.putInt(c.p + 32, v ? 1 : 0);
        return this;
    }

    @Override
    public String toString() {
        return "Window(" + x() + ", " + y() + ", " + w() + ", " + h() + (open() ? "" : ", closed") + ")";
    }
}
