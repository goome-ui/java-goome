package io.github.goomeui;

import static io.github.goomeui.Native.N;
import static io.github.goomeui.Native.b;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A block of premultiplied argb pixels you draw on. You get one from begin(), or make your own with
 * create(). Coordinates are logical, the canvas scales them, and colors are plain 0xAARRGGBB.
 */
public final class Canvas implements AutoCloseable {
    private static final Layout L = Layout.CANVAS;
    private long p;
    private final boolean owned;
    final List<Rect> hits = new ArrayList<>();

    Canvas(long p, boolean owned) {
        this.p = p;
        this.owned = owned;
    }

    /** Makes a canvas you own, at scale 1. Close it when you are done. */
    public static Canvas create(int w, int h) {
        return create(w, h, w, 1.0);
    }

    /** Makes a canvas you own. Stride is in pixels, scale is real px per logical px. */
    public static Canvas create(int w, int h, int stride, double scale) {
        if (w < 0 || h < 0 || stride < w) throw new IllegalArgumentException("bad canvas size " + w + "x" + h + " stride " + stride);
        long c = N.calloc(L.size);
        long px = N.calloc(4L * Math.max(1, h * stride));
        N.putLong(c + L.at("px"), px);
        N.putInt(c + L.at("w"), w);
        N.putInt(c + L.at("h"), h);
        N.putInt(c + L.at("stride"), stride);
        N.putDouble(c + L.at("scale"), scale);
        return new Canvas(c, true);
    }

    long ptr() {
        if (p == 0) throw new GoomeException.Gone("this canvas got handed back with end(), get a new one from begin()");
        return p;
    }

    long take() {
        long q = ptr();
        if (!owned) p = 0;
        return q;
    }

    void drop() {
        if (!owned) p = 0;
    }

    /** False once it was handed back with end() or closed. */
    public boolean valid() {
        return p != 0;
    }

    /** Frees a canvas made with create(). Canvases from begin() are not yours to close. */
    @Override
    public void close() {
        if (!owned || p == 0) return;
        N.free(N.getLong(p + L.at("px")));
        N.free(p);
        p = 0;
    }

    private int geti(String f) {
        return N.getInt(ptr() + L.at(f));
    }

    /** Width in real px. */
    public int w() {
        return geti("w");
    }

    /** Height in real px. */
    public int h() {
        return geti("h");
    }

    /** Row length in pixels. */
    public int stride() {
        return geti("stride");
    }

    /** Which monitor it is for. */
    public int mon() {
        return geti("mon");
    }

    /** Real px per logical px. */
    public double scale() {
        return N.getDouble(ptr() + L.at("scale"));
    }

    /** Logical x of the first pixel. */
    public double ox() {
        return N.getDouble(ptr() + L.at("ox"));
    }

    /** Logical y of the first pixel. */
    public double oy() {
        return N.getDouble(ptr() + L.at("oy"));
    }

    /** What was drawn since the last clear, in real px. */
    public Rect drawn() {
        return new Rect(geti("dx0"), geti("dy0"), geti("dx1") - geti("dx0"), geti("dy1") - geti("dy0"));
    }

    /** What the last clear wiped, in real px. */
    public Rect cleared() {
        return new Rect(geti("cx0"), geti("cy0"), geti("cx1") - geti("cx0"), geti("cy1") - geti("cy0"));
    }

    /** The current clip in real px, drawing never leaves it. */
    public Rect clipRect() {
        return new Rect(geti("kx0"), geti("ky0"), geti("kx1") - geti("kx0"), geti("ky1") - geti("ky0"));
    }

    /** How many clips are pushed. */
    public int clipDepth() {
        return geti("nclip");
    }

    void raw(String f, int v) {
        N.putInt(ptr() + L.at(f), v);
    }

    /** The pixels as a view on the native memory, premultiplied argb. */
    public IntBuffer pixels() {
        long q = ptr();
        long px = N.getLong(q + L.at("px"));
        int n = geti("h") * geti("stride");
        if (px == 0 || n <= 0) return IntBuffer.allocate(0);
        return Native.view(px, 4L * n).asIntBuffer();
    }

    /** One pixel in real px coords. */
    public int pixel(int x, int y) {
        return pixels().get(y * stride() + x);
    }

    /** Adds a rect where the overlay catches the mouse, used when the frame is sent with Goome.end(). */
    public void hit(double x, double y, double w, double h) {
        ptr();
        hits.add(Rect.of(x, y, w, h));
    }

