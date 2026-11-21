package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The colors, sizes, fonts, skins and icons the ui draws with. Ui.theme() is the live one, change a field
 * and the next frame uses it. make() and preset() build standalone ones you can push with Ui.pushTheme().
 * Colors are 0xAARRGGBB.
 */
public final class Theme {
    /** Values for iconTint(int): ICON_TINT tints the icons, ICON_AS_IS draws them as they are. */
    public static final int ICON_TINT = 0, ICON_AS_IS = 1;
    /** Values for align(double): auto, left, center or right. */
    public static final int ALIGN_AUTO = 0, ALIGN_LEFT = 1, ALIGN_CENTER = 2, ALIGN_RIGHT = 3;
    /** Easing curves for ease(double) and Ui.ease(). */
    public static final int EASE_SMOOTH = 0, EASE_LINEAR = 1, EASE_OUT = 2, EASE_IN_OUT = 3, EASE_BACK = 4, EASE_CUSTOM = 5, EASE_IN = 6, EASE_SPRING = 7,
        EASE_BOUNCE = 8;
    /** Presets for preset(int). PRESET_COUNT is how many there are. */
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

    /** An independent copy of this theme. */
    public Theme copy() {
        return new Theme().set(this);
    }

    /** Copies every field, font, skin and icon of another theme into this one. Returns this. */
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

    /** Builds a standalone theme from an accent color, dark or light, and a corner radius. */
    public static Theme make(int accent, boolean dark, double radius) {
        return make(null, accent, dark, radius);
    }

    /** Same, but starts from a copy of base. */
    public static Theme make(Theme base, int accent, boolean dark, double radius) {
        Theme t = base == null ? new Theme() : base.copy();
        Native.call(Fn.UI_THEME_MAKE, t.p(), accent, Native.b(dark), Native.b(radius));
        return t;
    }

    /** Builds a standalone theme from one of the PRESET_ numbers. Throws if it isnt one. */
    public static Theme preset(int which) {
        return preset(null, which);
    }

    /** Same, but starts from a copy of base. */
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

    /** Resets every color and size to the built in dark ones. Leaves the fonts alone. */
    public void dark() {
        Native.call(Fn.UI_THEME_DARK, p());
    }

    /** Switches the first 11 colors (bg to shadow) to the built in light ones. */
    public void light() {
        Native.call(Fn.UI_THEME_LIGHT, p());
    }

    /** Reads a color field by its native name, like accent or text_dim. Throws if there is none. */
    public int color(String name) {
        return N.getInt(p() + colorAt(name));
    }

    /** Sets a color field by its native name. */
    public void color(String name, int argb) {
        N.putInt(p() + colorAt(name), argb);
    }

    /** Reads a number field by its native name, like radius or tip_pad_x. Throws if there is none. */
    public double size(String name) {
        return N.getDouble(p() + sizeAt(name));
    }

    /** Sets a number field by its native name. */
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

