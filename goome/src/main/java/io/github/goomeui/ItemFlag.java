package io.github.goomeui;

import java.util.Collection;

/** Options for Ui.custom(). */
public enum ItemFlag {
    /** Tracks dragging along x. On its own it locks the drag to that axis. */
    DRAG_X(1),
    /** Tracks dragging along y. On its own it locks the drag to that axis. */
    DRAG_Y(2),
    /** Tab and focus can reach the widget. */
    FOCUSABLE(4),
    /** The widget ignores the mouse, no hover and no clicks. */
    NO_HIT(8),
    /** Holding the mouse down repeats the click after the config repeat delay. */
    REPEAT(16);

    /** The bit this flag stands for. */
    public final int bit;

    ItemFlag(int bit) {
        this.bit = bit;
    }

    static int bits(Collection<ItemFlag> flags) {
        int v = 0;
        if (flags != null) for (ItemFlag f : flags) v |= f.bit;
        return v;
    }
}
