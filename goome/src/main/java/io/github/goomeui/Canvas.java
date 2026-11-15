package io.github.goomeui;

import static io.github.goomeui.Native.N;
import static io.github.goomeui.Native.b;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public final class Canvas implements AutoCloseable {
    private static final Layout L = Layout.CANVAS;
    private long p;
    private final boolean owned;
    final List<Rect> hits = new ArrayList<>();

    Canvas(long p, boolean owned) {
        this.p = p;
        this.owned = owned;
    }

    public static Canvas create(int w, int h) {
        return create(w, h, w, 1.0);
    }

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

    public boolean valid() {
        return p != 0;
    }

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

    public int w() {
        return geti("w");
    }

    public int h() {
        return geti("h");
    }

    public int stride() {
        return geti("stride");
    }

    public int mon() {
        return geti("mon");
    }

    public double scale() {
        return N.getDouble(ptr() + L.at("scale"));
    }

    public double ox() {
        return N.getDouble(ptr() + L.at("ox"));
    }

    public double oy() {
        return N.getDouble(ptr() + L.at("oy"));
    }

    public Rect drawn() {
        return new Rect(geti("dx0"), geti("dy0"), geti("dx1") - geti("dx0"), geti("dy1") - geti("dy0"));
    }

    public Rect cleared() {
        return new Rect(geti("cx0"), geti("cy0"), geti("cx1") - geti("cx0"), geti("cy1") - geti("cy0"));
    }

    public Rect clipRect() {
        return new Rect(geti("kx0"), geti("ky0"), geti("kx1") - geti("kx0"), geti("ky1") - geti("ky0"));
    }

    public int clipDepth() {
        return geti("nclip");
    }

    void raw(String f, int v) {
        N.putInt(ptr() + L.at(f), v);
    }

    public IntBuffer pixels() {
        long q = ptr();
        long px = N.getLong(q + L.at("px"));
        int n = geti("h") * geti("stride");
        if (px == 0 || n <= 0) return IntBuffer.allocate(0);
        return Native.view(px, 4L * n).asIntBuffer();
    }

    public int pixel(int x, int y) {
        return pixels().get(y * stride() + x);
    }

    public void hit(double x, double y, double w, double h) {
        ptr();
        hits.add(Rect.of(x, y, w, h));
    }

    public void damage(Rect r) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DAMAGE, ptr(), s.rect(r));
        }
    }

    public void clear() {
        Native.call(Fn.CLEAR, ptr());
    }

    public void rect(double x, double y, double w, double h, int argb) {
        Native.call(Fn.DRAW_RECT, ptr(), b(x), b(y), b(w), b(h), argb);
    }

    public void roundRect(double x, double y, double w, double h, double radius, int argb) {
        Native.call(Fn.DRAW_ROUND_RECT, ptr(), b(x), b(y), b(w), b(h), b(radius), argb);
    }

    public void frame(double x, double y, double w, double h, double width, int argb) {
        Native.call(Fn.DRAW_FRAME, ptr(), b(x), b(y), b(w), b(h), b(width), argb);
    }

    public void line(double x0, double y0, double x1, double y1, double width, int argb) {
        Native.call(Fn.DRAW_LINE, ptr(), b(x0), b(y0), b(x1), b(y1), b(width), argb);
    }

    public void triangle(double x0, double y0, double x1, double y1, double x2, double y2, int argb) {
        Native.call(Fn.DRAW_TRIANGLE, ptr(), b(x0), b(y0), b(x1), b(y1), b(x2), b(y2), argb);
    }

    public void image(double x, double y, double w, double h, int[] argb, int iw, int ih) {
        if (argb.length < iw * ih) throw new IllegalArgumentException("need " + iw * ih + " pixels, got " + argb.length);
        try (Scratch s = Scratch.push()) {
            long px = s.alloc(4L * iw * ih);
            Native.view(px, 4L * iw * ih).asIntBuffer().put(argb, 0, iw * ih);
            Native.call(Fn.DRAW_IMAGE, ptr(), b(x), b(y), b(w), b(h), px, iw, ih);
        }
    }

    public void erase(double x, double y, double w, double h) {
        Native.call(Fn.ERASE, ptr(), b(x), b(y), b(w), b(h));
    }

    public double text(double x, double y, String s, int argb) {
        return text(x, y, s, argb, false);
    }

    public double text(double x, double y, String text, int argb, boolean big) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.DRAW_TEXT, ptr(), b(x), b(y), s.str(text), b(big), argb);
        }
    }

    public double textWidth(String text) {
        return textWidth(text, false);
    }

    public double textWidth(String text, boolean big) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.TEXT_WIDTH, ptr(), s.str(text), b(big));
        }
    }

    public double text(Font font, double size, double x, double y, String text, int argb) {
        try (Scratch s = Scratch.push()) {
            return Native.d(Fn.TEXT, ptr(), Font.handle(font), b(size), b(x), b(y), s.str(text), argb);
        }
    }

    public void blit(Canvas src, double alpha, double dx, double dy) {
        Native.call(Fn.CANVAS_BLIT, ptr(), src.ptr(), b(alpha), b(dx), b(dy));
    }

    public void clipPush(double x, double y, double w, double h) {
        Native.call(Fn.CLIP_PUSH, ptr(), b(x), b(y), b(w), b(h));
    }

    public void clipPop() {
        Native.call(Fn.CLIP_POP, ptr());
    }

    public Clip clip(double x, double y, double w, double h) {
        clipPush(x, y, w, h);
        return new Clip(this);
    }

    public static final class Clip implements AutoCloseable {
        private Canvas c;

        Clip(Canvas c) {
            this.c = c;
        }

        @Override
        public void close() {
            if (c != null && c.valid()) c.clipPop();
            c = null;
        }
    }

    public void rrect(double x, double y, double w, double h, double radius, int argb) {
        rrect(x, y, w, h, Radii.of(radius), argb);
    }

    public void rrect(double x, double y, double w, double h, Radii r, int argb) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_RRECT, ptr(), b(x), b(y), b(w), b(h), s.radii(r), argb);
        }
    }

    public void rrectLine(double x, double y, double w, double h, double radius, double width, int argb) {
        rrectLine(x, y, w, h, Radii.of(radius), width, argb);
    }

    public void rrectLine(double x, double y, double w, double h, Radii r, double width, int argb) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_RRECT_LINE, ptr(), b(x), b(y), b(w), b(h), s.radii(r), b(width), argb);
        }
    }

    public void rrectGrad(double x, double y, double w, double h, double radius, int top, int bottom) {
        rrectGrad(x, y, w, h, Radii.of(radius), top, bottom, false);
    }

    public void rrectGrad(double x, double y, double w, double h, Radii r, int from, int to, boolean sideways) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_RRECT_GRAD, ptr(), b(x), b(y), b(w), b(h), s.radii(r), from, to, b(sideways));
        }
    }

    public void shadow(double x, double y, double w, double h, double radius, double blur, int argb) {
        Native.call(Fn.DRAW_SHADOW, ptr(), b(x), b(y), b(w), b(h), b(radius), b(blur), argb);
    }

    public void circle(double cx, double cy, double r, int argb) {
        Native.call(Fn.DRAW_CIRCLE, ptr(), b(cx), b(cy), b(r), argb);
    }

    public void ring(double cx, double cy, double r, double width, int argb) {
        Native.call(Fn.DRAW_RING, ptr(), b(cx), b(cy), b(r), b(width), argb);
    }

    public void arc(double cx, double cy, double r, double width, double a0, double a1, int argb) {
        Native.call(Fn.DRAW_ARC, ptr(), b(cx), b(cy), b(r), b(width), b(a0), b(a1), argb);
    }

    public void poly(double[] xy, int argb) {
        if (xy.length % 2 != 0) throw new IllegalArgumentException("poly wants x,y pairs, got an odd number of values");
        try (Scratch s = Scratch.push()) {
            long pts = s.alloc(8L * Math.max(1, xy.length));
            for (int k = 0; k < xy.length; k++) N.putDouble(pts + 8L * k, xy[k]);
            Native.call(Fn.DRAW_POLY, ptr(), pts, xy.length / 2, argb);
        }
    }

    public void bitmap(Bitmap bm, double x, double y, double w, double h) {
        bitmap(bm, x, y, w, h, Radii.of(0), 0xFFFFFFFF);
    }

    public void bitmap(Bitmap bm, double x, double y, double w, double h, double radius, int tint) {
        bitmap(bm, x, y, w, h, Radii.of(radius), tint);
    }

    public void bitmap(Bitmap bm, double x, double y, double w, double h, Radii r, int tint) {
        try (Scratch s = Scratch.push()) {
            Native.call(Fn.DRAW_BITMAP, ptr(), bm.ptr(), b(x), b(y), b(w), b(h), s.radii(r), tint);
        }
    }

    public void bitmapPart(Bitmap bm, int sx, int sy, int sw, int sh, double x, double y, double w, double h, int tint) {
        Native.call(Fn.DRAW_BITMAP_PART, ptr(), bm.ptr(), sx, sy, sw, sh, b(x), b(y), b(w), b(h), tint);
    }

    public void bitmapNine(Bitmap bm, double x, double y, double w, double h, double border) {
        bitmapNine(bm, x, y, w, h, border, border, border, border);
    }

    public void bitmapNine(Bitmap bm, double x, double y, double w, double h, double l, double t, double r, double bottom) {
        Native.call(Fn.DRAW_BITMAP_NINE, ptr(), bm.ptr(), b(x), b(y), b(w), b(h), b(l), b(t), b(r), b(bottom));
    }

    @Override
    public String toString() {
        return p == 0 ? "Canvas(done)" : "Canvas(" + w() + "x" + h() + " scale " + scale() + ")";
    }
}