    /** Returns the color behind everything, app mode fills with it. */
    public int bg() { return color("bg"); }
    /** Sets the color behind everything, app mode fills with it. Returns this so you can chain. */
    public Theme bg(int v) { color("bg", v); return this; }
    /** Returns the window background color. */
    public int panel() { return color("panel"); }
    /** Sets the window background color. Returns this so you can chain. */
    public Theme panel(int v) { color("panel", v); return this; }
    /** Returns the outline color of windows and widgets. */
    public int border() { return color("border"); }
    /** Sets the outline color of windows and widgets. Returns this so you can chain. */
    public Theme border(int v) { color("border", v); return this; }
    /** Returns the normal text color. */
    public int text() { return color("text"); }
    /** Sets the normal text color. Returns this so you can chain. */
    public Theme text(int v) { color("text", v); return this; }
    /** Returns the quieter text color. */
    public int textDim() { return color("text_dim"); }
    /** Sets the quieter text color. Returns this so you can chain. */
    public Theme textDim(int v) { color("text_dim", v); return this; }
    /** Returns the accent color, used for checked boxes, slider fill and the active tab. */
    public int accent() { return color("accent"); }
    /**
     * Sets the accent color, used for checked boxes, slider fill and the active tab. Returns this so you
     * can chain.
     */
    public Theme accent(int v) { color("accent", v); return this; }
    /** Returns the color of text drawn on top of the accent. */
    public int accentText() { return color("accent_text"); }
    /** Sets the color of text drawn on top of the accent. Returns this so you can chain. */
    public Theme accentText(int v) { color("accent_text", v); return this; }
    /** Returns the widget background color while hovered. */
    public int hover() { return color("hover"); }
    /** Sets the widget background color while hovered. Returns this so you can chain. */
    public Theme hover(int v) { color("hover", v); return this; }
    /** Returns the widget background color while pressed. */
    public int active() { return color("active"); }
    /** Sets the widget background color while pressed. Returns this so you can chain. */
    public Theme active(int v) { color("active", v); return this; }
    /** Returns the color inside inputs and sliders. */
    public int inputBg() { return color("input_bg"); }
    /** Sets the color inside inputs and sliders. Returns this so you can chain. */
    public Theme inputBg(int v) { color("input_bg", v); return this; }
    /** Returns the window shadow color. */
    public int shadow() { return color("shadow"); }
    /** Sets the window shadow color. Returns this so you can chain. */
    public Theme shadow(int v) { color("shadow", v); return this; }

    /** Returns the corner radius. */
    public double radius() { return size("radius"); }
    /** Sets the corner radius. Returns this so you can chain. */
    public Theme radius(double v) { size("radius", v); return this; }
    /** Returns the space inside windows. */
    public double pad() { return size("pad"); }
    /** Sets the space inside windows. Returns this so you can chain. */
    public Theme pad(double v) { size("pad", v); return this; }
    /** Returns the space between widgets. */
    public double gap() { return size("gap"); }
    /** Sets the space between widgets. Returns this so you can chain. */
    public Theme gap(double v) { size("gap", v); return this; }
    /** Returns the outline width. */
    public double borderWidth() { return size("border_width"); }
    /** Sets the outline width. Returns this so you can chain. */
    public Theme borderWidth(double v) { size("border_width", v); return this; }
    /** Returns the text size, checkboxes and knobs grow with it. */
    public double fontSize() { return size("font_size"); }
    /** Sets the text size, checkboxes and knobs grow with it. Returns this so you can chain. */
    public Theme fontSize(double v) { size("font_size", v); return this; }
    /** Returns how soft the shadows are. */
    public double shadowBlur() { return size("shadow_blur"); }
    /** Sets how soft the shadows are. Returns this so you can chain. */
    public Theme shadowBlur(double v) { size("shadow_blur", v); return this; }
    /** Returns the animation length in ms, 0 turns animation off. */
    public double animMs() { return size("anim_ms"); }
    /** Sets the animation length in ms, 0 turns animation off. Returns this so you can chain. */
    public Theme animMs(double v) { size("anim_ms", v); return this; }

