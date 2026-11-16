package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Theme {
    public static final int ICON_TINT = 0, ICON_AS_IS = 1;
    public static final int ALIGN_AUTO = 0, ALIGN_LEFT = 1, ALIGN_CENTER = 2, ALIGN_RIGHT = 3;
    public static final int EASE_SMOOTH = 0, EASE_LINEAR = 1, EASE_OUT = 2, EASE_IN_OUT = 3, EASE_BACK = 4, EASE_CUSTOM = 5, EASE_IN = 6, EASE_SPRING = 7,
        EASE_BOUNCE = 8;
    public static final int PRESET_DARK = 0, PRESET_LIGHT = 1, PRESET_MIDNIGHT = 2, PRESET_PAPER = 3, PRESET_MINT = 4, PRESET_HIGH_CONTRAST = 5,
        PRESET_ROSE = 6, PRESET_COUNT = 7;
    static final Set<String> FONTS = Set.of("font", "font_heading", "font_mono", "font_small");
    private static final Layout L = Layout.THEME;
    private static final Layout S = Layout.SKIN;
    private static final Set<Theme> LIVE = Collections.newSetFromMap(new WeakHashMap<>());

    private final Ui ui;
    private final Cell own;
    private final Map<String, Font> fonts = new HashMap<>();
    private final Map<String, Skin> skins = new HashMap<>();
    private final Bitmap[] icons = new Bitmap[Icon.COUNT];
    static final String[] SKINS = {"skin_button", "skin_panel", "skin_input", "skin_checkbox", "skin_radio", "skin_toggle", "skin_slider",
        "skin_slider_fill", "skin_scrollbar", "skin_tab", "skin_titlebar", "skin_tooltip", "skin_menu", "skin_popup"};

    Theme(Ui ui) {
        this.ui = ui;
        own = null;
        synchronized (LIVE) {
            LIVE.add(this);
        }
    }

    private Theme() {
        ui = null;
        own = new Cell(this, L.size);
        synchronized (LIVE) {
            LIVE.add(this);
        }
    }

    public Theme copy() {
        return new Theme().set(this);
    }

    public Theme set(Theme from) {
        if (from == this) return this;
        Native.view(p(), L.size).put(Native.view(from.p(), L.size));
        fonts.clear();
        fonts.putAll(from.fonts);
        skins.clear();
        skins.putAll(from.skins);
        System.arraycopy(from.icons, 0, icons, 0, Icon.COUNT);
        return this;
    }

    public static Theme make(int accent, boolean dark, double radius) {
        return make(null, accent, dark, radius);
    }

    public static Theme make(Theme base, int accent, boolean dark, double radius) {
        Theme t = base == null ? new Theme() : base.copy();
        Native.call(Fn.UI_THEME_MAKE, t.p(), accent, Native.b(dark), Native.b(radius));
        return t;
    }

    public static Theme preset(int which) {
        return preset(null, which);
    }

    public static Theme preset(Theme base, int which) {
        if (which < 0 || which >= PRESET_COUNT) throw new IllegalArgumentException("preset is one of the PRESET_ numbers, not " + which);
        Theme t = base == null ? new Theme() : base.copy();
        Native.call(Fn.UI_THEME_PRESET, t.p(), which);
        return t;
    }

    long ptr() {
        return p();
    }

    private long p() {
        return own != null ? own.p : Native.call(Fn.UI_THEME, ui.h());
    }

    void detach() {
        synchronized (LIVE) {
            LIVE.remove(this);
        }
        fonts.clear();
        skins.clear();
        Arrays.fill(icons, null);
    }

    static void forget(Object res) {
        Theme[] all;
        synchronized (LIVE) {
            all = LIVE.toArray(new Theme[0]);
        }
        for (Theme t : all) {
            if (t.ui != null && t.ui.closed()) continue;
            for (String k : FONTS) if (t.fonts.get(k) == res) t.font(k, null);
            for (String k : t.skins.keySet().toArray(new String[0])) if (t.skins.get(k).bitmap == res) t.skin(k, null);
            for (Icon i : Icon.values()) if (t.icons[i.ordinal()] == res) t.icon(i, null);
        }
    }

    public void dark() {
        Native.call(Fn.UI_THEME_DARK, p());
    }

    public void light() {
        Native.call(Fn.UI_THEME_LIGHT, p());
    }

    public int color(String name) {
        return N.getInt(p() + colorAt(name));
    }

    public void color(String name, int argb) {
        N.putInt(p() + colorAt(name), argb);
    }

    public double size(String name) {
        return N.getDouble(p() + sizeAt(name));
    }

    public void size(String name, double v) {
        N.putDouble(p() + sizeAt(name), v);
    }

    private static int colorAt(String name) {
        int[] f = L.fields().get(name);
        if (f == null || f[1] != 4 || name.equals("icon_tint")) throw new IllegalArgumentException("theme has no color called '" + name + "'");
        return f[0];
    }

    private static int sizeAt(String name) {
        int[] f = L.fields().get(name);
        if (f == null || f[1] != 8 || FONTS.contains(name) || name.startsWith("skin")) throw new IllegalArgumentException("theme has no size called '" + name + "'");
        return f[0];
    }

    public int bg() { return color("bg"); }
    public Theme bg(int v) { color("bg", v); return this; }
    public int panel() { return color("panel"); }
    public Theme panel(int v) { color("panel", v); return this; }
    public int border() { return color("border"); }
    public Theme border(int v) { color("border", v); return this; }
    public int text() { return color("text"); }
    public Theme text(int v) { color("text", v); return this; }
    public int textDim() { return color("text_dim"); }
    public Theme textDim(int v) { color("text_dim", v); return this; }
    public int accent() { return color("accent"); }
    public Theme accent(int v) { color("accent", v); return this; }
    public int accentText() { return color("accent_text"); }
    public Theme accentText(int v) { color("accent_text", v); return this; }
    public int hover() { return color("hover"); }
    public Theme hover(int v) { color("hover", v); return this; }
    public int active() { return color("active"); }
    public Theme active(int v) { color("active", v); return this; }
    public int inputBg() { return color("input_bg"); }
    public Theme inputBg(int v) { color("input_bg", v); return this; }
    public int shadow() { return color("shadow"); }
    public Theme shadow(int v) { color("shadow", v); return this; }

    public double radius() { return size("radius"); }
    public Theme radius(double v) { size("radius", v); return this; }
    public double pad() { return size("pad"); }
    public Theme pad(double v) { size("pad", v); return this; }
    public double gap() { return size("gap"); }
    public Theme gap(double v) { size("gap", v); return this; }
    public double borderWidth() { return size("border_width"); }
    public Theme borderWidth(double v) { size("border_width", v); return this; }
    public double fontSize() { return size("font_size"); }
    public Theme fontSize(double v) { size("font_size", v); return this; }
    public double shadowBlur() { return size("shadow_blur"); }
    public Theme shadowBlur(double v) { size("shadow_blur", v); return this; }
    public double animMs() { return size("anim_ms"); }
    public Theme animMs(double v) { size("anim_ms", v); return this; }

    public int focus() { return color("focus"); }
    public Theme focus(int v) { color("focus", v); return this; }
    public int select() { return color("select"); }
    public Theme select(int v) { color("select", v); return this; }
    public int caret() { return color("caret"); }
    public Theme caret(int v) { color("caret", v); return this; }
    public int link() { return color("link"); }
    public Theme link(int v) { color("link", v); return this; }
    public int badge() { return color("badge"); }
    public Theme badge(int v) { color("badge", v); return this; }
    public int toast() { return color("toast"); }
    public Theme toast(int v) { color("toast", v); return this; }
    public int tooltip() { return color("tooltip"); }
    public Theme tooltip(int v) { color("tooltip", v); return this; }
    public int danger() { return color("danger"); }
    public Theme danger(int v) { color("danger", v); return this; }
    public int knob() { return color("knob"); }
    public Theme knob(int v) { color("knob", v); return this; }
    public int dim() { return color("dim"); }
    public Theme dim(int v) { color("dim", v); return this; }

    public double checkSize() { return size("check_size"); }
    public Theme checkSize(double v) { size("check_size", v); return this; }
    public double checkRadius() { return size("check_radius"); }
    public Theme checkRadius(double v) { size("check_radius", v); return this; }
    public double toggleW() { return size("toggle_w"); }
    public Theme toggleW(double v) { size("toggle_w", v); return this; }
    public double toggleH() { return size("toggle_h"); }
    public Theme toggleH(double v) { size("toggle_h", v); return this; }
    public double sliderH() { return size("slider_h"); }
    public Theme sliderH(double v) { size("slider_h", v); return this; }
    public double sliderTrack() { return size("slider_track"); }
    public Theme sliderTrack(double v) { size("slider_track", v); return this; }
    public double knobSize() { return size("knob_size"); }
    public Theme knobSize(double v) { size("knob_size", v); return this; }
    public double scrollW() { return size("scroll_w"); }
    public Theme scrollW(double v) { size("scroll_w", v); return this; }
    public double scrollMin() { return size("scroll_min"); }
    public Theme scrollMin(double v) { size("scroll_min", v); return this; }
    public double itemH() { return size("item_h"); }
    public Theme itemH(double v) { size("item_h", v); return this; }
    public double menuItemH() { return size("menu_item_h"); }
    public Theme menuItemH(double v) { size("menu_item_h", v); return this; }
    public double tabH() { return size("tab_h"); }
    public Theme tabH(double v) { size("tab_h", v); return this; }
    public double winTitleH() { return size("win_title_h"); }
    public Theme winTitleH(double v) { size("win_title_h", v); return this; }
    public double appTitleH() { return size("app_title_h"); }
    public Theme appTitleH(double v) { size("app_title_h", v); return this; }
    public double menubarH() { return size("menubar_h"); }
    public Theme menubarH(double v) { size("menubar_h", v); return this; }
    public double statusbarH() { return size("statusbar_h"); }
    public Theme statusbarH(double v) { size("statusbar_h", v); return this; }
    public double focusW() { return size("focus_w"); }
    public Theme focusW(double v) { size("focus_w", v); return this; }
    public double focusGap() { return size("focus_gap"); }
    public Theme focusGap(double v) { size("focus_gap", v); return this; }
    public double sepW() { return size("sep_w"); }
    public Theme sepW(double v) { size("sep_w", v); return this; }
    public double grip() { return size("grip"); }
    public Theme grip(double v) { size("grip", v); return this; }
    public double indent() { return size("indent"); }
    public Theme indent(double v) { size("indent", v); return this; }
    public double plotW() { return size("plot_w"); }
    public Theme plotW(double v) { size("plot_w", v); return this; }
    public double spinner() { return size("spinner"); }
    public Theme spinner(double v) { size("spinner", v); return this; }
    public double caretW() { return size("caret_w"); }
    public Theme caretW(double v) { size("caret_w", v); return this; }
    public double winMinW() { return size("win_min_w"); }
    public Theme winMinW(double v) { size("win_min_w", v); return this; }
    public double winMinH() { return size("win_min_h"); }
    public Theme winMinH(double v) { size("win_min_h", v); return this; }
    public double popGap() { return size("pop_gap"); }
    public Theme popGap(double v) { size("pop_gap", v); return this; }
    public double menuGap() { return size("menu_gap"); }
    public Theme menuGap(double v) { size("menu_gap", v); return this; }
    public double tipPadX() { return size("tip_pad_x"); }
    public Theme tipPadX(double v) { size("tip_pad_x", v); return this; }
    public double tipPadY() { return size("tip_pad_y"); }
    public Theme tipPadY(double v) { size("tip_pad_y", v); return this; }
    public double tipX() { return size("tip_x"); }
    public Theme tipX(double v) { size("tip_x", v); return this; }
    public double tipY() { return size("tip_y"); }
    public Theme tipY(double v) { size("tip_y", v); return this; }
    public double tipRadius() { return size("tip_radius"); }
    public Theme tipRadius(double v) { size("tip_radius", v); return this; }
    public double shadowX() { return size("shadow_x"); }
    public Theme shadowX(double v) { size("shadow_x", v); return this; }
    public double shadowY() { return size("shadow_y"); }
    public Theme shadowY(double v) { size("shadow_y", v); return this; }
    public double align() { return size("align"); }
    public Theme align(double v) { size("align", v); return this; }
    public double headingSize() { return size("heading_size"); }
    public Theme headingSize(double v) { size("heading_size", v); return this; }
    public double smallSize() { return size("small_size"); }
    public Theme smallSize(double v) { size("small_size", v); return this; }
    public double ease() { return size("ease"); }
    public Theme ease(double v) { size("ease", v); return this; }
    public double hoverMs() { return size("hover_ms"); }
    public Theme hoverMs(double v) { size("hover_ms", v); return this; }
    public double pressMs() { return size("press_ms"); }
    public Theme pressMs(double v) { size("press_ms", v); return this; }
    public double popupMs() { return size("popup_ms"); }
    public Theme popupMs(double v) { size("popup_ms", v); return this; }
    public double collapseMs() { return size("collapse_ms"); }
    public Theme collapseMs(double v) { size("collapse_ms", v); return this; }
    public double toastFadeMs() { return size("toast_fade_ms"); }
    public Theme toastFadeMs(double v) { size("toast_fade_ms", v); return this; }

    public boolean hasFont() {
        return N.getLong(p() + L.at("font")) != 0;
    }

    public Font font() { return font("font"); }
    public Theme font(Font f) { return font("font", f); }
    public Font fontHeading() { return font("font_heading"); }
    public Theme fontHeading(Font f) { return font("font_heading", f); }
    public Font fontMono() { return font("font_mono"); }
    public Theme fontMono(Font f) { return font("font_mono", f); }
    public Font fontSmall() { return font("font_small"); }
    public Theme fontSmall(Font f) { return font("font_small", f); }

    private Font font(String slot) {
        long f = N.getLong(p() + L.at(slot));
        if (f == 0) return null;
        Font kept = fonts.get(slot);
        if (kept != null && kept.raw() == f) return kept;
        return new Font(f, 0, false);
    }

    private Theme font(String slot, Font f) {
        N.putLong(p() + L.at(slot), f == null ? 0 : f.handle());
        if (f == null) fonts.remove(slot);
        else fonts.put(slot, f);
        return this;
    }

    public Skin skin(String name) {
        at(name);
        return skins.get(name);
    }

    public Theme skin(String name, Skin skin) {
        long q = p() + at(name);
        if (skin == null) {
            N.zero(q, S.size);
            skins.remove(name);
            return this;
        }
        skin.write(q);
        skins.put(name, skin);
        return this;
    }

    public Skin skin(int kind) {
        return skin(skinName(kind));
    }

    public Theme skin(int kind, Skin skin) {
        return skin(skinName(kind), skin);
    }

    public Bitmap icon(Icon which) {
        return icons[which.ordinal()];
    }

    public Theme icon(Icon which, Bitmap bm) {
        N.putLong(p() + L.at("icon") + 8L * which.ordinal(), bm == null ? 0 : bm.ptr());
        icons[which.ordinal()] = bm;
        return this;
    }

    public int iconTint() {
        return N.getInt(p() + L.at("icon_tint"));
    }

    public Theme iconTint(int v) {
        N.putInt(p() + L.at("icon_tint"), v);
        return this;
    }

    static String skinName(int kind) {
        if (kind < 0 || kind >= SKINS.length) throw new IllegalArgumentException("skin kind is one of the Ui.SKIN_ numbers, not " + kind);
        return SKINS[kind];
    }

    private static int at(String name) {
        if (!Arrays.asList(SKINS).contains(name)) {
            throw new IllegalArgumentException("theme has no skin called '" + name + "', its one of " + String.join(", ", SKINS));
        }
        return L.at(name);
    }
}
