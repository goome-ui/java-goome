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

/**
 * The immediate mode ui kit. Call begin(), run your widgets every frame, then end(). Widgets that hold a
 * value take it and return the new one, changed() says if the last widget changed it.
 */
public final class Ui implements AutoCloseable {
    /**
     * Flags for window(), or them together. Move by the title, resize from the corner, collapse from the
     * title, no title bar, close button.
     */
    public static final int WIN_MOVE = 1, WIN_RESIZE = 2, WIN_COLLAPSE = 4, WIN_NO_TITLE = 8, WIN_CLOSE = 16;
    /** Flags for app(): title bar, menu bar, status bar, or no padding. */
    public static final int APP_TITLEBAR = 1, APP_MENUBAR = 2, APP_STATUSBAR = 4, APP_NO_PAD = 8;
    /** Flags for textEdit(): read only, line numbers, wrapping, monospace font. */
    public static final int EDIT_READONLY = 1, EDIT_LINE_NUMBERS = 2, EDIT_WRAP = 4, EDIT_MONO = 8;
    /** Flags for tableBegin(): sortable columns, resizable columns, striped rows, borders. */
    public static final int TABLE_SORTABLE = 1, TABLE_RESIZABLE = 2, TABLE_STRIPES = 4, TABLE_BORDERS = 8;
    /** Skin slots for pushSkin() and Theme.skin(int, Skin). SKIN_COUNT is how many there are. */
    public static final int SKIN_BUTTON = 0, SKIN_PANEL = 1, SKIN_INPUT = 2, SKIN_CHECKBOX = 3, SKIN_RADIO = 4, SKIN_TOGGLE = 5, SKIN_SLIDER_TRACK = 6,
        SKIN_SLIDER_FILL = 7, SKIN_SCROLLBAR = 8, SKIN_TAB = 9, SKIN_TITLEBAR = 10, SKIN_TOOLTIP = 11, SKIN_MENU = 12, SKIN_POPUP = 13, SKIN_COUNT = 14;
    /**
     * Flags for inputEx(): password (draws the mask per letter, copy and cut do nothing), read only,
     * numbers only, dont select all on focus, and enter makes entered() true.
     */
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

    /** The Goome this ui belongs to. */
    public Goome goome() {
        return g;
    }

    /** The theme the ui draws with. Change a field and the next frame uses it. */
    public Theme theme() {
        return theme;
    }

    /** Timings, key map and other behavior that isnt a look. */
    public Config config() {
        return config;
    }

    /** Multiplies the sizes in the theme by s. */
    public void scale(double s) {
        Native.call(Fn.UI_SCALE, h(), b(s));
    }

    /** True once close() ran. */
    public boolean closed() {
        return u == 0;
    }

    /** Frees the ui. Closing the Goome does this for you. */
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

    /** True if the last widget changed its value or was clicked. */
    public boolean changed() {
        return changed;
    }

    /** True if the last inputEx() was confirmed with enter, needs INPUT_ENTER_RETURNS. */
    public boolean entered() {
        return entered;
    }

    private boolean changed(long rc) {
        changed = rc != 0;
        return changed;
    }

    /** Gives the ui an event. Returns true if it used it. */
    public boolean feed(Event e) {
        try (Scratch s = Scratch.push()) {
            long p = s.alloc(Layout.EVENT.size);
            e.write(p);
            return Native.i(Fn.UI_FEED, h(), p) != 0;
        }
    }

    /** Starts a ui frame on a monitor. Returns null when there is nothing to draw on, skip the frame then. */
    public Canvas begin(int mon) {
        long p = Native.call(Fn.UI_BEGIN, h(), mon);
        g.began++;
        if (canvas != null) canvas.drop();
        canvas = p == 0 ? null : new Canvas(p, false);
        return canvas;
    }

    /** Finishes the frame and sends it, with every widgets hit area. */
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

    /** begin, body, end in one go. Skips the frame if begin gave nothing. */
    public void frame(int mon, Consumer<Canvas> body) {
        Canvas c = begin(mon);
        if (c == null) return;
        try {
            body.accept(c);
        } finally {
            end();
        }
    }