    /** Returns the focus ring and input glow color, 0 uses the accent. */
    public int focus() { return color("focus"); }
    /** Sets the focus ring and input glow color, 0 uses the accent. Returns this so you can chain. */
    public Theme focus(int v) { color("focus", v); return this; }
    /** Returns the selected text color, 0 uses a faded accent. */
    public int select() { return color("select"); }
    /** Sets the selected text color, 0 uses a faded accent. Returns this so you can chain. */
    public Theme select(int v) { color("select", v); return this; }
    /** Returns the text caret color, 0 uses the accent. */
    public int caret() { return color("caret"); }
    /** Sets the text caret color, 0 uses the accent. Returns this so you can chain. */
    public Theme caret(int v) { color("caret", v); return this; }
    /** Returns the link color, 0 uses the accent. */
    public int link() { return color("link"); }
    /** Sets the link color, 0 uses the accent. Returns this so you can chain. */
    public Theme link(int v) { color("link", v); return this; }
    /** Returns the badge color when none is passed, 0 uses the accent. */
    public int badge() { return color("badge"); }
    /** Sets the badge color when none is passed, 0 uses the accent. Returns this so you can chain. */
    public Theme badge(int v) { color("badge", v); return this; }
    /** Returns the toast background color, 0 mixes panel and hover. */
    public int toast() { return color("toast"); }
    /** Sets the toast background color, 0 mixes panel and hover. Returns this so you can chain. */
    public Theme toast(int v) { color("toast", v); return this; }
    /** Returns the tooltip background color, 0 mixes bg and hover. */
    public int tooltip() { return color("tooltip"); }
    /** Sets the tooltip background color, 0 mixes bg and hover. Returns this so you can chain. */
    public Theme tooltip(int v) { color("tooltip", v); return this; }
    /** Returns the color of the red close buttons, 0 uses the built in red. */
    public int danger() { return color("danger"); }
    /** Sets the color of the red close buttons, 0 uses the built in red. Returns this so you can chain. */
    public Theme danger(int v) { color("danger", v); return this; }
    /** Returns the color of slider and toggle knobs, 0 is white. */
    public int knob() { return color("knob"); }
    /** Sets the color of slider and toggle knobs, 0 is white. Returns this so you can chain. */
    public Theme knob(int v) { color("knob", v); return this; }
    /** Returns the color drawn behind modals, 0 uses 0x73000000. */
    public int dim() { return color("dim"); }
    /** Sets the color drawn behind modals, 0 uses 0x73000000. Returns this so you can chain. */
    public Theme dim(int v) { color("dim", v); return this; }

