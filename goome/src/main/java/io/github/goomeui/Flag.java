package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Flag {
    private final Cell c = new Cell(this, 8);

    public Flag() {
    }

    public Flag(boolean value) {
        set(value);
    }

    long ptr() {
        return c.p;
    }

    public boolean get() {
        return N.getInt(c.p) != 0;
    }

    public void set(boolean v) {
        N.putInt(c.p, v ? 1 : 0);
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public String toString() {
        return "Flag(" + get() + ")";
    }
}