    /** Marks a real px rect as changed. */
    public void damage(Rect r) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DAMAGE, ptr(), s.rect(r));
        }
    }

    /** Wipes the canvas to transparent. */
    public void clear() {
        Native.call(Fn.CLEAR, ptr());
    }

    /** Filled rectangle. */
    public void rect(double x, double y, double w, double h, int argb) {
        Native.call(Fn.DRAW_RECT, ptr(), b(x), b(y), b(w), b(h), argb);
    }

    /** Filled rectangle with rounded corners. */
    public void roundRect(double x, double y, double w, double h, double radius, int argb) {
        Native.call(Fn.DRAW_ROUND_RECT, ptr(), b(x), b(y), b(w), b(h), b(radius), argb);
    }

    /** Rectangle outline. */
    public void frame(double x, double y, double w, double h, double width, int argb) {
        Native.call(Fn.DRAW_FRAME, ptr(), b(x), b(y), b(w), b(h), b(width), argb);
    }

    /** A line of this thickness. */
    public void line(double x0, double y0, double x1, double y1, double width, int argb) {
        Native.call(Fn.DRAW_LINE, ptr(), b(x0), b(y0), b(x1), b(y1), b(width), argb);
    }

    /** Filled triangle. */
    public void triangle(double x0, double y0, double x1, double y1, double x2, double y2, int argb) {
        Native.call(Fn.DRAW_TRIANGLE, ptr(), b(x0), b(y0), b(x1), b(y1), b(x2), b(y2), argb);
    }

    /** Draws raw argb pixels of size iw by ih stretched into the rect. */
    public void image(double x, double y, double w, double h, int[] argb, int iw, int ih) {
        if (argb.length < iw * ih) throw new IllegalArgumentException("need " + iw * ih + " pixels, got " + argb.length);
        try (Scratch s = Scratch.push()) {
            long px = s.alloc(4L * iw * ih);
            Native.view(px, 4L * iw * ih).asIntBuffer().put(argb, 0, iw * ih);
            Native.call(Fn.DRAW_IMAGE, ptr(), b(x), b(y), b(w), b(h), px, iw, ih);
        }
    }

    /** Makes a rect transparent again. */
    public void erase(double x, double y, double w, double h) {
        Native.call(Fn.ERASE, ptr(), b(x), b(y), b(w), b(h));
    }

    /** Draws text with the built in font. Returns the width. */
    public double text(double x, double y, String s, int argb) {
        return text(x, y, s, argb, false);
    }

    /** Same, but big picks the larger built in size. */
    public double text(double x, double y, String text, int argb, boolean big) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.DRAW_TEXT, ptr(), b(x), b(y), s.str(text), b(big), argb);
        }
    }

    /** Width of text in the built in font. */
    public double textWidth(String text) {
        return textWidth(text, false);
    }

    /** Width of text in the built in font, big for the larger size. */
    public double textWidth(String text, boolean big) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.TEXT_WIDTH, ptr(), s.str(text), b(big));
        }
    }

    /** Draws text with a Font at this size. y is the top, a null font means the built in one. Returns the width. */
    public double text(Font font, double size, double x, double y, String text, int argb) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.TEXT, ptr(), Font.handle(font), b(size), b(x), b(y), s.str(text), argb);
        }
    }

    /** Draws another canvas onto this one with this opacity at an offset. */
    public void blit(Canvas src, double alpha, double dx, double dy) {
        Native.call(Fn.CANVAS_BLIT, ptr(), src.ptr(), b(alpha), b(dx), b(dy));
    }

    /** Limits drawing to a rect. Snaps to whole px, 16 deep, begin() resets it. Pop it with clipPop(). */
    public void clipPush(double x, double y, double w, double h) {
        Native.call(Fn.CLIP_PUSH, ptr(), b(x), b(y), b(w), b(h));
    }

    /** Undoes the last clipPush. */
    public void clipPop() {
        Native.call(Fn.CLIP_POP, ptr());
    }

    /** clipPush as a Clip you close in try with resources. */
    public Clip clip(double x, double y, double w, double h) {
        clipPush(x, y, w, h);
        return new Clip(this);
    }

    /** A pushed clip. Closing it pops the clip. */
    public static final class Clip implements AutoCloseable {
        private Canvas c;

        Clip(Canvas c) {
            this.c = c;
        }

        /** Pops the clip, does nothing the second time. */
        @Override
        public void close() {
            if (c != null && c.valid()) c.clipPop();
            c = null;
        }
    }

    /** Filled rounded rectangle. */
    public void rrect(double x, double y, double w, double h, double radius, int argb) {
        rrect(x, y, w, h, Radii.of(radius), argb);
    }

    /** Filled rectangle with a radius per corner. */
    public void rrect(double x, double y, double w, double h, Radii r, int argb) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_RRECT, ptr(), b(x), b(y), b(w), b(h), s.radii(r), argb);
        }
    }

    /** Outline of a rounded rectangle. */
    public void rrectLine(double x, double y, double w, double h, double radius, double width, int argb) {
        rrectLine(x, y, w, h, Radii.of(radius), width, argb);
    }

    /** Outline with a radius per corner. */
    public void rrectLine(double x, double y, double w, double h, Radii r, double width, int argb) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_RRECT_LINE, ptr(), b(x), b(y), b(w), b(h), s.radii(r), b(width), argb);
        }
    }

    /** Rounded rectangle with a top to bottom gradient. */
    public void rrectGrad(double x, double y, double w, double h, double radius, int top, int bottom) {
        rrectGrad(x, y, w, h, Radii.of(radius), top, bottom, false);
    }

    /** Gradient rectangle with a radius per corner. sideways goes left to right instead of top to bottom. */
    public void rrectGrad(double x, double y, double w, double h, Radii r, int from, int to, boolean sideways) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_RRECT_GRAD, ptr(), b(x), b(y), b(w), b(h), s.radii(r), from, to, b(sideways));
        }
    }

    /** A soft shadow for a rounded rect, blur works like css box-shadow. */
    public void shadow(double x, double y, double w, double h, double radius, double blur, int argb) {
        Native.call(Fn.DRAW_SHADOW, ptr(), b(x), b(y), b(w), b(h), b(radius), b(blur), argb);
    }

    /** Filled circle. */
    public void circle(double cx, double cy, double r, int argb) {
        Native.call(Fn.DRAW_CIRCLE, ptr(), b(cx), b(cy), b(r), argb);
    }

    /** Circle outline. r is the middle of the stroke. */
    public void ring(double cx, double cy, double r, double width, int argb) {
        Native.call(Fn.DRAW_RING, ptr(), b(cx), b(cy), b(r), b(width), argb);
    }

    /** An arc stroke. Angles are radians, 0 is 3 oclock and it goes clockwise. Ends are round. */
    public void arc(double cx, double cy, double r, double width, double a0, double a1, int argb) {
        Native.call(Fn.DRAW_ARC, ptr(), b(cx), b(cy), b(r), b(width), b(a0), b(a1), argb);
    }

    /** Filled polygon from x,y pairs, nonzero fill. Throws if the count is odd. */
    public void poly(double[] xy, int argb) {
        if (xy.length % 2 != 0) throw new IllegalArgumentException("poly wants x,y pairs, got an odd number of values");
        try (Scratch s = Scratch.push()) {
            long pts = s.alloc(8L * Math.max(1, xy.length));
            for (int k = 0; k < xy.length; k++) N.putDouble(pts + 8L * k, xy[k]);
            Native.call(Fn.DRAW_POLY, ptr(), pts, xy.length / 2, argb);
        }
    }

    /** Draws a bitmap stretched into the rect. */
    public void bitmap(Bitmap bm, double x, double y, double w, double h) {
        bitmap(bm, x, y, w, h, Radii.of(0), 0xFFFFFFFF);
    }

    /** Draws a bitmap with rounded corners and a tint color. */
    public void bitmap(Bitmap bm, double x, double y, double w, double h, double radius, int tint) {
        bitmap(bm, x, y, w, h, Radii.of(radius), tint);
    }

    /** Draws a bitmap with a radius per corner and a tint color. */
    public void bitmap(Bitmap bm, double x, double y, double w, double h, Radii r, int tint) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_BITMAP, ptr(), bm.ptr(), b(x), b(y), b(w), b(h), s.radii(r), tint);
        }
    }

    /** Draws part of a bitmap, the source rect is in bitmap px. */
    public void bitmapPart(Bitmap bm, int sx, int sy, int sw, int sh, double x, double y, double w, double h, int tint) {
        Native.call(Fn.DRAW_BITMAP_PART, ptr(), bm.ptr(), sx, sy, sw, sh, b(x), b(y), b(w), b(h), tint);
    }

    /** Draws a bitmap as a nine slice with the same border on every side, in bitmap px. */
    public void bitmapNine(Bitmap bm, double x, double y, double w, double h, double border) {
        bitmapNine(bm, x, y, w, h, border, border, border, border);
    }

    /** Nine slice with a border size per side, in bitmap px. */
    public void bitmapNine(Bitmap bm, double x, double y, double w, double h, double l, double t, double r, double bottom) {
        Native.call(Fn.DRAW_BITMAP_NINE, ptr(), bm.ptr(), b(x), b(y), b(w), b(h), b(l), b(t), b(r), b(bottom));
    }

    @Override
    public String toString() {
        return p == 0 ? "Canvas(done)" : "Canvas(" + w() + "x" + h() + " scale " + scale() + ")";
    }
}
