package io.github.goomeui;

import java.util.Locale;

/**
 * The theme fields you can push with Ui.pushColor() and Ui.pushNum(), or set through Styled. Each one is a
 * color (0xAARRGGBB) or a number, color() tells which.
 */
public enum Style {
    /** The color behind everything, app mode fills with it. */
    BG,
    /** The window background color. */
    PANEL,
    /** The outline color of windows and widgets. */
    BORDER,
    /** The normal text color. */
    TEXT,
    /** The quieter text color. */
    TEXT_DIM,
    /** The accent color, used for checked boxes, slider fill and the active tab. */
    ACCENT,
    /** The color of text drawn on top of the accent. */
    ACCENT_TEXT,
    /** The widget background color while hovered. */
    HOVER,
    /** The widget background color while pressed. */
    ACTIVE,
    /** The color inside inputs and sliders. */
    INPUT_BG,
    /** The window shadow color. */
    SHADOW,
    /** The corner radius. */
    RADIUS,
    /** The space inside windows. */
    PAD,
    /** The space between widgets. */
    GAP,
    /** The outline width. */
    BORDER_WIDTH,
    /** The text size, checkboxes and knobs grow with it. */
    FONT_SIZE,
    /** How soft the shadows are. */
    SHADOW_BLUR,
    /** The animation length in ms, 0 turns animation off. */
    ANIM_MS,
    /** The focus ring and input glow color, 0 uses the accent. */
    FOCUS,
    /** The selected text color, 0 uses a faded accent. */
    SELECT,
    /** The text caret color, 0 uses the accent. */
    CARET,
    /** The link color, 0 uses the accent. */
    LINK,
    /** The badge color when none is passed, 0 uses the accent. */
    BADGE,
    /** The toast background color, 0 mixes panel and hover. */
    TOAST,
    /** The tooltip background color, 0 mixes bg and hover. */
    TOOLTIP,
    /** The color of the red close buttons, 0 uses the built in red. */
    DANGER,
    /** The color of slider and toggle knobs, 0 is white. */
    KNOB,
    /** The color drawn behind modals, 0 uses 0x73000000. */
    DIM,
    /** The size of checkbox and radio boxes. */
    CHECK_SIZE,
    /** The corner radius of checkboxes. */
    CHECK_RADIUS,
    /** The toggle track width. */
    TOGGLE_W,
    /** The toggle track height. */
    TOGGLE_H,
    /** The slider height. */
    SLIDER_H,
    /** The slider track thickness. */
    SLIDER_TRACK,
    /** The slider knob radius. */
    KNOB_SIZE,
    /** The scrollbar width. */
    SCROLL_W,
    /** The shortest a scrollbar thumb gets. */
    SCROLL_MIN,
    /** The height of every widget row, 0 means line height plus pad. */
    ITEM_H,
    /** The height of menu rows, 0 comes from the font. */
    MENU_ITEM_H,
    /** The height of tabs, 0 comes from the font. */
    TAB_H,
    /** The window title bar height, 0 comes from the font. */
    WIN_TITLE_H,
    /** The app mode title bar height, 0 comes from the font. */
    APP_TITLE_H,
    /** The app menu bar height, 0 comes from the font. */
    MENUBAR_H,
    /** The app status bar height, 0 comes from the font. */
    STATUSBAR_H,
    /** The focus ring width. */
    FOCUS_W,
    /** The space between a widget and its focus ring. */
    FOCUS_GAP,
    /** The separator thickness. */
    SEP_W,
    /** The size of the window resize grip. */
    GRIP,
    /** The tree indent. */
    INDENT,
    /** The plot line width. */
    PLOT_W,
    /** The spinner size when 0 is passed, 0 means line height. */
    SPINNER,
    /** The caret width, 0 is thin and a wide one makes a block caret. */
    CARET_W,
    /** The smallest width a window resizes to. */
    WIN_MIN_W,
    /** The smallest height a window resizes to. */
    WIN_MIN_H,
    /** The space between a combo or popup and what opened it. */
    POP_GAP,
    /** The offset of submenus. */
    MENU_GAP,
    /** The horizontal padding inside tooltips. */
    TIP_PAD_X,
    /** The vertical padding inside tooltips. */
    TIP_PAD_Y,
    /** The horizontal tooltip offset from the mouse. */
    TIP_X,
    /** The vertical tooltip offset from the mouse. */
    TIP_Y,
    /** The tooltip corner radius. */
    TIP_RADIUS,
    /** The horizontal shadow offset. */
    SHADOW_X,
    /** The vertical shadow offset. */
    SHADOW_Y,
    /** The text alignment of buttons and labels, one of the ALIGN_ numbers, auto is what each widget normally does. */
    ALIGN,
    /** The heading text size, 0 means 1.3 times the font size. */
    HEADING_SIZE,
    /** The size of badge and status bar text, 0 is a bit under the font size. */
    SMALL_SIZE,
    /** The easing curve, one of the EASE_ numbers. */
    EASE,
    /** How long hover fades take, below 0 follows animMs. */
    HOVER_MS,
    /** How long press fades take, below 0 follows animMs. */
    PRESS_MS,
    /** How long popups and menus take to open, below 0 follows animMs. */
    POPUP_MS,
    /** How long anything that folds open or shut takes, below 0 follows animMs. */
    COLLAPSE_MS,
    /** How long toasts take to fade out, below 0 follows animMs. */
    TOAST_FADE_MS;

    /** True if this style is a color, false if it is a number. */
    public boolean color() {
        return ordinal() < RADIUS.ordinal() || (ordinal() >= FOCUS.ordinal() && ordinal() <= DIM.ordinal());
    }

    /** The native field name, like text_dim. */
    public String field() {
        return name().toLowerCase(Locale.ROOT);
    }
}
