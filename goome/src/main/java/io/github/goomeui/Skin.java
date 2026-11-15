package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Skin {
    public final Bitmap bitmap;
    public final double left, top, right, bottom;

    public Skin(Bitmap bitmap, double border) {
        this(bitmap, border, border, border, border);
    }

    public Skin(Bitmap bitmap, double left, double top, double right, double bottom) {
        this.bitmap = bitmap;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    void write(long q) {
        Layout l = Layout.SKIN;
        N.putLong(q + l.at("bm"), bitmap.ptr());
        N.putDouble(q + l.at("l"), left);
        N.putDouble(q + l.at("t"), top);
        N.putDouble(q + l.at("r"), right);
        N.putDouble(q + l.at("b"), bottom);
    }
}
