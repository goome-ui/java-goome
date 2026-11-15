package io.github.goomeui;

public final class Sort {
    public final boolean changed;
    public final int col;
    public final boolean desc;

    Sort(boolean changed, int col, boolean desc) {
        this.changed = changed;
        this.col = col;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "Sort(col " + col + (desc ? " desc" : "") + (changed ? ", changed" : "") + ")";
    }
}
