package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.util.List;

public record A11yNode(int role, int parent, int states, double x, double y, double w, double h, String label, String value, long id,
    double num, double min, double max, int caret) {

    public static final int ROLE_NONE = 0, ROLE_WINDOW = 1, ROLE_BUTTON = 2, ROLE_CHECKBOX = 3, ROLE_RADIO = 4, ROLE_SLIDER = 5, ROLE_TEXT = 6,
        ROLE_INPUT = 7, ROLE_COMBO = 8, ROLE_LIST = 9, ROLE_ITEM = 10, ROLE_TAB = 11, ROLE_MENU = 12, ROLE_MENUITEM = 13, ROLE_TABLE = 14,
        ROLE_TREE = 15, ROLE_PROGRESS = 16, ROLE_IMAGE = 17, ROLE_GROUP = 18, ROLE_DIALOG = 19;
    public static final int FOCUSED = 1, CHECKED = 2, DISABLED = 4, EXPANDED = 8, SELECTED = 16;
    public static final List<String> ROLE_NAMES = List.of("none", "window", "button", "checkbox", "radio", "slider", "text", "input", "combo",
        "list", "item", "tab", "menu", "menuitem", "table", "tree", "progress", "image", "group", "dialog");

    static A11yNode read(long p) {
        Layout l = Layout.A11Y_NODE;
        return new A11yNode(N.getInt(p + l.at("role")), N.getInt(p + l.at("parent")), N.getInt(p + l.at("states")), N.getDouble(p + l.at("x")),
            N.getDouble(p + l.at("y")), N.getDouble(p + l.at("w")), N.getDouble(p + l.at("h")), Native.chars(p + l.at("label"), 96),
            Native.chars(p + l.at("value"), 64), N.getInt(p + l.at("id")) & 0xFFFFFFFFL, N.getDouble(p + l.at("num")), N.getDouble(p + l.at("min")),
            N.getDouble(p + l.at("max")), N.getInt(p + l.at("caret")));
    }

    public String roleName() {
        return role >= 0 && role < ROLE_NAMES.size() ? ROLE_NAMES.get(role) : String.valueOf(role);
    }

    public boolean has(int state) {
        return (states & state) != 0;
    }

    public boolean focused() {
        return has(FOCUSED);
    }

    public boolean checked() {
        return has(CHECKED);
    }

    public boolean disabled() {
        return has(DISABLED);
    }

    public boolean expanded() {
        return has(EXPANDED);
    }

    public boolean selected() {
        return has(SELECTED);
    }

    @Override
    public String toString() {
        String v = value.isEmpty() ? "" : " = '" + value + "'";
        return "A11yNode(" + roleName() + " '" + label + "'" + v + " parent " + parent + " states " + states + ")";
    }
}
