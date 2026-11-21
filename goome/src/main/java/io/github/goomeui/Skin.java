package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** A nine slice picture for a widget, set it with Theme.skin() or Ui.pushSkin(). */
public final class Skin {
    /** The picture. */
    public final Bitmap bitmap;
    /** Border sizes in bitmap px, the parts that dont stretch. */
    public final double left, top, right, bottom;

    /** A skin with the same border on every side. */
    public Skin(Bitmap bitmap, double border) {
        this(bitmap, border, border, border, border);
    }

    /** A skin with a border size per side. */
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
