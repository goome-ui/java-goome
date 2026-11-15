package io.github.goomeui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Layout {
    static final List<Layout> ALL = new ArrayList<>();

    final String name;
    int size;
    int align = 1;
    final Map<String, int[]> fields = new LinkedHashMap<>();

    private Layout(String name) {
        this.name = name;
    }

    private Layout add(String f, int size, int align) {
        size = Math.max(size, 0);
        int at = (this.size + align - 1) / align * align;
        fields.put(f, new int[] {at, size});
        this.size = at + size;
        this.align = Math.max(this.align, align);
        return this;
    }

    private Layout i(String... names) {
        for (String n : names) add(n, 4, 4);
        return this;
    }

    private Layout d(String... names) {
        for (String n : names) add(n, 8, 8);
        return this;
    }

    private Layout p(String... names) {
        for (String n : names) add(n, 8, 8);
        return this;
    }

    private Layout l(String n) {
        int s = Native.N.longSize();
        return add(n, s, s);
    }

    private Layout bytes(String n, int count) {
        return add(n, count, 1);
    }

    private Layout ints(String n, int count) {
        return add(n, 4 * count, 4);
    }

    private Layout ptrs(String n, int count) {
        return add(n, 8 * count, 8);
    }

    private Layout sub(String n, Layout l) {
        return add(n, l.size, l.align);
    }

    private Layout done() {
        size = (size + align - 1) / align * align;
        ALL.add(this);
        return this;
    }

    int at(String f) {
        int[] v = fields.get(f);
        if (v == null) throw new IllegalArgumentException(name + " has no " + f);
        return v[0];
    }

    Map<String, int[]> fields() {
        return Collections.unmodifiableMap(fields);
    }

    static final Layout OPTS = new Layout("goome_opts").p("title").i("backend").p("state_dir").i("no_capture", "capture_fps", "hide_self")
        .p("test_frames", "test_script", "test_shots").i("mode", "width", "height", "min_width", "min_height", "window_flags").d("scale").done();
    static final Layout RECT = new Layout("goome_rect").i("x", "y", "w", "h").done();
    static final Layout MONITOR = new Layout("goome_monitor").i("id").bytes("name", 64).sub("area", RECT).d("scale").i("pw", "ph", "rotation").done();
    static final Layout IMAGE = new Layout("goome_image").p("px").i("w", "h", "stride").sub("area", RECT).i("mon", "has_self", "has_cursor")
        .sub("cursor", RECT).done();
    static final Layout CANVAS = new Layout("goome_canvas").p("px").i("w", "h", "stride").d("ox", "oy", "scale").i("mon")
        .i("dx0", "dy0", "dx1", "dy1", "cx0", "cy0", "cx1", "cy1", "kx0", "ky0", "kx1", "ky1", "nclip").ints("clips", 64).done();
    static final Layout EVENT = new Layout("goome_event").i("type").d("x", "y").i("mon", "button", "dy", "key", "mods").bytes("text", 8).i("id")
        .bytes("msg", 64).done();
    static final Layout PACER = new Layout("goome_pacer").l("next").i("fps").done();
    static final Layout RADII = new Layout("goome_radii").d("tl", "tr", "br", "bl").done();
    static final Layout BITMAP = new Layout("goome_bitmap").p("px").i("w", "h", "stride", "owned").done();
    static final Layout SKIN = new Layout("goome_skin").p("bm").d("l", "t", "r", "b").done();
    static final Layout THEME = new Layout("goome_theme")
        .i("bg", "panel", "border", "text", "text_dim", "accent", "accent_text", "hover", "active", "input_bg", "shadow")
        .d("radius", "pad", "gap", "border_width", "font_size", "shadow_blur", "anim_ms").p("font")
        .sub("skin_button", SKIN).sub("skin_panel", SKIN).sub("skin_input", SKIN).ptrs("icon", Icon.COUNT).i("icon_tint")
        .sub("skin_checkbox", SKIN).sub("skin_radio", SKIN).sub("skin_toggle", SKIN).sub("skin_slider", SKIN).sub("skin_slider_fill", SKIN)
        .sub("skin_scrollbar", SKIN).sub("skin_tab", SKIN).sub("skin_titlebar", SKIN).sub("skin_tooltip", SKIN).sub("skin_menu", SKIN)
        .sub("skin_popup", SKIN).i("focus", "select", "caret", "link", "badge", "toast", "tooltip", "danger", "knob", "dim")
        .d("check_size", "check_radius", "toggle_w", "toggle_h", "slider_h", "slider_track", "knob_size")
        .d("scroll_w", "scroll_min", "item_h", "menu_item_h", "tab_h", "win_title_h", "app_title_h", "menubar_h", "statusbar_h")
        .d("focus_w", "focus_gap", "sep_w", "grip", "indent", "plot_w", "spinner", "caret_w")
        .d("win_min_w", "win_min_h", "pop_gap", "menu_gap", "tip_pad_x", "tip_pad_y", "tip_x", "tip_y", "tip_radius")
        .d("shadow_x", "shadow_y", "align", "heading_size", "small_size", "ease")
        .d("hover_ms", "press_ms", "popup_ms", "collapse_ms", "toast_fade_ms").p("font_heading", "font_mono", "font_small").done();
    static final Layout ITEM = new Layout("goome_ui_item").d("x", "y", "w", "h")
        .i("hovered", "pressed", "held", "released", "clicked", "double_clicked", "right_clicked", "focused")
        .d("mx", "my", "dx", "dy", "drag_x", "drag_y").i("key").done();
    static final Layout COMBO = new Layout("goome_combo").i("key", "mods").done();
    static final Layout CONFIG = new Layout("goome_ui_config")
        .i("tip_delay_ms", "dbl_click_ms", "dbl_click_px", "drag_px", "repeat_delay_ms", "repeat_ms", "blink_ms", "menu_delay_ms", "toast_ms")
        .d("scroll_lines").i("raise_on_click", "esc_closes", "tab_nav", "focus_on_mouse").ints("cursor", Config.CUR_COUNT)
        .ints("keys", 2 * 4 * Config.ACT_COUNT).p("ease", "ease_user", "mask").done();
}
