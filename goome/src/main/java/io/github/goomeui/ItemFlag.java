package io.github.goomeui;

import java.util.Collection;

public enum ItemFlag {
    DRAG_X(1), DRAG_Y(2), FOCUSABLE(4), NO_HIT(8), REPEAT(16);

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
