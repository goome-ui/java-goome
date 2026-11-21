package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.charset.StandardCharsets;

/**
 * Behavior of a Ui that isnt a look: timings, the key map, cursors and the password mask. One per ui, from
 * Ui.config(). Setters return the Config so you can chain them.
 */
public final class Config {
    /** Editing actions for bind(), key() and mods(). ACT_COUNT is how many there are. */
    public static final int ACT_COPY = 0, ACT_CUT = 1, ACT_PASTE = 2, ACT_UNDO = 3, ACT_REDO = 4, ACT_SELECT_ALL = 5, ACT_WORD_LEFT = 6, ACT_WORD_RIGHT = 7,
        ACT_DEL_WORD_LEFT = 8, ACT_DEL_WORD_RIGHT = 9, ACT_DOC_START = 10, ACT_DOC_END = 11, ACT_COUNT = 12;
    /** Cursor slots for cursor(int), which cursor each kind of widget asks for. CUR_COUNT is how many there are. */
    public static final int CUR_BUTTON = 0, CUR_INPUT = 1, CUR_LINK = 2, CUR_DRAG_VALUE = 3, CUR_SPLIT_X = 4, CUR_SPLIT_Y = 5, CUR_GRIP = 6, CUR_COLUMN = 7,
        CUR_DRAG_DROP = 8, CUR_CUSTOM_X = 9, CUR_CUSTOM_Y = 10, CUR_CUSTOM_XY = 11, CUR_COUNT = 12;
    private static final Layout L = Layout.CONFIG;

    private final Ui ui;
    private Object mask;

    Config(Ui ui) {
        this.ui = ui;
    }

    private long p() {
        return Native.call(Fn.UI_CFG, ui.h());
    }

    private int num(String f) {
        return N.getInt(p() + L.at(f));
    }

    private Config num(String f, int v) {
        N.putInt(p() + L.at(f), v);
        return this;
    }

    /** Returns the hover time in ms before a tooltip shows, 500 by default. */
    public int tipDelayMs() { return num("tip_delay_ms"); }
    /** Sets the hover time in ms before a tooltip shows, 500 by default. Returns this so you can chain. */
    public Config tipDelayMs(int v) { return num("tip_delay_ms", v); }
    /** Returns the longest time in ms between the clicks of a double click, 450 by default. */
    public int dblClickMs() { return num("dbl_click_ms"); }
    /**
     * Sets the longest time in ms between the clicks of a double click, 450 by default. Returns this so you
     * can chain.
     */
    public Config dblClickMs(int v) { return num("dbl_click_ms", v); }
    /** Returns how far in px the mouse can move between the clicks of a double click, 5 by default. */
    public int dblClickPx() { return num("dbl_click_px"); }
    /**
     * Sets how far in px the mouse can move between the clicks of a double click, 5 by default. Returns
     * this so you can chain.
     */
    public Config dblClickPx(int v) { return num("dbl_click_px", v); }
    /** Returns how far in px before a press turns into a drag, 4 by default. */
    public int dragPx() { return num("drag_px"); }
    /** Sets how far in px before a press turns into a drag, 4 by default. Returns this so you can chain. */
    public Config dragPx(int v) { return num("drag_px", v); }
    /** Returns how long in ms a held key or ItemFlag.REPEAT waits before it starts repeating, 380 by default. */
    public int repeatDelayMs() { return num("repeat_delay_ms"); }
    /**
     * Sets how long in ms a held key or ItemFlag.REPEAT waits before it starts repeating, 380 by default.
     * Returns this so you can chain.
     */
    public Config repeatDelayMs(int v) { return num("repeat_delay_ms", v); }
    /** Returns the time in ms between repeats, 55 by default. */
    public int repeatMs() { return num("repeat_ms"); }
    /** Sets the time in ms between repeats, 55 by default. Returns this so you can chain. */
    public Config repeatMs(int v) { return num("repeat_ms", v); }
    /** Returns the caret blink time in ms, 0 means no blink, 530 by default. */
    public int blinkMs() { return num("blink_ms"); }
    /** Sets the caret blink time in ms, 0 means no blink, 530 by default. Returns this so you can chain. */
    public Config blinkMs(int v) { return num("blink_ms", v); }
    /** Returns the hover time in ms before a submenu opens, 120 by default. */
    public int menuDelayMs() { return num("menu_delay_ms"); }
    /** Sets the hover time in ms before a submenu opens, 120 by default. Returns this so you can chain. */
    public Config menuDelayMs(int v) { return num("menu_delay_ms", v); }
    /** Returns how long in ms a toast stays when Ui.toast() gets 0, 3000 by default. */
    public int toastMs() { return num("toast_ms"); }
    /** Sets how long in ms a toast stays when Ui.toast() gets 0, 3000 by default. Returns this so you can chain. */
    public Config toastMs(int v) { return num("toast_ms", v); }

