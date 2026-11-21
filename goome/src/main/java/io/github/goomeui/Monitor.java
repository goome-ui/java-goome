package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** One monitor, from Goome.monitors(). */
public final class Monitor {
    /** The monitor number to pass to begin() and capture(). */
    public final int id;
    /** The monitor name. */
    public final String name;
    /** Where it sits on the desktop, logical coords. */
    public final Rect area;
    /** Real px per logical px. */
    public final double scale;
    /** Size in real px. */
    public final int pw, ph;
    /** 0, 90, 180 or 270, plus 360 if mirrored. Grabs and canvases already undo it. */
    public final int rotation;

    Monitor(long p) {
        Layout l = Layout.MONITOR;
        id = N.getInt(p + l.at("id"));
        name = Native.chars(p + l.at("name"), 64);
        area = Rect.read(p + l.at("area"));
        scale = N.getDouble(p + l.at("scale"));
        pw = N.getInt(p + l.at("pw"));
        ph = N.getInt(p + l.at("ph"));
        rotation = N.getInt(p + l.at("rotation"));
    }

    @Override
    public String toString() {
        return String.format("Monitor(%d, %s, %dx%d at %d,%d, scale %s)", id, name, area.w, area.h, area.x, area.y, scale);
    }
}
