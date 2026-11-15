package io.github.goomeui;

import static io.github.goomeui.Native.N;

public record Item(double x, double y, double w, double h,
    boolean hovered, boolean pressed, boolean held, boolean released, boolean clicked, boolean doubleClicked, boolean rightClicked, boolean focused,
    double mx, double my, double dx, double dy, double dragX, double dragY, int key) {

    static Item read(long p) {
        Layout l = Layout.ITEM;
        return new Item(N.getDouble(p + l.at("x")), N.getDouble(p + l.at("y")), N.getDouble(p + l.at("w")), N.getDouble(p + l.at("h")),
            on(p, "hovered"), on(p, "pressed"), on(p, "held"), on(p, "released"), on(p, "clicked"), on(p, "double_clicked"), on(p, "right_clicked"),
            on(p, "focused"), N.getDouble(p + l.at("mx")), N.getDouble(p + l.at("my")), N.getDouble(p + l.at("dx")), N.getDouble(p + l.at("dy")),
            N.getDouble(p + l.at("drag_x")), N.getDouble(p + l.at("drag_y")), N.getInt(p + l.at("key")));
    }

    private static boolean on(long p, String f) {
        return N.getInt(p + Layout.ITEM.at(f)) != 0;
    }
}
