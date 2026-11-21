package io.github.goomeui;

import static io.github.goomeui.Native.N;
import static io.github.goomeui.Native.b;

import java.util.ArrayList;
import java.util.List;

/** A truetype or opentype font. Use it with Canvas.text() or hand it to the theme. Close it when you are done. */
public final class Font implements AutoCloseable {
    /** Paragraph directions for textDir. */
    public static final int DIR_AUTO = 0, DIR_LTR = 1, DIR_RTL = 2;

    private long f;
    private final List<Font> fallbacks = new ArrayList<>();
    private long keep;
    private final boolean owned;

    Font(long f, long keep, boolean owned) {
        this.f = f;
        this.keep = keep;
        this.owned = owned;
    }

    /** Loads a .ttf, .otf or .ttc file. */
    public static Font load(String path) {
        try (Scratch s = Scratch.push()) {
            return made(Native.call(Fn.FONT_LOAD, s.str(path)), 0, "cant load the font " + path);
        }
    }

    /** Loads one face of a .ttc by index. */
    public static Font load(String path, int index) {
        try (Scratch s = Scratch.push()) {
            return made(Native.call(Fn.FONT_LOAD_INDEX, s.str(path), index), 0, "cant load the font " + path);
        }
    }

    /** Loads a font from memory. The bytes are kept as they are, dont change them while the font lives. */
    public static Font fromBytes(byte[] data) {
        long mem = N.calloc(data.length);
        Native.put(mem, data);
        long h = Native.call(Fn.FONT_LOAD_MEM, mem, data.length, 0);
        if (h == 0) N.free(mem);
        return made(h, mem, "thats not a font goome can read");
    }

    /** Loads a font from memory and copies the bytes first. */
    public static Font fromBytesCopy(byte[] data) {
        try (Scratch s = Scratch.push()) {
            long mem = s.bytes(data);
            return made(Native.call(Fn.FONT_LOAD_MEM, mem, data.length, 1), 0, "thats not a font goome can read");
        }
    }

    /**
     * Finds an installed font by name. null as the name gives the desktops usual ui font. Returns null if
     * nothing matches.
     */
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

    /** True once close() ran. */
    public boolean closed() {
        return f == 0;
    }

    /** Width and height of text at this size. */
    public TextSize measure(String text, double size) {
        return measure(this, text, size);
    }

    /** Width of text at this size. */
    public double width(String text, double size) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.TEXT_MEASURE, handle(), b(size), s.str(text), 0);
        }
    }

    /** Height of one line at this size. */
    public double lineHeight(double size) {
        return lineHeight(this, size);
    }

    /** Like measure, a null font means the built in one. */
    public static TextSize measure(Font font, String text, double size) {
        try (Scratch s = Scratch.push()) {
            long h = s.alloc(8);
            double w = Native.d(Fn.TEXT_MEASURE, handle(font), b(size), s.str(text), h);
            return new TextSize(w, N.getDouble(h));
        }
    }

    /** Like lineHeight, a null font means the built in one. */
    public static double lineHeight(Font font, double size) {
        return Native.d(Fn.TEXT_LINE_HEIGHT, handle(font), b(size));
    }

    /**
     * Adds a font that is tried for characters this one doesnt have, in order. It doesnt free it, you keep
     * it open.
     */
    public Font fallback(Font other) {
        if (other == null) throw new IllegalArgumentException("fallback needs a font");
        Native.call(Fn.FONT_FALLBACK, handle(), other.handle());
        if (other != this && !fallbacks.contains(other)) fallbacks.add(other);
        return this;
    }

    /** The fallback fonts added so far. */
    public List<Font> fallbacks() {
        return List.copyOf(fallbacks);
    }

    /** Paragraph direction for all text, AUTO picks from the first strong letter of each line. */
    public static void textDir(int dir) {
        if (dir < DIR_AUTO || dir > DIR_RTL) throw new IllegalArgumentException("text direction is DIR_AUTO, DIR_LTR or DIR_RTL, not " + dir);
        Native.call(Fn.TEXT_DIR, dir);
    }

    /** Picks which built in font the plain text calls use. */
    public static void setBuiltin(int font) {
        Native.call(Fn.SET_FONT, font);
    }

    /** How many built in fonts there are. */
    public static int builtinCount() {
        return Native.i(Fn.FONT_COUNT);
    }

    /** Frees the font. Close it after the ui that uses it. */
    @Override
    public void close() {
        if (f == 0) return;
        Theme.forget(this);
        long h = f;
        f = 0;
        if (owned) Native.call(Fn.FONT_FREE, h);
        if (keep != 0) N.free(keep);
        keep = 0;
        fallbacks.clear();
    }

    @Override
    public String toString() {
        return f == 0 ? "Font(closed)" : "Font(" + Long.toHexString(f) + ")";
    }
}