    /** Adds a rect that catches the mouse in overlay mode, for things you drew yourself. */
    public void hit(double x, double y, double w, double h) {
        Native.call(Fn.UI_HIT, h(), b(x), b(y), b(w), b(h));
    }

    /** True while something still moves, draw again soon. */
    public boolean animating() {
        return Native.i(Fn.UI_ANIMATING, h()) != 0;
    }

    /** Runs t (0 to 1) through one of the Theme EASE_ curves. */
    public static double ease(int ease, double t) {
        return Native.d(Fn.EASE, ease, b(t));
    }

    /** Eases a number toward target over ms. The value is kept per id between frames, call it every frame. */
    public double anim(String id, double target, double ms, int ease) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.UI_ANIM, h(), s.str(id), b(target), b(ms), ease);
        }
    }

    /** Same as anim with a target of 1 for on and 0 for off. */
    public double anim(String id, boolean on, double ms, int ease) {
        return anim(id, on ? 1 : 0, ms, ease);
    }

    /** Eases an argb color toward target, kept per id like anim. */
    public int animColor(String id, int target, double ms, int ease) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_ANIM_COLOR, h(), s.str(id), target, b(ms), ease);
        }
    }

    /** Turns on the accessibility tree. */
    public void a11y(boolean on) {
        Native.call(Fn.UI_A11Y, h(), b(on));
    }

    /** Sets the role and label of the next widget, one of the A11yNode ROLE_ numbers. */
    public void a11yRole(int role, String label) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_A11Y_ROLE, h(), role, s.str(label));
        }
    }

    /** Sets the accessibility value of the next widget. */
    public void a11yValue(String value) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_A11Y_VALUE, h(), s.str(value));
        }
    }

    /** The accessibility tree of the last frame, empty unless a11y is on. */
    public List<A11yNode> a11yTree() {
        int n = Native.i(Fn.A11Y_TREE, h(), 0, 0);
        List<A11yNode> out = new ArrayList<>(n);
        if (n <= 0) return out;
        int size = Layout.A11Y_NODE.size;
        try (Scratch s = Scratch.push()) {
            long arr = s.alloc((long) size * n);
            int got = Math.min(n, Native.i(Fn.A11Y_TREE, h(), arr, n));
            for (int k = 0; k < got; k++) out.add(A11yNode.read(arr + (long) k * size));
        }
        return out;
    }

    /** Sends an accessibility action (press, set, expand or focus) for a node. It goes in as a CONTROL event. */
    public void a11yAct(String action, A11yNode node, String text) {
        String msg = "a11y " + action + " " + node.id() + (text != null ? " " + text : "");
        g.push(Event.of(Goome.CONTROL).msg(msg));
    }

    /** Presses the node like a click. */
    public void a11yPress(A11yNode node) {
        a11yAct("press", node, null);
    }

    /** Sets the value of the node. */
    public void a11ySet(A11yNode node, String value) {
        a11yAct("set", node, value);
    }

    /** Expands the node. */
    public void a11yExpand(A11yNode node) {
        a11yAct("expand", node, null);
    }

    /** Focuses the node. */
    public void a11yFocus(A11yNode node) {
        a11yAct("focus", node, null);
    }

    /** What the ui wants right now as Goome WANT_ bits. Good after feed() and end(). */
    public int wants() {
        return Native.i(Fn.UI_WANTS, h());
    }

    /** True if the ui wants the mouse. */
    public boolean wantsMouse() {
        return (wants() & Goome.WANT_MOUSE) != 0;
    }

    /** True if the ui wants the keyboard. */
    public boolean wantsKeyboard() {
        return (wants() & Goome.WANT_KEYBOARD) != 0;
    }

    /** What the ui takes from the os by itself, WANT_ bits. */
    public int autoGrab() {
        return autoGrab;
    }

    /** Sets what the ui takes from the os by itself. Default is both, 0 means the app does it. */
    public void autoGrab(int wants) {
        Native.call(Fn.UI_AUTO_GRAB, h(), wants);
        autoGrab = wants;
    }

    /** Starts a floating window. Returns true if it is open and not collapsed, then call windowEnd() either way. */
    public boolean window(String title, Window win, int flags) {
        long p = win.x0();
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_WINDOW, h(), s.str(title), p, p + 8, p + 16, p + 24, flags, win.open0()) != 0;
        }
    }

    /** A window that runs body when it is open and calls windowEnd() for you. */
    public void window(String title, Window win, int flags, Runnable body) {
        try {
            if (window(title, win, flags)) body.run();
        } finally {
            windowEnd();
        }
    }

    /** Ends a window(). */
    public void windowEnd() {
        Native.call(Fn.UI_WINDOW_END, h());
    }

    /** Lays the next widgets out in this many equal columns. */
    public void row(int cols) {
        Native.call(Fn.UI_ROW, h(), cols);
    }

    /** Puts the next widget on the same line. */
    public void sameLine() {
        Native.call(Fn.UI_SAME_LINE, h());
    }

    /** Adds empty space. */
    public void space(double px) {
        Native.call(Fn.UI_SPACE, h(), b(px));
    }

    /** Draws a thin line. */
    public void separator() {
        Native.call(Fn.UI_SEPARATOR, h());
    }

    /** Width of the next widget, 0 fills. */
    public void width(double w) {
        Native.call(Fn.UI_WIDTH, h(), b(w));
    }

    /** Plain text. */
    public void label(String text) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_LABEL, h(), s.str("%s"), s.str(text));
        }
    }

    /** Text through String.format. */
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

    /** Bigger text. */
    public void heading(String text) {
        text(Fn.UI_HEADING, text);
    }

    /** Text that wraps at the edge of the window. */
    public void wrapped(String text) {
        text(Fn.UI_WRAPPED, text);
    }

    /** A button. Returns true on the frame it is clicked. */
    public boolean button(String label) {
        return changed(textI(Fn.UI_BUTTON, label));
    }

    /** A button that shows a bitmap. The id keeps it apart from other image buttons. */
    public boolean imageButton(String id, Bitmap bm, double w, double h) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_IMAGE_BUTTON, h(), s.str(id), bm.ptr(), b(w), b(h)));
        }
    }

    /** Draws a bitmap at this size, with rounded corners if radius is above 0. */
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

    /** A checkbox. Returns the new value. */
    public boolean checkbox(String label, boolean value) {
        return flag(Fn.UI_CHECKBOX, label, value);
    }

    /** A toggle switch. Returns the new value. */
    public boolean toggle(String label, boolean value) {
        return flag(Fn.UI_TOGGLE, label, value);
    }

    /**
     * A radio button. value is the current choice, option is the one this button stands for, returns the
     * new choice.
     */
    public int radio(String label, int value, int option) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value);
            changed(Native.i(Fn.UI_RADIO, h(), s.str(label), v, option));
            return N.getInt(v);
        }
    }

    /** A slider from lo to hi. Returns the new value. */
    public double slider(String label, double value, double lo, double hi) {
        return slider(label, value, lo, hi, null);
    }

    /** A slider with a printf style format for the number shown, like %.1f. */
    public double slider(String label, double value, double lo, double hi, String fmt) {
        try (Scratch s = Scratch.push()) {
            long v = s.f64(value);
            changed(Native.i(Fn.UI_SLIDER, h(), s.str(label), v, b(lo), b(hi), s.str(fmt)));
            return N.getDouble(v);
        }
    }

    /** A slider for whole numbers. */
    public int sliderInt(String label, int value, int lo, int hi) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value);
            changed(Native.i(Fn.UI_SLIDER_INT, h(), s.str(label), v, lo, hi));
            return N.getInt(v);
        }
    }

    /** A one line text input. cap is the most bytes it holds. Returns the new text. */
    public String input(String label, String text, int cap) {
        try (Scratch s = Scratch.push()) {
            long buf = s.alloc(cap);
            Native.put(buf, Event.cut(text, cap));
            changed(Native.i(Fn.UI_INPUT, h(), s.str(label), buf, cap));
            return Native.chars(buf, cap);
        }
    }

    /** An input with INPUT_ flags and a hint shown while it is empty. */
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

    /** An input that shows the mask instead of the letters. */
    public String password(String label, String text, int cap) {
        return inputEx(label, text, cap, INPUT_PASSWORD, null);
    }

    /** A dropdown. sel is the picked index, returns the new one. */
    public int combo(String label, int sel, String... items) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(sel);
            changed(Native.i(Fn.UI_COMBO, h(), s.str(label), v, s.strs(items), items.length));
            return N.getInt(v);
        }
    }

    /** A row you can click to select. Returns true when it was clicked. */
    public boolean selectable(String label, boolean selected) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_SELECTABLE, h(), s.str(label), b(selected)));
        }
    }

    /** A tab bar. sel is the picked tab, returns the new one. */
    public int tabs(String id, int sel, String... names) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(sel);
            changed(Native.i(Fn.UI_TABS, h(), s.str(id), s.strs(names), names.length, v));
            return N.getInt(v);
        }
    }

    /** A header that folds open and shut, keeps its own state. Returns true while open. */
    public boolean collapsing(String label) {
        changed = false;
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_COLLAPSING, h(), s.str(label), 0) != 0;
        }
    }

    /** A collapsing header whose state is in the Flag, so you can open it from code. */
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

    /** A progress bar, frac goes 0 to 1. */
    public void progress(double frac) {
        progress(frac, null);
    }

    /** A progress bar with text on it. */
    public void progress(double frac, String text) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_PROGRESS, h(), b(frac), s.str(text));
        }
    }

    /** A color picker. Returns the new 0xAARRGGBB. */
    public int color(String label, int argb) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(argb);
            changed(Native.i(Fn.UI_COLOR, h(), s.str(label), v));
            return N.getInt(v);
        }
    }

    /** Tooltip for the widget right before it. */
    public void tooltip(String text) {
        text(Fn.UI_TOOLTIP, text);
    }

    /** True if the widget right before it is hovered. */
    public boolean hovered() {
        return Native.i(Fn.UI_HOVERED, h()) != 0;
    }

    /** Starts a scroll area of this height. Always call scrollEnd(). */
    public boolean scrollBegin(String id, double height) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_SCROLL_BEGIN, h(), s.str(id), b(height)) != 0;
        }
    }

    /** Ends a scrollBegin(). */
    public void scrollEnd() {
        Native.call(Fn.UI_SCROLL_END, h());
    }

    /** A scroll area that runs body and calls scrollEnd() for you. */
    public void scroll(String id, double height, Runnable body) {
        try {
            if (scrollBegin(id, height)) body.run();
        } finally {
            scrollEnd();
        }
    }

    /** Pushes an id scope. Use it when the same labels repeat, like in a loop. */
    public void pushId(String id) {
        text(Fn.UI_PUSH_ID, id);
    }

    /** Pushes a number as an id scope, the loop index is the usual pick. */
    public void pushId(int id) {
        pushId(Integer.toString(id));
    }

    /** Pops the last pushId. */
    public void popId() {
        Native.call(Fn.UI_POP_ID, h());
    }

    /** Starts the root panel that fills monitor 0, call it after begin(). Finish with appEnd(). */
    public boolean app(String title, int flags) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_APP, h(), s.str(title), flags) != 0;
        }
    }

    /** Ends app(). */
    public void appEnd() {
        Native.call(Fn.UI_APP_END, h());
    }

    /** Starts the menu bar, needs APP_MENUBAR. Returns true if there is one to fill. */
    public boolean menubarBegin() {
        return Native.i(Fn.UI_MENUBAR_BEGIN, h()) != 0;
    }

    /** Ends menubarBegin(). */
    public void menubarEnd() {
        Native.call(Fn.UI_MENUBAR_END, h());
    }

    /** A menu. Returns true while open, then call menuEnd(). Nest them for submenus. */
    public boolean menuBegin(String label) {
        return textI(Fn.UI_MENU_BEGIN, label) != 0;
    }

    /** Ends menuBegin(). */
    public void menuEnd() {
        Native.call(Fn.UI_MENU_END, h());
    }

    /** A menu entry. Returns true when clicked. */
    public boolean menuItem(String label) {
        return menuItem(label, null, null);
    }

    /** A menu entry with a shortcut shown on the right. */
    public boolean menuItem(String label, String shortcut) {
        return menuItem(label, shortcut, null);
    }

    /** A menu entry with a check mark kept in the Flag. The checked flag can be null. */
    public boolean menuItem(String label, String shortcut, Flag checked) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_MENU_ITEM, h(), s.str(label), s.str(shortcut), checked == null ? 0 : checked.ptr()));
        }
    }

    /** A line between menu entries. */
    public void menuSeparator() {
        Native.call(Fn.UI_MENU_SEPARATOR, h());
    }

    /**
     * A right click menu for the widget before it, or empty space. Returns true while open, then call
     * contextEnd().
     */
    public boolean contextBegin(String id) {
        return textI(Fn.UI_CONTEXT_BEGIN, id) != 0;
    }

    /** Ends contextBegin(). */
    public void contextEnd() {
        Native.call(Fn.UI_CONTEXT_END, h());
    }

    /** Opens a popup by id. */
    public void popupOpen(String id) {
        text(Fn.UI_POPUP_OPEN, id);
    }

    /** Fills a popup. Returns true while it is open, then call popupEnd(). */
    public boolean popupBegin(String id) {
        return textI(Fn.UI_POPUP_BEGIN, id) != 0;
    }

    /** Ends popupBegin(). */
    public void popupEnd() {
        Native.call(Fn.UI_POPUP_END, h());
    }

    /** Closes the innermost open popup, menu or modal. */
    public void popupClose() {
        Native.call(Fn.UI_POPUP_CLOSE, h());
    }

    /** A modal dialog, centered, dims the rest, esc closes it. Returns true while open, then call modalEnd(). */
    public boolean modalBegin(String title, Flag open) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_MODAL_BEGIN, h(), s.str(title), open == null ? 0 : open.ptr()) != 0;
        }
    }

    /** Ends modalBegin(). */
    public void modalEnd() {
        Native.call(Fn.UI_MODAL_END, h());
    }

    /** Shows a short message for ms, 0 uses the config toast time. */
    public void toast(String text, int ms) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_TOAST, h(), s.str(text), ms);
        }
    }

    /** Text for the status bar, needs APP_STATUSBAR. */
    public void statusbar(String text) {
        text(Fn.UI_STATUSBAR, text);
    }

    /** True if the widget right before it has keyboard focus. */
    public boolean focused() {
        return Native.i(Fn.UI_FOCUSED, h()) != 0;
    }

    /** Gives focus to the next widget. */
    public void setFocus() {
        Native.call(Fn.UI_SET_FOCUS, h());
    }

    /** A multiline editor on the buffer, EDIT_ flags. Returns true if the text changed. */
    public boolean textEdit(String id, TextBuffer buf, double height, int flags) {
        try (Scratch s = Scratch.push()) {
            return changed(Native.i(Fn.UI_TEXT_EDIT, h(), s.str(id), buf.ptr(), buf.capacity(), b(height), flags));
        }
    }

    /** Starts a table, TABLE_ flags. Add the columns, then rows with tableRow() and tableNext(), then tableEnd(). */
    public boolean tableBegin(String id, int cols, double height, int flags) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_TABLE_BEGIN, h(), s.str(id), cols, b(height), flags) != 0;
        }
    }

    /** Adds a column, width 0 shares the rest. */
    public void tableColumn(String name, double width) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_TABLE_COLUMN, h(), s.str(name), b(width));
        }
    }

    /** Call after the columns. Says if the sort changed this frame and to what, pass the current col and desc. */
    public Sort tableSort(int col, boolean desc) {
        try (Scratch s = Scratch.push()) {
            long c = s.i32(col), d = s.i32(desc ? 1 : 0);
            boolean r = Native.i(Fn.UI_TABLE_SORT, h(), c, d) != 0;
            return new Sort(r, N.getInt(c), N.getInt(d) != 0);
        }
    }

    /** Starts a new row. */
    public void tableRow() {
        Native.call(Fn.UI_TABLE_ROW, h());
    }

    /** Moves to the next cell. */
    public void tableNext() {
        Native.call(Fn.UI_TABLE_NEXT, h());
    }

    /** Ends the table. */
    public void tableEnd() {
        Native.call(Fn.UI_TABLE_END, h());
    }

    /** A tree node. Returns true while open, then call treeEnd(). */
    public boolean tree(String label, Flag open) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_TREE, h(), s.str(label), open == null ? 0 : open.ptr()) != 0;
        }
    }

    /** Ends an open tree node. */
    public void treeEnd() {
        Native.call(Fn.UI_TREE_END, h());
    }

    /** For long lists in a scroll area. Returns {first, last}, only draw those rows. */
    public int[] listClip(int count, double rowH) {
        try (Scratch s = Scratch.push()) {
            long a = s.i32(0), z = s.i32(0);
            Native.call(Fn.UI_LIST_CLIP, h(), count, b(rowH), a, z);
            return new int[] {N.getInt(a), N.getInt(z)};
        }
    }

    /** A number you change by dragging it. Returns the new value. */
    public double dragFloat(String label, double value, double speed, double lo, double hi, String fmt) {
        try (Scratch s = Scratch.push()) {
            long v = s.f64(value);
            changed(Native.i(Fn.UI_DRAG_FLOAT, h(), s.str(label), v, b(speed), b(lo), b(hi), s.str(fmt)));
            return N.getDouble(v);
        }
    }

    /** A whole number you change by dragging it. */
    public int dragInt(String label, int value, double speed, int lo, int hi) {
        try (Scratch s = Scratch.push()) {
            long v = s.i32(value);
            changed(Native.i(Fn.UI_DRAG_INT, h(), s.str(label), v, b(speed), lo, hi));
            return N.getInt(v);
        }
    }

    /** A number box with plus and minus buttons that move by step. */
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

    /** A line plot, the range comes from the values. */
    public void plotLines(String label, float[] values, double height) {
        double[] r = range(values);
        plot(Fn.UI_PLOT_LINES, label, values, r[0], r[1], height);
    }

    /** A line plot with a fixed range. */
    public void plotLines(String label, float[] values, double lo, double hi, double height) {
        plot(Fn.UI_PLOT_LINES, label, values, lo, hi, height);
    }

    /** A bar plot, the range comes from the values. */
    public void plotBars(String label, float[] values, double height) {
        double[] r = range(values);
        plot(Fn.UI_PLOT_BARS, label, values, r[0], r[1], height);
    }

    /** A bar plot with a fixed range. */
    public void plotBars(String label, float[] values, double lo, double hi, double height) {
        plot(Fn.UI_PLOT_BARS, label, values, lo, hi, height);
    }

    /**
     * Starts a resizable two pane split. Fill the first pane, call splitNext(), fill the second, then
     * splitEnd().
     */
    public boolean splitBegin(String id, Split split) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_SPLIT_BEGIN, h(), s.str(id), b(split.vertical), split.ptr()) != 0;
        }
    }

    /** Switches to the second pane. */
    public void splitNext() {
        Native.call(Fn.UI_SPLIT_NEXT, h());
    }

    /** Ends the split. */
    public void splitEnd() {
        Native.call(Fn.UI_SPLIT_END, h());
    }

    /** Makes the widget before it draggable, carrying text. Returns true while it is dragged. */
    public boolean dragSource(String type, String data) {
        return dragSource(type, data.getBytes(StandardCharsets.UTF_8));
    }

    /** Makes the widget before it draggable, carrying bytes. */
    public boolean dragSource(String type, byte[] data) {
        try (Scratch s = Scratch.push()) {
            return Native.i(Fn.UI_DRAG_SOURCE, h(), s.str(type), s.bytes(data), data.length) != 0;
        }
    }

    /**
     * Makes the widget before it accept a drop of that type. Returns the dropped bytes, or null if nothing
     * landed.
     */
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

    /** A loading spinner, 0 uses the theme size. */
    public void spinner(double size) {
        Native.call(Fn.UI_SPINNER, h(), b(size));
    }

    /** A text link. Returns true when clicked. */
    public boolean link(String text) {
        return changed(textI(Fn.UI_LINK, text));
    }

    /** A small colored tag in the theme badge color. */
    public void badge(String text) {
        badge(text, theme.accent());
    }

    /** A small tag in this color. */
    public void badge(String text, int argb) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_BADGE, h(), s.str(text), argb);
        }
    }

    /** Puts the next widget at x,y from the content top left (scroll included), the flow doesnt move. */
    public void pos(double x, double y) {
        Native.call(Fn.UI_POS, h(), b(x), b(y));
    }

    /** Size of the next widget, 0 means its own size for that side. */
    public void size(double w, double h) {
        Native.call(Fn.UI_SIZE, h(), b(w), b(h));
    }

    /** Changes a theme color until it is popped. Throws if the Style is a number. */
    public void pushColor(Style what, int argb) {
        if (!what.color()) throw new IllegalArgumentException(what + " isnt a color, use pushNum");
        Native.call(Fn.UI_PUSH_COLOR, h(), what.ordinal(), argb);
    }

    /** Changes a theme number until it is popped. Throws if the Style is a color. */
    public void pushNum(Style what, double v) {
        if (what.color()) throw new IllegalArgumentException(what + " is a color, use pushColor");
        Native.call(Fn.UI_PUSH_NUM, h(), what.ordinal(), b(v));
    }

    /** Per corner radii for the next widgets, beats the RADIUS style until popped. */
    public void pushRadii(Radii r) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.UI_PUSH_RADII, h(), s.radii(r));
        }
    }

    /** Per corner radii, top left, top right, bottom right, bottom left. */
    public void pushRadii(double tl, double tr, double br, double bl) {
        pushRadii(new Radii(tl, tr, br, bl));
    }

    /** Uses this font until popped, null is the built in one. */
    public void pushFont(Font f) {
        Native.call(Fn.UI_PUSH_FONT, h(), Font.handle(f));
        if (f != null) kept.add(f);
    }

    /** Uses a nine slice skin for a SKIN_ slot until popped, null means no skin. */
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

    /** Swaps in a whole theme with one push. */
    public void pushTheme(Theme t) {
        Native.call(Fn.UI_PUSH_THEME, h(), t.ptr());
        kept.add(t);
    }

    /** Uses your own bitmap for an icon until popped. */
    public void pushIcon(Icon which, Bitmap bm) {
        Native.call(Fn.UI_PUSH_ICON, h(), which.ordinal(), bm == null ? 0 : bm.ptr());
        if (bm != null) kept.add(bm);
    }

    /** Pops the last push, of any kind. */
    public void pop() {
        pop(1);
    }

    /** Pops the last n pushes, of any kind. */
    public void pop(int n) {
        Native.call(Fn.UI_POP, h(), n);
    }

    /** Starts a chained set of pushes, use it in try with resources so they pop by themselves. */
    public Styled style() {
        return new Styled(this);
    }

    /** A custom widget of this size, draw it on canvas(). The Item says what the mouse did. */
    public Item custom(String id, double w, double h) {
        return customBits(id, w, h, 0);
    }

    /** A custom widget with ItemFlag options. */
    public Item custom(String id, double w, double h, ItemFlag... flags) {
        return customBits(id, w, h, ItemFlag.bits(Arrays.asList(flags)));
    }

    /** A custom widget with ItemFlag options as a set. */
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

    /** A canvas to draw on right where the ui is, clipped like the widgets. null if there is none. */
    public Canvas canvas() {
        long p = Native.call(Fn.UI_CANVAS, h());
        if (p == 0) return null;
        Canvas c = new Canvas(p, false);
        layers.add(c);
        return c;
    }

    /** n doubles kept per id between frames, zero the first time. */
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
