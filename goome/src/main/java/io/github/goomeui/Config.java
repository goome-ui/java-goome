package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.charset.StandardCharsets;

public final class Config {
    public static final int ACT_COPY = 0, ACT_CUT = 1, ACT_PASTE = 2, ACT_UNDO = 3, ACT_REDO = 4, ACT_SELECT_ALL = 5, ACT_WORD_LEFT = 6, ACT_WORD_RIGHT = 7,
        ACT_DEL_WORD_LEFT = 8, ACT_DEL_WORD_RIGHT = 9, ACT_DOC_START = 10, ACT_DOC_END = 11, ACT_COUNT = 12;
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

    public int tipDelayMs() { return num("tip_delay_ms"); }
    public Config tipDelayMs(int v) { return num("tip_delay_ms", v); }
    public int dblClickMs() { return num("dbl_click_ms"); }
    public Config dblClickMs(int v) { return num("dbl_click_ms", v); }
    public int dblClickPx() { return num("dbl_click_px"); }
    public Config dblClickPx(int v) { return num("dbl_click_px", v); }
    public int dragPx() { return num("drag_px"); }
    public Config dragPx(int v) { return num("drag_px", v); }
    public int repeatDelayMs() { return num("repeat_delay_ms"); }
    public Config repeatDelayMs(int v) { return num("repeat_delay_ms", v); }
    public int repeatMs() { return num("repeat_ms"); }
    public Config repeatMs(int v) { return num("repeat_ms", v); }
    public int blinkMs() { return num("blink_ms"); }
    public Config blinkMs(int v) { return num("blink_ms", v); }
    public int menuDelayMs() { return num("menu_delay_ms"); }
    public Config menuDelayMs(int v) { return num("menu_delay_ms", v); }
    public int toastMs() { return num("toast_ms"); }
    public Config toastMs(int v) { return num("toast_ms", v); }

    public double scrollLines() { return N.getDouble(p() + L.at("scroll_lines")); }
    public Config scrollLines(double v) { N.putDouble(p() + L.at("scroll_lines"), v); return this; }

    public boolean raiseOnClick() { return num("raise_on_click") != 0; }
    public Config raiseOnClick(boolean v) { return num("raise_on_click", v ? 1 : 0); }
    public boolean escCloses() { return num("esc_closes") != 0; }
    public Config escCloses(boolean v) { return num("esc_closes", v ? 1 : 0); }
    public boolean tabNav() { return num("tab_nav") != 0; }
    public Config tabNav(boolean v) { return num("tab_nav", v ? 1 : 0); }
    public boolean focusOnMouse() { return num("focus_on_mouse") != 0; }
    public Config focusOnMouse(boolean v) { return num("focus_on_mouse", v ? 1 : 0); }

    public int cursor(int kind) {
        return N.getInt(p() + L.at("cursor") + 4L * check(kind, CUR_COUNT, "CUR_"));
    }

    public Config cursor(int kind, int shape) {
        N.putInt(p() + L.at("cursor") + 4L * check(kind, CUR_COUNT, "CUR_"), shape);
        return this;
    }

    public int key(int act, int slot) {
        return N.getInt(combo(act, slot));
    }

    public int mods(int act, int slot) {
        return N.getInt(combo(act, slot) + 4);
    }

    private long combo(int act, int slot) {
        check(act, ACT_COUNT, "ACT_");
        if (slot < 0 || slot >= 4) throw new IllegalArgumentException("each action has 4 key slots, 0 to 3, not " + slot);
        return p() + L.at("keys") + (long) Layout.COMBO.size * (4L * act + slot);
    }

    public boolean bind(int act, String combos) {
        check(act, ACT_COUNT, "ACT_");
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_BIND, ui.h(), act, s.str(combos)) == 0;
        }
    }

    public String mask() {
        return Native.str(N.getLong(p() + L.at("mask")));
    }

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
