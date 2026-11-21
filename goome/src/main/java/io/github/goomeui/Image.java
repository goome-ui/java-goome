package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.function.BooleanSupplier;

/**
 * A screen grab or a frame from Goome. The pixels live in native memory and are only good until the next
 * grab or frame, copy() them to keep them.
 */
public final class Image {
    private final long px;
    /** Size in real px, stride in pixels. */
    public final int w, h, stride;
    /** Where it sits on the desktop, logical coords. */
    public final Rect area;
    /** Which monitor it came from. */
    public final int mon;
    /**
     * hasSelf means our own overlay is in the grab, mask it with Goome.alphaAt(). hasCursor means the mouse
     * got baked in at cursor.
     */
    public final boolean hasSelf, hasCursor;
    /** Where the mouse got baked in, logical. */
    public final Rect cursor;
    private final BooleanSupplier fresh;
    private final String what;

    Image(long p, BooleanSupplier fresh, String what) {
        Layout l = Layout.IMAGE;
        px = N.getLong(p + l.at("px"));
        w = N.getInt(p + l.at("w"));
        h = N.getInt(p + l.at("h"));
        stride = N.getInt(p + l.at("stride"));
        area = Rect.read(p + l.at("area"));
        mon = N.getInt(p + l.at("mon"));
        hasSelf = N.getInt(p + l.at("has_self")) != 0;
        hasCursor = N.getInt(p + l.at("has_cursor")) != 0;
        cursor = Rect.read(p + l.at("cursor"));
        this.fresh = fresh;
        this.what = what;
    }

    /** False once a newer grab replaced this one. */
    public boolean fresh() {
        return fresh.getAsBoolean();
    }

    private void check() {
        if (!fresh()) throw new GoomeException.Gone(what);
    }

    long address() {
        check();
        return px;
    }

    /** The pixels as a view on the native memory, 0xFFRRGGBB for grabs. Only good while the image is fresh. */
    public IntBuffer pixels() {
        check();
        if (px == 0 || h <= 0) return IntBuffer.allocate(0);
        return Native.view(px, 4L * h * stride).asIntBuffer();
    }

    /** One pixel. */
    public int pixel(int x, int y) {
        check();
        if (x < 0 || y < 0 || x >= w || y >= h) throw new IndexOutOfBoundsException("pixel " + x + "," + y + " is outside " + w + "x" + h);
        return N.getInt(px + 4L * ((long) y * stride + x));
    }

    /** A copy of the pixels without the stride padding, w times h ints. */
    public int[] copy() {
        IntBuffer v = pixels();
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            v.position(y * stride);
            v.get(out, y * w, w);
        }
        return out;
    }

    /** Copies rect r (real px, null for all of it) into a direct buffer. Stride is in bytes. */
    public void copyTo(ByteBuffer dst, int dstStride, PixelFormat fmt, Rect r) {
        check();
        copy(px, w, h, stride, dst, dstStride, fmt, r);
    }

    static void copy(long px, int w, int h, int stride, ByteBuffer dst, int dstStride, PixelFormat fmt, Rect r) {
        if (r == null) r = new Rect(0, 0, w, h);
        if (r.x < 0 || r.y < 0 || r.w <= 0 || r.h <= 0 || r.x + r.w > w || r.y + r.h > h) {
            throw new GoomeException.BadArg("frame_copy: rect is off the " + w + "x" + h + " frame");
        }
        if (dstStride < r.w * 4) throw new GoomeException.BadArg("frame_copy: stride " + dstStride + " is too small for " + r.w + " px");
        long need = (long) (r.h - 1) * dstStride + r.w * 4L;
        if (dst.remaining() < need) throw new IllegalArgumentException("dst has " + dst.remaining() + " bytes left, needs " + need);
        ByteBuffer out = dst.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int base = dst.position();
        IntBuffer src = Native.view(px, 4L * h * stride).asIntBuffer();
        int[] row = new int[r.w];
        for (int y = 0; y < r.h; y++) {
            src.position((r.y + y) * stride + r.x);
            src.get(row, 0, r.w);
            int at = base + y * dstStride;
            for (int k = 0; k < r.w; k++) out.putInt(at + k * 4, convert(row[k], fmt));
        }
    }

    static int convert(int p, PixelFormat fmt) {
        switch (fmt) {
            case BGRA_PREMUL:
                return p;
            case RGBA_PREMUL:
                return swap(p);
            case BGRA:
                return unpremul(p);
            default:
                return swap(unpremul(p));
        }
    }

    private static int swap(int p) {
        return (p & 0xFF00FF00) | ((p >>> 16) & 0xFF) | ((p & 0xFF) << 16);
    }

    private static int unpremul(int p) {
        int a = p >>> 24;
        if (a == 255) return p;
        if (a == 0) return 0;
        int r = Math.min(255, (((p >>> 16) & 255) * 255 + a / 2) / a);
        int g = Math.min(255, (((p >>> 8) & 255) * 255 + a / 2) / a);
        int b = Math.min(255, ((p & 255) * 255 + a / 2) / a);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** Writes the image to a ppm file. */
    public void savePpm(String path) {
        check();
        Goome.savePpm(path, px, w, h, stride);
    }

    @Override
    public String toString() {
        return "Image(" + w + "x" + h + " of monitor " + mon + ")";
    }
}
