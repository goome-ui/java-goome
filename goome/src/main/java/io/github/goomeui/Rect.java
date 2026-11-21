package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** A rectangle with int fields. */
public final class Rect {
    /** Position and size. */
    public final int x, y, w, h;

    /** Makes a rect. */
    public Rect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** Makes a rect from doubles, the values are cut to whole numbers. */
    public static Rect of(double x, double y, double w, double h) {
        return new Rect((int) x, (int) y, (int) w, (int) h);
    }

    /** True if it has no area. */
    public boolean empty() {
        return w <= 0 || h <= 0;
    }

    /** True if the point is inside. */
    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /** The smallest rect that covers both. */
    public Rect union(Rect o) {
        if (o == null || o.empty()) return this;
        if (empty()) return o;
        int x0 = Math.min(x, o.x), y0 = Math.min(y, o.y);
        return new Rect(x0, y0, Math.max(x + w, o.x + o.w) - x0, Math.max(y + h, o.y + o.h) - y0);
    }

    static Rect read(long p) {
        return new Rect(N.getInt(p), N.getInt(p + 4), N.getInt(p + 8), N.getInt(p + 12));
    }

    static void write(long p, Rect r) {
        N.putInt(p, r.x);
        N.putInt(p + 4, r.y);
        N.putInt(p + 8, r.w);
        N.putInt(p + 12, r.h);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rect)) return false;
        Rect r = (Rect) o;
        return r.x == x && r.y == y && r.w == w && r.h == h;
    }

    @Override
    public int hashCode() {
        return ((x * 31 + y) * 31 + w) * 31 + h;
    }

    @Override
    public String toString() {
        return "Rect(" + x + ", " + y + ", " + w + "x" + h + ")";
    }
}
