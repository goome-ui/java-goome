package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.charset.StandardCharsets;

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

    public static Event of(int type) {
        return new Event(type);
    }

    public static Event down(double x, double y, int button) {
        return new Event(Goome.DOWN).at(x, y).button(button);
    }

    public static Event up(double x, double y, int button) {
        return new Event(Goome.UP).at(x, y).button(button);
    }

    public static Event motion(double x, double y) {
        return new Event(Goome.MOTION).at(x, y);
    }

    public static Event scroll(double x, double y, int dy) {
        Event e = new Event(Goome.SCROLL).at(x, y);
        e.dy = dy;
        return e;
    }

    public static Event key(int key, int mods, String text) {
        Event e = new Event(Goome.KEY);
        e.key = key;
        e.mods = mods;
        e.text = text == null ? "" : text;
        return e;
    }

    public static Event typed(String text) {
        Event e = new Event(Goome.TEXT).msg(text);
        e.full = e.msg;
        return e;
    }

    public static Event preedit(String text, int caret) {
        Event e = new Event(Goome.PREEDIT).msg(text).id(caret);
        e.full = e.msg;
        return e;
    }

    public static Event focus(boolean gained) {
        return new Event(Goome.FOCUS).id(gained ? 1 : 0);
    }

    public Event at(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Event button(int b) {
        button = b;
        return this;
    }

    public Event mods(int m) {
        mods = m;
        return this;
    }

    public Event mon(int m) {
        mon = m;
        return this;
    }

    public Event id(int i) {
        id = i;
        return this;
    }

    public Event msg(String m) {
        msg = m == null ? "" : m;
        return this;
    }

    public int type() {
        return type;
    }

    public String name() {
        return type >= 0 && type < NAMES.length ? NAMES[type] : String.valueOf(type);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public int mon() {
        return mon;
    }

    public int button() {
        return button;
    }

    public int dy() {
        return dy;
    }

    public int key() {
        return key;
    }

    public int mods() {
        return mods;
    }

    public String text() {
        return text;
    }

    public int id() {
        return id;
    }

    public String msg() {
        return msg;
    }

    public String fullText() {
        if (type != Goome.TEXT && type != Goome.PREEDIT) return null;
        return full != null ? full : msg;
    }

    public int caret() {
        return type == Goome.PREEDIT ? id : 0;
    }

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