    /** Returns the size of checkbox and radio boxes. */
    public double checkSize() { return size("check_size"); }
    /** Sets the size of checkbox and radio boxes. Returns this so you can chain. */
    public Theme checkSize(double v) { size("check_size", v); return this; }
    /** Returns the corner radius of checkboxes. */
    public double checkRadius() { return size("check_radius"); }
    /** Sets the corner radius of checkboxes. Returns this so you can chain. */
    public Theme checkRadius(double v) { size("check_radius", v); return this; }
    /** Returns the toggle track width. */
    public double toggleW() { return size("toggle_w"); }
    /** Sets the toggle track width. Returns this so you can chain. */
    public Theme toggleW(double v) { size("toggle_w", v); return this; }
    /** Returns the toggle track height. */
    public double toggleH() { return size("toggle_h"); }
    /** Sets the toggle track height. Returns this so you can chain. */
    public Theme toggleH(double v) { size("toggle_h", v); return this; }
    /** Returns the slider height. */
    public double sliderH() { return size("slider_h"); }
    /** Sets the slider height. Returns this so you can chain. */
    public Theme sliderH(double v) { size("slider_h", v); return this; }
    /** Returns the slider track thickness. */
    public double sliderTrack() { return size("slider_track"); }
    /** Sets the slider track thickness. Returns this so you can chain. */
    public Theme sliderTrack(double v) { size("slider_track", v); return this; }
    /** Returns the slider knob radius. */
    public double knobSize() { return size("knob_size"); }
    /** Sets the slider knob radius. Returns this so you can chain. */
    public Theme knobSize(double v) { size("knob_size", v); return this; }
    /** Returns the scrollbar width. */
    public double scrollW() { return size("scroll_w"); }
    /** Sets the scrollbar width. Returns this so you can chain. */
    public Theme scrollW(double v) { size("scroll_w", v); return this; }
    /** Returns the shortest a scrollbar thumb gets. */
    public double scrollMin() { return size("scroll_min"); }
    /** Sets the shortest a scrollbar thumb gets. Returns this so you can chain. */
    public Theme scrollMin(double v) { size("scroll_min", v); return this; }
    /** Returns the height of every widget row, 0 means line height plus pad. */
    public double itemH() { return size("item_h"); }
    /** Sets the height of every widget row, 0 means line height plus pad. Returns this so you can chain. */
    public Theme itemH(double v) { size("item_h", v); return this; }
    /** Returns the height of menu rows, 0 comes from the font. */
    public double menuItemH() { return size("menu_item_h"); }
    /** Sets the height of menu rows, 0 comes from the font. Returns this so you can chain. */
    public Theme menuItemH(double v) { size("menu_item_h", v); return this; }
    /** Returns the height of tabs, 0 comes from the font. */
    public double tabH() { return size("tab_h"); }
    /** Sets the height of tabs, 0 comes from the font. Returns this so you can chain. */
    public Theme tabH(double v) { size("tab_h", v); return this; }
    /** Returns the window title bar height, 0 comes from the font. */
    public double winTitleH() { return size("win_title_h"); }
    /** Sets the window title bar height, 0 comes from the font. Returns this so you can chain. */
    public Theme winTitleH(double v) { size("win_title_h", v); return this; }
    /** Returns the app mode title bar height, 0 comes from the font. */
    public double appTitleH() { return size("app_title_h"); }
    /** Sets the app mode title bar height, 0 comes from the font. Returns this so you can chain. */
    public Theme appTitleH(double v) { size("app_title_h", v); return this; }
    /** Returns the app menu bar height, 0 comes from the font. */
    public double menubarH() { return size("menubar_h"); }
    /** Sets the app menu bar height, 0 comes from the font. Returns this so you can chain. */
    public Theme menubarH(double v) { size("menubar_h", v); return this; }
    /** Returns the app status bar height, 0 comes from the font. */
    public double statusbarH() { return size("statusbar_h"); }
    /** Sets the app status bar height, 0 comes from the font. Returns this so you can chain. */
    public Theme statusbarH(double v) { size("statusbar_h", v); return this; }
    /** Returns the focus ring width. */
    public double focusW() { return size("focus_w"); }
    /** Sets the focus ring width. Returns this so you can chain. */
    public Theme focusW(double v) { size("focus_w", v); return this; }
    /** Returns the space between a widget and its focus ring. */
    public double focusGap() { return size("focus_gap"); }
    /** Sets the space between a widget and its focus ring. Returns this so you can chain. */
    public Theme focusGap(double v) { size("focus_gap", v); return this; }
    /** Returns the separator thickness. */
    public double sepW() { return size("sep_w"); }
    /** Sets the separator thickness. Returns this so you can chain. */
    public Theme sepW(double v) { size("sep_w", v); return this; }
    /** Returns the size of the window resize grip. */
    public double grip() { return size("grip"); }
    /** Sets the size of the window resize grip. Returns this so you can chain. */
    public Theme grip(double v) { size("grip", v); return this; }
    /** Returns the tree indent. */
    public double indent() { return size("indent"); }
    /** Sets the tree indent. Returns this so you can chain. */
    public Theme indent(double v) { size("indent", v); return this; }
    /** Returns the plot line width. */
    public double plotW() { return size("plot_w"); }
    /** Sets the plot line width. Returns this so you can chain. */
    public Theme plotW(double v) { size("plot_w", v); return this; }
    /** Returns the spinner size when 0 is passed, 0 means line height. */
    public double spinner() { return size("spinner"); }
    /** Sets the spinner size when 0 is passed, 0 means line height. Returns this so you can chain. */
    public Theme spinner(double v) { size("spinner", v); return this; }
    /** Returns the caret width, 0 is thin and a wide one makes a block caret. */
    public double caretW() { return size("caret_w"); }
    /** Sets the caret width, 0 is thin and a wide one makes a block caret. Returns this so you can chain. */
    public Theme caretW(double v) { size("caret_w", v); return this; }
    /** Returns the smallest width a window resizes to. */
    public double winMinW() { return size("win_min_w"); }
    /** Sets the smallest width a window resizes to. Returns this so you can chain. */
    public Theme winMinW(double v) { size("win_min_w", v); return this; }
    /** Returns the smallest height a window resizes to. */
    public double winMinH() { return size("win_min_h"); }
    /** Sets the smallest height a window resizes to. Returns this so you can chain. */
    public Theme winMinH(double v) { size("win_min_h", v); return this; }
    /** Returns the space between a combo or popup and what opened it. */
    public double popGap() { return size("pop_gap"); }
    /** Sets the space between a combo or popup and what opened it. Returns this so you can chain. */
    public Theme popGap(double v) { size("pop_gap", v); return this; }
    /** Returns the offset of submenus. */
    public double menuGap() { return size("menu_gap"); }
    /** Sets the offset of submenus. Returns this so you can chain. */
    public Theme menuGap(double v) { size("menu_gap", v); return this; }
    /** Returns the horizontal padding inside tooltips. */
    public double tipPadX() { return size("tip_pad_x"); }
    /** Sets the horizontal padding inside tooltips. Returns this so you can chain. */
    public Theme tipPadX(double v) { size("tip_pad_x", v); return this; }
    /** Returns the vertical padding inside tooltips. */
    public double tipPadY() { return size("tip_pad_y"); }
    /** Sets the vertical padding inside tooltips. Returns this so you can chain. */
    public Theme tipPadY(double v) { size("tip_pad_y", v); return this; }
    /** Returns the horizontal tooltip offset from the mouse. */
    public double tipX() { return size("tip_x"); }
    /** Sets the horizontal tooltip offset from the mouse. Returns this so you can chain. */
    public Theme tipX(double v) { size("tip_x", v); return this; }
    /** Returns the vertical tooltip offset from the mouse. */
    public double tipY() { return size("tip_y"); }
    /** Sets the vertical tooltip offset from the mouse. Returns this so you can chain. */
    public Theme tipY(double v) { size("tip_y", v); return this; }
    /** Returns the tooltip corner radius. */
    public double tipRadius() { return size("tip_radius"); }
    /** Sets the tooltip corner radius. Returns this so you can chain. */
    public Theme tipRadius(double v) { size("tip_radius", v); return this; }
    /** Returns the horizontal shadow offset. */
    public double shadowX() { return size("shadow_x"); }
    /** Sets the horizontal shadow offset. Returns this so you can chain. */
    public Theme shadowX(double v) { size("shadow_x", v); return this; }
    /** Returns the vertical shadow offset. */
    public double shadowY() { return size("shadow_y"); }
    /** Sets the vertical shadow offset. Returns this so you can chain. */
    public Theme shadowY(double v) { size("shadow_y", v); return this; }
    /**
     * Returns the text alignment of buttons and labels, one of the ALIGN_ numbers, auto is what each widget
     * normally does.
     */
    public double align() { return size("align"); }
    /**
     * Sets the text alignment of buttons and labels, one of the ALIGN_ numbers, auto is what each widget
     * normally does. Returns this so you can chain.
     */
    public Theme align(double v) { size("align", v); return this; }
    /** Returns the heading text size, 0 means 1.3 times the font size. */
    public double headingSize() { return size("heading_size"); }
    /** Sets the heading text size, 0 means 1.3 times the font size. Returns this so you can chain. */
    public Theme headingSize(double v) { size("heading_size", v); return this; }
    /** Returns the size of badge and status bar text, 0 is a bit under the font size. */
    public double smallSize() { return size("small_size"); }
    /** Sets the size of badge and status bar text, 0 is a bit under the font size. Returns this so you can chain. */
    public Theme smallSize(double v) { size("small_size", v); return this; }
    /** Returns the easing curve, one of the EASE_ numbers. */
    public double ease() { return size("ease"); }
    /** Sets the easing curve, one of the EASE_ numbers. Returns this so you can chain. */
    public Theme ease(double v) { size("ease", v); return this; }
    /** Returns how long hover fades take, below 0 follows animMs. */
    public double hoverMs() { return size("hover_ms"); }
    /** Sets how long hover fades take, below 0 follows animMs. Returns this so you can chain. */
    public Theme hoverMs(double v) { size("hover_ms", v); return this; }
    /** Returns how long press fades take, below 0 follows animMs. */
    public double pressMs() { return size("press_ms"); }
    /** Sets how long press fades take, below 0 follows animMs. Returns this so you can chain. */
    public Theme pressMs(double v) { size("press_ms", v); return this; }
    /** Returns how long popups and menus take to open, below 0 follows animMs. */
    public double popupMs() { return size("popup_ms"); }
    /** Sets how long popups and menus take to open, below 0 follows animMs. Returns this so you can chain. */
    public Theme popupMs(double v) { size("popup_ms", v); return this; }
    /** Returns how long anything that folds open or shut takes, below 0 follows animMs. */
    public double collapseMs() { return size("collapse_ms"); }
    /**
     * Sets how long anything that folds open or shut takes, below 0 follows animMs. Returns this so you can
     * chain.
     */
    public Theme collapseMs(double v) { size("collapse_ms", v); return this; }
    /** Returns how long toasts take to fade out, below 0 follows animMs. */
    public double toastFadeMs() { return size("toast_fade_ms"); }
    /** Sets how long toasts take to fade out, below 0 follows animMs. Returns this so you can chain. */
    public Theme toastFadeMs(double v) { size("toast_fade_ms", v); return this; }

