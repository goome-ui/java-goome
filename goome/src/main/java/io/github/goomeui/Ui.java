package io.github.goomeui;

import static io.github.goomeui.Native.N;
import static io.github.goomeui.Native.b;

import java.nio.DoubleBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class Ui implements AutoCloseable {
    public static final int WIN_MOVE = 1, WIN_RESIZE = 2, WIN_COLLAPSE = 4, WIN_NO_TITLE = 8, WIN_CLOSE = 16;
    public static final int APP_TITLEBAR = 1, APP_MENUBAR = 2, APP_STATUSBAR = 4, APP_NO_PAD = 8;
    public static final int EDIT_READONLY = 1, EDIT_LINE_NUMBERS = 2, EDIT_WRAP = 4, EDIT_MONO = 8;
    public static final int TABLE_SORTABLE = 1, TABLE_RESIZABLE = 2, TABLE_STRIPES = 4, TABLE_BORDERS = 8;
    public static final int SKIN_BUTTON = 0, SKIN_PANEL = 1, SKIN_INPUT = 2, SKIN_CHECKBOX = 3, SKIN_RADIO = 4, SKIN_TOGGLE = 5, SKIN_SLIDER_TRACK = 6,
        SKIN_SLIDER_FILL = 7, SKIN_SCROLLBAR = 8, SKIN_TAB = 9, SKIN_TITLEBAR = 10, SKIN_TOOLTIP = 11, SKIN_MENU = 12, SKIN_POPUP = 13, SKIN_COUNT = 14;
    public static final int INPUT_PASSWORD = 1, INPUT_READONLY = 2, INPUT_NUMBER = 4, INPUT_NO_SELECT_ON_FOCUS = 8, INPUT_ENTER_RETURNS = 16;

    private final Goome g;
    private long u;
    private final Theme theme;
    private final Config config;
    private Canvas canvas;
    private final List<Canvas> layers = new ArrayList<>();
    private final List<Object> kept = new ArrayList<>();
    private boolean changed, entered;
    private int autoGrab = Goome.WANT_MOUSE | Goome.WANT_KEYBOARD;

    Ui(Goome g) {
        this.g = g;
        u = Native.call(Fn.UI_NEW, g.h());
        if (u == 0) throw new GoomeException.NoMemory("no memory for the ui");
        g.remember(this);
        theme = new Theme(this);
        config = new Config(this);
    }

    long h() {
        if (u == 0) throw new GoomeException.Gone("this ui is closed");
        return u;
    }

    public Goome goome() {
        return g;
    }

    public Theme theme() {
        return theme;
    }

    public Config config() {
        return config;
    }

    public void scale(double s) {
        Native.call(Fn.UI_SCALE, h(), b(s));
    }

    public boolean closed() {
        return u == 0;
    }

    @Override
    public void close() {
        if (u == 0) return;
        theme.detach();
        if (canvas != null) canvas.drop();
        canvas = null;
        dropLayers();
        long h = u;
        u = 0;
        g.forget(this);
        Native.call(Fn.UI_FREE, h);
    }

    public boolean changed() {
        return changed;
    }

    public boolean entered() {
        return entered;
    }

    private boolean changed(long rc) {
        changed = rc != 0;
        return changed;
    }

    public boolean feed(Event e) {
        try (Scratch s = Scratch.push()) {
            long p = s.alloc(Layout.EVENT.size);
            e.write(p);
            return Native.i(Fn.UI_FEED, h(), p) != 0;
        }
    }

    public Canvas begin(int mon) {
        long p = Native.call(Fn.UI_BEGIN, h(), mon);
        g.began++;
        if (canvas != null) canvas.drop();
        canvas = p == 0 ? null : new Canvas(p, false);
        return canvas;
    }

    public void end() {
        Native.call(Fn.UI_END, h());
        g.ended++;
        if (canvas != null) canvas.drop();
        canvas = null;
        dropLayers();
    }

    private void dropLayers() {
        for (Canvas c : layers) c.drop();
        layers.clear();
        kept.clear();
    }

    public void frame(int mon, Consumer<Canvas> body) {
        Canvas c = begin(mon);
        if (c == null) return;
        try {
            body.accept(c);
        } finally {
            end();
        }
    }

    public void hit(double x, double y, double w, double h) {
        Native.call(Fn.UI_HIT, h(), b(x), b(y), b(w), b(h));
    }

    public boolean animating() {
        return Native.i(Fn.UI_ANIMATING, h()) != 0;
    }

    public int wants() {
        return Native.i(Fn.UI_WANTS, h());
    }

    public boolean wantsMouse() {
        return (wants() & Goome.WANT_MOUSE) != 0;
    }

    public boolean wantsKeyboard() {
        return (wants() & Goome.WANT_KEYBOARD) != 0;
    }

    public int autoGrab() {
        return autoGrab;
    }

    public void autoGrab(int wants) {
        Native.call(Fn.UI_AUTO_GRAB, h(), wants);
        autoGrab = wants;
    }

    public boolean window(String title, Window win, int flags) {
        long p = win.x0();
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_WINDOW, h(), s.str(title), p, p + 8, p + 16, p + 24, flags, win.open0()) != 0;
        }
    }

    public void window(String title, Window win, int flags, Runnable body) {
        try {
            if (window(title, win, flags)) body.run();
        } finally {
            windowEnd();
        }
    }

    public void windowEnd() {
        Native.call(Fn.UI_WINDOW_END, h());
    }

    public void row(int cols) {
        Native.call(Fn.UI_ROW, h(), cols);
    }

    public void sameLine() {
        Native.call(Fn.UI_SAME_LINE, h());
    }

    public void space(double px) {
        Native.call(Fn.UI_SPACE, h(), b(px));
    }

    public void separator() {
        Native.call(Fn.UI_SEPARATOR, h());
    }

    public void width(double w) {
        Native.call(Fn.UI_WIDTH, h(), b(w));
    }

    public void label(String text) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_LABEL, h(), s.str("%s"), s.str(text));
        }
    }

    public void label(String fmt, Object... args) {
        label(String.format(fmt, args));
    }

    private void text(Fn f, String text) {
        try (Scratch s = Scratch.push()) {
            Native.call(f, h(), s.str(text));
        }
    }

    private int textI(Fn f, String text) {
        try (Scratch s = Scratch.push()) {
            return Native.i(f, h(), s.str(text));
        }
    }

    public void heading(String text) {
        text(Fn.UI_HEADING, text);
    }

    public void wrapped(String text) {
        text(Fn.UI_WRAPPED, text);
    }

    public boolean button(String label) {
        return changed(textI(Fn.UI_BUTTON, label));
    }

    public boolean imageButton(String id, Bitmap bm, double w, double h) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_IMAGE_BUTTON, h(), s.str(id), bm.ptr(), b(w), b(h)));
        }
    }

    public void image(Bitmap bm, double w, double h, double radius) {
        Native.call(Fn.UI_IMAGE, h(), bm.ptr(), b(w), b(h), b(radius));
    }

    private boolean flag(Fn f, String label, boolean value) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value ? 1 : 0);
            changed(Native.i(f, h(), s.str(label), v));
            return N.getInt(v) != 0;
        }
    }

    public boolean checkbox(String label, boolean value) {
        return flag(Fn.UI_CHECKBOX, label, value);
    }

    public boolean toggle(String label, boolean value) {
        return flag(Fn.UI_TOGGLE, label, value);
    }

    public int radio(String label, int value, int option) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value);
            changed(Native.i(Fn.UI_RADIO, h(), s.str(label), v, option));
            return N.getInt(v);
        }
    }

    public double slider(String label, double value, double lo, double hi) {
        return slider(label, value, lo, hi, null);
    }

    public double slider(String label, double value, double lo, double hi, String fmt) {
        try (Scratch s = Scratch.push()) {
            long v = s.f64(value);
            changed(Native.i(Fn.UI_SLIDER, h(), s.str(label), v, b(lo), b(hi), s.str(fmt)));
            return N.getDouble(v);
        }
    }

    public int sliderInt(String label, int value, int lo, int hi) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value);
            changed(Native.i(Fn.UI_SLIDER_INT, h(), s.str(label), v, lo, hi));
            return N.getInt(v);
        }
    }

    public String input(String label, String text, int cap) {
        try (Scratch s = Scratch.push()) {
            long buf = s.alloc(cap);
            Native.put(buf, Event.cut(text, cap));
            changed(Native.i(Fn.UI_INPUT, h(), s.str(label), buf, cap));
            return Native.chars(buf, cap);
        }
    }

    public String inputEx(String label, String text, int cap, int flags, String hint) {
        try (Scratch s = Scratch.push()) {
            long buf = s.alloc(cap);
            Native.put(buf, Event.cut(text, cap));
            int rc = Native.i(Fn.UI_INPUT_EX, h(), s.str(label), buf, cap, flags, s.str(hint));
            changed(rc);
            entered = rc == 2;
            return Native.chars(buf, cap);
        }
    }

    public String password(String label, String text, int cap) {
        return inputEx(label, text, cap, INPUT_PASSWORD, null);
    }

    public int combo(String label, int sel, String... items) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(sel);
            changed(Native.i(Fn.UI_COMBO, h(), s.str(label), v, s.strs(items), items.length));
            return N.getInt(v);
        }
    }

    public boolean selectable(String label, boolean selected) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_SELECTABLE, h(), s.str(label), b(selected)));
        }
    }

    public int tabs(String id, int sel, String... names) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(sel);
            changed(Native.i(Fn.UI_TABS, h(), s.str(id), s.strs(names), names.length, v));
            return N.getInt(v);
        }
    }

    public boolean collapsing(String label) {
        changed = false;
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_COLLAPSING, h(), s.str(label), 0) != 0;
        }
    }

    public boolean collapsing(String label, Flag open) {
        return withFlag(Fn.UI_COLLAPSING, label, open);
    }

    private boolean withFlag(Fn f, String label, Flag flag) {
        try (Scratch s = Scratch.push()) {
            boolean was = flag != null && flag.get();
            boolean r = Native.i(f, h(), s.str(label), flag == null ? 0 : flag.ptr()) != 0;
            changed = flag != null && flag.get() != was;
            return r;
        }
    }

    public void progress(double frac) {
        progress(frac, null);
    }

    public void progress(double frac, String text) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_PROGRESS, h(), b(frac), s.str(text));
        }
    }

    public int color(String label, int argb) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(argb);
            changed(Native.i(Fn.UI_COLOR, h(), s.str(label), v));
            return N.getInt(v);
        }
    }

    public void tooltip(String text) {
        text(Fn.UI_TOOLTIP, text);
    }

    public boolean hovered() {
        return Native.i(Fn.UI_HOVERED, h()) != 0;
    }

    public boolean scrollBegin(String id, double height) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_SCROLL_BEGIN, h(), s.str(id), b(height)) != 0;
        }
    }

    public void scrollEnd() {
        Native.call(Fn.UI_SCROLL_END, h());
    }

    public void scroll(String id, double height, Runnable body) {
        try {
            if (scrollBegin(id, height)) body.run();
        } finally {
            scrollEnd();
        }
    }

    public void pushId(String id) {
        text(Fn.UI_PUSH_ID, id);
    }

    public void pushId(int id) {
        pushId(Integer.toString(id));
    }

    public void popId() {
        Native.call(Fn.UI_POP_ID, h());
    }

    public boolean app(String title, int flags) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_APP, h(), s.str(title), flags) != 0;
        }
    }

    public void appEnd() {
        Native.call(Fn.UI_APP_END, h());
    }

    public boolean menubarBegin() {
        return Native.i(Fn.UI_MENUBAR_BEGIN, h()) != 0;
    }

    public void menubarEnd() {
        Native.call(Fn.UI_MENUBAR_END, h());
    }

    public boolean menuBegin(String label) {
        return textI(Fn.UI_MENU_BEGIN, label) != 0;
    }

    public void menuEnd() {
        Native.call(Fn.UI_MENU_END, h());
    }

    public boolean menuItem(String label) {
        return menuItem(label, null, null);
    }

    public boolean menuItem(String label, String shortcut) {
        return menuItem(label, shortcut, null);
    }

    public boolean menuItem(String label, String shortcut, Flag checked) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_MENU_ITEM, h(), s.str(label), s.str(shortcut), checked == null ? 0 : checked.ptr()));
        }
    }

    public void menuSeparator() {
        Native.call(Fn.UI_MENU_SEPARATOR, h());
    }

    public boolean contextBegin(String id) {
        return textI(Fn.UI_CONTEXT_BEGIN, id) != 0;
    }

    public void contextEnd() {
        Native.call(Fn.UI_CONTEXT_END, h());
    }

    public void popupOpen(String id) {
        text(Fn.UI_POPUP_OPEN, id);
    }

    public boolean popupBegin(String id) {
        return textI(Fn.UI_POPUP_BEGIN, id) != 0;
    }

    public void popupEnd() {
        Native.call(Fn.UI_POPUP_END, h());
    }

    public void popupClose() {
        Native.call(Fn.UI_POPUP_CLOSE, h());
    }

    public boolean modalBegin(String title, Flag open) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_MODAL_BEGIN, h(), s.str(title), open == null ? 0 : open.ptr()) != 0;
        }
    }

    public void modalEnd() {
        Native.call(Fn.UI_MODAL_END, h());
    }

    public void toast(String text, int ms) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_TOAST, h(), s.str(text), ms);
        }
    }

    public void statusbar(String text) {
        text(Fn.UI_STATUSBAR, text);
    }

    public boolean focused() {
        return Native.i(Fn.UI_FOCUSED, h()) != 0;
    }

    public void setFocus() {
        Native.call(Fn.UI_SET_FOCUS, h());
    }

    public boolean textEdit(String id, TextBuffer buf, double height, int flags) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_TEXT_EDIT, h(), s.str(id), buf.ptr(), buf.capacity(), b(height), flags));
        }
    }

    public boolean tableBegin(String id, int cols, double height, int flags) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_TABLE_BEGIN, h(), s.str(id), cols, b(height), flags) != 0;
        }
    }

    public void tableColumn(String name, double width) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_TABLE_COLUMN, h(), s.str(name), b(width));
        }
    }

    public Sort tableSort(int col, boolean desc) {
        try (Scratch s = Scratch.push()) {
            long c = s.i32(col), d = s.i32(desc ? 1 : 0);
            boolean r = Native.i(Fn.UI_TABLE_SORT, h(), c, d) != 0;
            return new Sort(r, N.getInt(c), N.getInt(d) != 0);
        }
    }

    public void tableRow() {
        Native.call(Fn.UI_TABLE_ROW, h());
    }

    public void tableNext() {
        Native.call(Fn.UI_TABLE_NEXT, h());
    }

    public void tableEnd() {
        Native.call(Fn.UI_TABLE_END, h());
    }

    public boolean tree(String label, Flag open) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_TREE, h(), s.str(label), open == null ? 0 : open.ptr()) != 0;
        }
    }

    public void treeEnd() {
        Native.call(Fn.UI_TREE_END, h());
    }

    public int[] listClip(int count, double rowH) {
        try (Scratch s = Scratch.push()) {
            long a = s.i32(0), z = s.i32(0);
            Native.call(Fn.UI_LIST_CLIP, h(), count, b(rowH), a, z);
            return new int[] {N.getInt(a), N.getInt(z)};
        }
    }

    public double dragFloat(String label, double value, double speed, double lo, double hi, String fmt) {
        try (Scratch s = Scratch.push()) {
            long v = s.f64(value);
            changed(Native.i(Fn.UI_DRAG_FLOAT, h(), s.str(label), v, b(speed), b(lo), b(hi), s.str(fmt)));
            return N.getDouble(v);
        }
    }

    public int dragInt(String label, int value, double speed, int lo, int hi) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value);
            changed(Native.i(Fn.UI_DRAG_INT, h(), s.str(label), v, b(speed), lo, hi));
            return N.getInt(v);
        }
    }

    public double number(String label, double value, double step, String fmt) {
        try (Scratch s = Scratch.push()) {
            long v = s.f64(value);
            changed(Native.i(Fn.UI_NUMBER, h(), s.str(label), v, b(step), s.str(fmt)));
            return N.getDouble(v);
        }
    }

    private void plot(Fn f, String label, float[] v, double lo, double hi, double height) {
        try (Scratch s = Scratch.push()) {
            long arr = s.alloc(4L * Math.max(1, v.length));
            for (int k = 0; k < v.length; k++) N.putFloat(arr + 4L * k, v[k]);
            Native.call(f, h(), s.str(label), arr, v.length, b(lo), b(hi), b(height));
        }
    }

    private static double[] range(float[] v) {
        if (v.length == 0) return new double[] {0, 1};
        double lo = v[0], hi = v[0];
        for (float x : v) {
            lo = Math.min(lo, x);
            hi = Math.max(hi, x);
        }
        return new double[] {lo, hi};
    }

    public void plotLines(String label, float[] values, double height) {
        double[] r = range(values);
        plot(Fn.UI_PLOT_LINES, label, values, r[0], r[1], height);
    }

    public void plotLines(String label, float[] values, double lo, double hi, double height) {
        plot(Fn.UI_PLOT_LINES, label, values, lo, hi, height);
    }

    public void plotBars(String label, float[] values, double height) {
        double[] r = range(values);
        plot(Fn.UI_PLOT_BARS, label, values, r[0], r[1], height);
    }

    public void plotBars(String label, float[] values, double lo, double hi, double height) {
        plot(Fn.UI_PLOT_BARS, label, values, lo, hi, height);
    }

    public boolean splitBegin(String id, Split split) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_SPLIT_BEGIN, h(), s.str(id), b(split.vertical), split.ptr()) != 0;
        }
    }

    public void splitNext() {
        Native.call(Fn.UI_SPLIT_NEXT, h());
    }

    public void splitEnd() {
        Native.call(Fn.UI_SPLIT_END, h());
    }

    public boolean dragSource(String type, String data) {
        return dragSource(type, data.getBytes(StandardCharsets.UTF_8));
    }

    public boolean dragSource(String type, byte[] data) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_DRAG_SOURCE, h(), s.str(type), s.bytes(data), data.length) != 0;
        }
    }

    public byte[] dropTarget(String type, int cap) {
        try (Scratch s = Scratch.push()) {
            long buf = s.alloc(cap);
            int n = Native.i(Fn.UI_DROP_TARGET, h(), s.str(type), buf, cap);
            if (n <= 0) return null;
            byte[] out = new byte[Math.min(n, cap)];
            Native.view(buf, out.length).get(out);
            return out;
        }
    }

    public void spinner(double size) {
        Native.call(Fn.UI_SPINNER, h(), b(size));
    }

    public boolean link(String text) {
        return changed(textI(Fn.UI_LINK, text));
    }

    public void badge(String text) {
        badge(text, theme.accent());
    }

    public void badge(String text, int argb) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_BADGE, h(), s.str(text), argb);
        }
    }

    public void pos(double x, double y) {
        Native.call(Fn.UI_POS, h(), b(x), b(y));
    }

    public void size(double w, double h) {
        Native.call(Fn.UI_SIZE, h(), b(w), b(h));
    }

    public void pushColor(Style what, int argb) {
        if (!what.color()) throw new IllegalArgumentException(what + " isnt a color, use pushNum");
        Native.call(Fn.UI_PUSH_COLOR, h(), what.ordinal(), argb);
    }

    public void pushNum(Style what, double v) {
        if (what.color()) throw new IllegalArgumentException(what + " is a color, use pushColor");
        Native.call(Fn.UI_PUSH_NUM, h(), what.ordinal(), b(v));
    }

    public void pushRadii(Radii r) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_PUSH_RADII, h(), s.radii(r));
        }
    }

    public void pushRadii(double tl, double tr, double br, double bl) {
        pushRadii(new Radii(tl, tr, br, bl));
    }

    public void pushFont(Font f) {
        Native.call(Fn.UI_PUSH_FONT, h(), Font.handle(f));
        if (f != null) kept.add(f);
    }

    public void pushSkin(int kind, Skin skin) {
        if (kind < 0 || kind >= SKIN_COUNT) throw new IllegalArgumentException("skin kind is one of the SKIN_ numbers, not " + kind);
        try (Scratch s = Scratch.push()) {
            long q = 0;
            if (skin != null) {
                q = s.alloc(Layout.SKIN.size);
                skin.write(q);
                kept.add(skin);
            }
            Native.call(Fn.UI_PUSH_SKIN, h(), kind, q);
        }
    }

    public void pushTheme(Theme t) {
        Native.call(Fn.UI_PUSH_THEME, h(), t.ptr());
        kept.add(t);
    }

    public void pushIcon(Icon which, Bitmap bm) {
        Native.call(Fn.UI_PUSH_ICON, h(), which.ordinal(), bm == null ? 0 : bm.ptr());
        if (bm != null) kept.add(bm);
    }

    public void pop() {
        pop(1);
    }

    public void pop(int n) {
        Native.call(Fn.UI_POP, h(), n);
    }

    public Styled style() {
        return new Styled(this);
    }

    public Item custom(String id, double w, double h) {
        return customBits(id, w, h, 0);
    }

    public Item custom(String id, double w, double h, ItemFlag... flags) {
        return customBits(id, w, h, ItemFlag.bits(Arrays.asList(flags)));
    }

    public Item custom(String id, double w, double h, Set<ItemFlag> flags) {
        return customBits(id, w, h, ItemFlag.bits(flags));
    }

    private Item customBits(String id, double w, double h, int flags) {
        try (Scratch s = Scratch.push()) {
            long it = s.alloc(Layout.ITEM.size);
            changed(Native.i(Fn.UI_CUSTOM, h(), s.str(id), b(w), b(h), flags, it));
            return Item.read(it);
        }
    }

    public Canvas canvas() {
        long p = Native.call(Fn.UI_CANVAS, h());
        if (p == 0) return null;
        Canvas c = new Canvas(p, false);
        layers.add(c);
        return c;
    }

    public DoubleBuffer store(String id, int n) {
        if (n <= 0) throw new IllegalArgumentException("store needs at least 1 double, not " + n);
        long p;
        try (Scratch s = Scratch.push()) {
            p = Native.call(Fn.UI_STORE, h(), s.str(id), n);
        }
        if (p == 0) throw new GoomeException.NoMemory("no memory for " + n + " stored doubles");
        return Native.view(p, 8L * n).asDoubleBuffer();
    }
}
