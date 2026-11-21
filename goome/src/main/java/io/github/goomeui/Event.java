package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.charset.StandardCharsets;

/**
 * Something that happened, from Goome.poll() or built by hand for Goome.push() and Ui.feed(). Check type()
 * against the Goome event constants.
 */
public final class Event {
    private static final String[] NAMES = {"none", "down", "up", "motion", "scroll", "key", "hotkey", "control", "monitors", "quit", "close", "focus", "drop", "text", "preedit"};
    private static final Layout L = Layout.EVENT;

    int type;
    double x, y;
    int mon, button, dy, key, mods, id;
    String text = "", msg = "", path, full;

    Event(int type) {
        this.type = type;
    }

    /** An event of just this type. */
    public static Event of(int type) {
        return new Event(type);
    }

    /** A mouse press. Button is 1 left, 2 middle, 3 right. */
    public static Event down(double x, double y, int button) {
        return new Event(Goome.DOWN).at(x, y).button(button);
    }

    /** A mouse release. */
    public static Event up(double x, double y, int button) {
        return new Event(Goome.UP).at(x, y).button(button);
    }

    /** A mouse move. */
    public static Event motion(double x, double y) {
        return new Event(Goome.MOTION).at(x, y);
    }

    /** A scroll, dy in notches, +1 is down. */
    public static Event scroll(double x, double y, int dy) {
        Event e = new Event(Goome.SCROLL).at(x, y);
        e.dy = dy;
        return e;
    }

    /**
     * A key press. Key is a lowercase ascii letter or a Goome K_ code, mods are SHIFT and friends or-ed,
     * text is what it typed.
     */
    public static Event key(int key, int mods, String text) {
        Event e = new Event(Goome.KEY);
        e.key = key;
        e.mods = mods;
        e.text = text == null ? "" : text;
        return e;
    }

    /** Finished ime or compose text. */
    public static Event typed(String text) {
        Event e = new Event(Goome.TEXT).msg(text);
        e.full = e.msg;
        return e;
    }

    /** Text that is still being composed, caret is a byte offset into it. */
    public static Event preedit(String text, int caret) {
        Event e = new Event(Goome.PREEDIT).msg(text).id(caret);
        e.full = e.msg;
        return e;
    }

    /** A focus change. */
    public static Event focus(boolean gained) {
        return new Event(Goome.FOCUS).id(gained ? 1 : 0);
    }

    /** Sets the position, logical. */
    public Event at(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Sets the mouse button. */
    public Event button(int b) {
        button = b;
        return this;
    }

    /** Sets the modifier bits. */
    public Event mods(int m) {
        mods = m;
        return this;
    }

    /** Sets the monitor. */
    public Event mon(int m) {
        mon = m;
        return this;
    }

    /** Sets the id, a hotkey id or the focus flag. */
    public Event id(int i) {
        id = i;
        return this;
    }

    /** Sets the message text. */
    public Event msg(String m) {
        msg = m == null ? "" : m;
        return this;
    }

    /** The event type, one of the Goome event constants. */
    public int type() {
        return type;
    }

    /** The type as a word, like down or key. */
    public String name() {
        return type >= 0 && type < NAMES.length ? NAMES[type] : String.valueOf(type);
    }

    /** Logical x of the mouse. */
    public double x() {
        return x;
    }

    /** Logical y of the mouse. */
    public double y() {
        return y;
    }

    /** Which monitor. */
    public int mon() {
        return mon;
    }

    /** Mouse button, 1 left, 2 middle, 3 right. */
    public int button() {
        return button;
    }

    /** Scroll notches, +1 is down. */
    public int dy() {
        return dy;
    }

    /** Lowercase ascii or a Goome K_ code. */
    public int key() {
        return key;
    }

    /** The SHIFT, CTRL, ALT and SUPER bits held. */
    public int mods() {
        return mods;
    }

    /** What the key typed in utf-8, can be empty. */
    public String text() {
        return text;
    }

    /** Hotkey id for HOTKEY, 1 or 0 for FOCUS, caret offset for PREEDIT. */
    public int id() {
        return id;
    }

    /**
     * The control line for CONTROL, or the text for TEXT, PREEDIT and DROP (cut at 63 bytes, see fullText()
     * and path()).
     */
    public String msg() {
        return msg;
    }

    /** The whole text of a TEXT or PREEDIT event, null for other types. */
    public String fullText() {
        if (type != Goome.TEXT && type != Goome.PREEDIT) return null;
        return full != null ? full : msg;
    }

    /** Byte offset of the caret in a PREEDIT, 0 otherwise. */
    public int caret() {
        return type == Goome.PREEDIT ? id : 0;
    }

    /** Full path of a dropped file for DROP, null for other types. */
    public String path() {
        return path != null ? path : type == Goome.DROP ? msg : null;
    }

    static Event read(long p) {
        Event e = new Event(N.getInt(p + L.at("type")));
        e.x = N.getDouble(p + L.at("x"));
        e.y = N.getDouble(p + L.at("y"));
        e.mon = N.getInt(p + L.at("mon"));
        e.button = N.getInt(p + L.at("button"));
        e.dy = N.getInt(p + L.at("dy"));
        e.key = N.getInt(p + L.at("key"));
        e.mods = N.getInt(p + L.at("mods"));
        e.text = Native.chars(p + L.at("text"), 8);
        e.id = N.getInt(p + L.at("id"));
        e.msg = Native.chars(p + L.at("msg"), 64);
        return e;
    }

    void write(long p) {
        N.zero(p, L.size);
        N.putInt(p + L.at("type"), type);
        N.putDouble(p + L.at("x"), x);
        N.putDouble(p + L.at("y"), y);
        N.putInt(p + L.at("mon"), mon);
        N.putInt(p + L.at("button"), button);
        N.putInt(p + L.at("dy"), dy);
        N.putInt(p + L.at("key"), key);
        N.putInt(p + L.at("mods"), mods);
        Native.put(p + L.at("text"), cut(text, 8));
        N.putInt(p + L.at("id"), id);
        Native.put(p + L.at("msg"), cut(msg, 64));
    }

    static byte[] cut(String s, int size) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        if (b.length <= size - 1) return b;
        int n = size - 1;
        while (n > 0 && (b[n] & 0xC0) == 0x80) n--;
        return java.util.Arrays.copyOf(b, n);
    }

    @Override
    public String toString() {
        String more;
        switch (type) {
            case Goome.DOWN:
            case Goome.UP: more = String.format(" %.0f,%.0f button %d", x, y, button); break;
            case Goome.MOTION: more = String.format(" %.0f,%.0f", x, y); break;
            case Goome.SCROLL: more = String.format(" %.0f,%.0f dy %d", x, y, dy); break;
            case Goome.KEY: more = String.format(" key %d mods %#x text '%s'", key, mods, text); break;
            case Goome.HOTKEY: more = " id " + id; break;
            case Goome.CONTROL: more = " '" + msg + "'"; break;
            case Goome.FOCUS: more = id != 0 ? " gained" : " lost"; break;
            case Goome.TEXT: more = " '" + fullText() + "'"; break;
            case Goome.PREEDIT: more = " '" + fullText() + "' caret " + id; break;
            case Goome.DROP: more = String.format(" %.0f,%.0f '%s'", x, y, path()); break;
            default: more = "";
        }
        return "Event(" + name() + more + ")";
    }
}
