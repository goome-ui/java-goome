package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Window {
    private final Cell c = new Cell(this, 40);

    public Window(double x, double y, double w, double h) {
        x(x).y(y).w(w).h(h).open(true);
    }

    long x0() {
        return c.p;
    }

    long open0() {
        return c.p + 32;
    }

    public double x() {
        return N.getDouble(c.p);
    }

    public Window x(double v) {
        N.putDouble(c.p, v);
        return this;
    }

    public double y() {
        return N.getDouble(c.p + 8);
    }

    public Window y(double v) {
        N.putDouble(c.p + 8, v);
        return this;
    }

    public double w() {
        return N.getDouble(c.p + 16);
    }

    public Window w(double v) {
        N.putDouble(c.p + 16, v);
        return this;
    }

    public double h() {
        return N.getDouble(c.p + 24);
    }

    public Window h(double v) {
        N.putDouble(c.p + 24, v);
        return this;
    }

    public boolean open() {
        return N.getInt(c.p + 32) != 0;
    }

    public Window open(boolean v) {
        N.putInt(c.p + 32, v ? 1 : 0);
        return this;
    }

    @Override
    public String toString() {
        return "Window(" + x() + ", " + y() + ", " + w() + ", " + h() + (open() ? "" : ", closed") + ")";
    }
}