    /** Returns the lines per mouse wheel step, 3 by default. */
    public double scrollLines() { return N.getDouble(p() + L.at("scroll_lines")); }
    /** Sets the lines per mouse wheel step, 3 by default. Returns this so you can chain. */
    public Config scrollLines(double v) { N.putDouble(p() + L.at("scroll_lines"), v); return this; }

    /** Returns whether clicking a window brings it to the front, on by default. */
    public boolean raiseOnClick() { return num("raise_on_click") != 0; }
    /** Sets whether clicking a window brings it to the front, on by default. Returns this so you can chain. */
    public Config raiseOnClick(boolean v) { return num("raise_on_click", v ? 1 : 0); }
    /** Returns whether esc closes the top popup or modal, on by default. */
    public boolean escCloses() { return num("esc_closes") != 0; }
    /** Sets whether esc closes the top popup or modal, on by default. Returns this so you can chain. */
    public Config escCloses(boolean v) { return num("esc_closes", v ? 1 : 0); }
    /** Returns whether tab and shift+tab move focus, on by default. */
    public boolean tabNav() { return num("tab_nav") != 0; }
    /** Sets whether tab and shift+tab move focus, on by default. Returns this so you can chain. */
    public Config tabNav(boolean v) { return num("tab_nav", v ? 1 : 0); }
    /** Returns whether clicking a widget focuses it and shows the focus ring, off by default. */
    public boolean focusOnMouse() { return num("focus_on_mouse") != 0; }
    /**
     * Sets whether clicking a widget focuses it and shows the focus ring, off by default. Returns this so
     * you can chain.
     */
    public Config focusOnMouse(boolean v) { return num("focus_on_mouse", v ? 1 : 0); }

    /** The cursor shape that kind of widget asks for, kind is a CUR_ number. */
    public int cursor(int kind) {
        return N.getInt(p() + L.at("cursor") + 4L * check(kind, CUR_COUNT, "CUR_"));
    }

    /** Sets the cursor shape for a kind of widget, shape is a Goome CURSOR_ number. */
    public Config cursor(int kind, int shape) {
        N.putInt(p() + L.at("cursor") + 4L * check(kind, CUR_COUNT, "CUR_"), shape);
        return this;
    }

    /** The key code bound to an action in one of its 4 slots, 0 if empty. */
    public int key(int act, int slot) {
        return N.getInt(combo(act, slot));
    }

    /** The modifier bits of the binding in that slot. */
    public int mods(int act, int slot) {
        return N.getInt(combo(act, slot) + 4);
    }

    private long combo(int act, int slot) {
        check(act, ACT_COUNT, "ACT_");
        if (slot < 0 || slot >= 4) throw new IllegalArgumentException("each action has 4 key slots, 0 to 3, not " + slot);
        return p() + L.at("keys") + (long) Layout.COMBO.size * (4L * act + slot);
    }

    /**
     * Replaces every key on an action. Up to 4 combos split by spaces or commas, written like hotkeys, null
     * or empty takes them all off. Returns false if none could be read.
     */
    public boolean bind(int act, String combos) {
        check(act, ACT_COUNT, "ACT_");
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_BIND, ui.h(), act, s.str(combos)) == 0;
        }
    }

    /** What a password input draws per letter, null means a star. */
    public String mask() {
        return Native.str(N.getLong(p() + L.at("mask")));
    }

    /** Sets what a password input draws per letter, null goes back to the star. */
    public Config mask(String m) {
        if (m == null) {
            N.putLong(p() + L.at("mask"), 0);
            mask = null;
            return this;
        }
        byte[] b = m.getBytes(StandardCharsets.UTF_8);
        Object keep = new Object();
        Cell c = new Cell(keep, b.length + 1);
        Native.put(c.p, b);
        N.putLong(p() + L.at("mask"), c.p);
        mask = keep;
        return this;
    }

    /** Puts every setting back to its default. */
    public Config reset() {
        Native.call(Fn.UI_CONFIG_DEFAULT, p());
        mask = null;
        return this;
    }

    private static int check(int v, int count, String what) {
        if (v < 0 || v >= count) throw new IllegalArgumentException("thats not one of the " + what + " numbers: " + v);
        return v;
    }
}
