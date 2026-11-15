package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.IntBuffer;

public final class Bitmap implements AutoCloseable {
    private static final Layout L = Layout.BITMAP;
    private long p;
    private long ownPx;

    private Bitmap() {
        p = N.calloc(L.size);
    }

    private static Bitmap loaded(int rc, Bitmap b, String what) {
        if (rc != Goome.OK) {
            String why = Native.str(Native.call(Fn.BITMAP_ERROR));
            b.close();
            throw GoomeException.of(rc, "cant load " + what + ": " + (why != null ? why : "unknown"));
        }
        return b;
    }

    public static Bitmap load(String path) {
        Bitmap b = new Bitmap();
        try (Scratch s = Scratch.push()) {
            return loaded(Native.i(Fn.BITMAP_LOAD, s.str(path), b.p), b, path);
        }
    }

    public static Bitmap fromBytes(byte[] data) {
        Bitmap b = new Bitmap();
        long mem = N.calloc(data.length);
        try {
            Native.put(mem, data);
            return loaded(Native.i(Fn.BITMAP_LOAD_MEM, mem, data.length, b.p), b, "the image bytes");
        } finally {
            N.free(mem);
        }
    }

    public static Bitmap fromArgb(int[] argb, int w, int h) {
        if (w < 0 || h < 0 || argb.length < w * h) throw new IllegalArgumentException("need " + w * h + " pixels, got " + argb.length);
        Bitmap b = new Bitmap();
        try (Scratch s = Scratch.push()) {
            long px = s.alloc(4L * Math.max(1, w * h));
            Native.view(px, 4L * w * h).asIntBuffer().put(argb, 0, w * h);
            return loaded(Native.i(Fn.BITMAP_FROM_ARGB, px, w, h, b.p), b, "the pixels");
        }
    }

    public static Bitmap wrap(int[] premul, int w, int h, int stride) {
        int n = h > 0 ? (h - 1) * stride + w : 0;
        if (w < 0 || h < 0 || stride < w || premul.length < n) throw new IllegalArgumentException("need " + n + " pixels for " + w + "x" + h + " stride " + stride);
        Bitmap b = new Bitmap();
        b.ownPx = N.calloc(4L * Math.max(1, h * stride));
        Native.view(b.ownPx, 4L * n).asIntBuffer().put(premul, 0, n);
        N.putLong(b.p + L.at("px"), b.ownPx);
        N.putInt(b.p + L.at("w"), w);
        N.putInt(b.p + L.at("h"), h);
        N.putInt(b.p + L.at("stride"), stride);
        return b;
    }

    long ptr() {
        if (p == 0) throw new GoomeException.Gone("this bitmap is closed");
        return p;
    }

    public boolean closed() {
        return p == 0;
    }

    public int w() {
        return N.getInt(ptr() + L.at("w"));
    }

    public int h() {
        return N.getInt(ptr() + L.at("h"));
    }

    public int stride() {
        return N.getInt(ptr() + L.at("stride"));
    }

    public boolean owned() {
        return N.getInt(ptr() + L.at("owned")) != 0;
    }

    public IntBuffer pixels() {
        long q = ptr();
        long px = N.getLong(q + L.at("px"));
        int n = h() * stride();
        if (px == 0 || n <= 0) return IntBuffer.allocate(0);
        return Native.view(px, 4L * n).asIntBuffer();
    }

    @Override
    public void close() {
        if (p == 0) return;
        Theme.forget(this);
        if (N.getInt(p + L.at("owned")) != 0) Native.call(Fn.BITMAP_FREE, p);
        if (ownPx != 0) N.free(ownPx);
        ownPx = 0;
        N.free(p);
        p = 0;
    }

    @Override
    public String toString() {
        return p == 0 ? "Bitmap(closed)" : "Bitmap(" + w() + "x" + h() + ")";
    }
}
