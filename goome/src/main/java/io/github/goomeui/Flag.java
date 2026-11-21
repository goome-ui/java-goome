package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** A boolean the ui can change, for things like open menus and collapsing headers. */
public final class Flag {
    private final Cell c = new Cell(this, 8);

    /** A flag that starts out false. */
    public Flag() {
    }

    /** A flag with a starting value. */
    public Flag(boolean value) {
        set(value);
    }

    long ptr() {
        return c.p;
    }

    /** The current value. */
    public boolean get() {
        return N.getInt(c.p) != 0;
    }

    /** Sets the value. */
    public void set(boolean v) {
        N.putInt(c.p, v ? 1 : 0);
    }

    /** Flips the value. */
    public void toggle() {
        set(!get());
    }

    @Override
    public String toString() {
        return "Flag(" + get() + ")";
    }
}
