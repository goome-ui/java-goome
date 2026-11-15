package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Monitor {
    public final int id;
    public final String name;
    public final Rect area;
    public final double scale;
    public final int pw, ph;
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