    /** True if a font is set, false means the built in one. */
    public boolean hasFont() {
        return N.getLong(p() + L.at("font")) != 0;
    }

    /** The main font, null for the built in one. */
    public Font font() { return font("font"); }
    /** Sets the main font, null means the built in one. Keep the font open while the ui uses it. */
    public Theme font(Font f) { return font("font", f); }
    /** The font for headings, null means the main font. */
    public Font fontHeading() { return font("font_heading"); }
    /** Sets the font for headings, null means the main font. */
    public Theme fontHeading(Font f) { return font("font_heading", f); }
    /** The font of the text editor with EDIT_MONO, null means the built in mono one. */
    public Font fontMono() { return font("font_mono"); }
    /** Sets the font of the text editor with EDIT_MONO. */
    public Theme fontMono(Font f) { return font("font_mono", f); }
    /** The font for badges and the status bar, null means the main font. */
    public Font fontSmall() { return font("font_small"); }
    /** Sets the font for badges and the status bar. */
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

    /** The skin set under a field name like skin_button, null if none. Throws on an unknown name. */
    public Skin skin(String name) {
        at(name);
        return skins.get(name);
    }

    /** Sets a skin by field name, null takes it off. */
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

    /** The skin for one of the Ui SKIN_ numbers, null if none. */
    public Skin skin(int kind) {
        return skin(skinName(kind));
    }

    /** Sets the skin for one of the Ui SKIN_ numbers, null takes it off. */
    public Theme skin(int kind, Skin skin) {
        return skin(skinName(kind), skin);
    }

    /** The bitmap set for that icon, null if none. */
    public Bitmap icon(Icon which) {
        return icons[which.ordinal()];
    }

    /** Uses your own bitmap for an icon, null goes back to the built in one. */
    public Theme icon(Icon which, Bitmap bm) {
        N.putLong(p() + L.at("icon") + 8L * which.ordinal(), bm == null ? 0 : bm.ptr());
        icons[which.ordinal()] = bm;
        return this;
    }

    /** Whether icons are tinted, ICON_TINT or ICON_AS_IS. */
    public int iconTint() {
        return N.getInt(p() + L.at("icon_tint"));
    }

    /** Sets ICON_TINT or ICON_AS_IS. */
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
