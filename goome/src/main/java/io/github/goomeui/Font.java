package io.github.goomeui;

import static io.github.goomeui.Native.N;
import static io.github.goomeui.Native.b;

public final class Font implements AutoCloseable {
    private long f;
    private long keep;
    private final boolean owned;

    Font(long f, long keep, boolean owned) {
        this.f = f;
        this.keep = keep;
        this.owned = owned;
    }

    public static Font load(String path) {
        try (Scratch s = Scratch.push()) {
            return made(Native.call(Fn.FONT_LOAD, s.str(path)), 0, "cant load the font " + path);
        }
    }

    public static Font load(String path, int index) {
        try (Scratch s = Scratch.push()) {
            return made(Native.call(Fn.FONT_LOAD_INDEX, s.str(path), index), 0, "cant load the font " + path);
        }
    }

    public static Font fromBytes(byte[] data) {
        long mem = N.calloc(data.length);
        Native.put(mem, data);
        long h = Native.call(Fn.FONT_LOAD_MEM, mem, data.length, 0);
        if (h == 0) N.free(mem);
        return made(h, mem, "thats not a font goome can read");
    }

    public static Font fromBytesCopy(byte[] data) {
        try (Scratch s = Scratch.push()) {
            long mem = s.bytes(data);
            return made(Native.call(Fn.FONT_LOAD_MEM, mem, data.length, 1), 0, "thats not a font goome can read");
        }
    }

    public static Font system(String name) {
        try (Scratch s = Scratch.push()) {
            long h = Native.call(Fn.FONT_SYSTEM, s.str(name));
            return h == 0 ? null : new Font(h, 0, true);
        }
    }

    private static Font made(long h, long keep, String err) {
        if (h == 0) throw new GoomeException.BadArg(err);
        return new Font(h, keep, true);
    }

    static long handle(Font f) {
        return f == null ? 0 : f.handle();
    }

    long handle() {
        if (f == 0) throw new GoomeException.Gone("this font is closed");
        return f;
    }

    long raw() {
        return f;
    }

    public boolean closed() {
        return f == 0;
    }

    public TextSize measure(String text, double size) {
        return measure(this, text, size);
    }

    public double width(String text, double size) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.TEXT_MEASURE, handle(), b(size), s.str(text), 0);
        }
    }

    public double lineHeight(double size) {
        return lineHeight(this, size);
    }

    public static TextSize measure(Font font, String text, double size) {
        try (Scratch s = Scratch.push()) {
            long h = s.alloc(8);
            double w = Native.d(Fn.TEXT_MEASURE, handle(font), b(size), s.str(text), h);
            return new TextSize(w, N.getDouble(h));
        }
    }

    public static double lineHeight(Font font, double size) {
        return Native.d(Fn.TEXT_LINE_HEIGHT, handle(font), b(size));
    }

    public static void setBuiltin(int font) {
        Native.call(Fn.SET_FONT, font);
    }

    public static int builtinCount() {
        return Native.i(Fn.FONT_COUNT);
    }

    @Override
    public void close() {
        if (f == 0) return;
        Theme.forget(this);
        long h = f;
        f = 0;
        if (owned) Native.call(Fn.FONT_FREE, h);
        if (keep != 0) N.free(keep);
        keep = 0;
    }

    @Override
    public String toString() {
        return f == 0 ? "Font(closed)" : "Font(" + Long.toHexString(f) + ")";
    }
}
