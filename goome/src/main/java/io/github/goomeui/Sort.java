package io.github.goomeui;

/** What Ui.tableSort() says about the table sorting. */
public final class Sort {
    /** True if the sort changed this frame. */
    public final boolean changed;
    /** The column sorted by. */
    public final int col;
    /** True for descending. */
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
