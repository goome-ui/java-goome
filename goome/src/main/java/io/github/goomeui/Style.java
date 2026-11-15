package io.github.goomeui;

import java.util.Locale;

public enum Style {
    BG, PANEL, BORDER, TEXT, TEXT_DIM, ACCENT, ACCENT_TEXT, HOVER, ACTIVE, INPUT_BG, SHADOW,
    RADIUS, PAD, GAP, BORDER_WIDTH, FONT_SIZE, SHADOW_BLUR, ANIM_MS,
    FOCUS, SELECT, CARET, LINK, BADGE, TOAST, TOOLTIP, DANGER, KNOB, DIM,
    CHECK_SIZE, CHECK_RADIUS, TOGGLE_W, TOGGLE_H, SLIDER_H, SLIDER_TRACK, KNOB_SIZE,
    SCROLL_W, SCROLL_MIN, ITEM_H, MENU_ITEM_H, TAB_H, WIN_TITLE_H, APP_TITLE_H, MENUBAR_H, STATUSBAR_H,
    FOCUS_W, FOCUS_GAP, SEP_W, GRIP, INDENT, PLOT_W, SPINNER, CARET_W,
    WIN_MIN_W, WIN_MIN_H, POP_GAP, MENU_GAP, TIP_PAD_X, TIP_PAD_Y, TIP_X, TIP_Y, TIP_RADIUS,
    SHADOW_X, SHADOW_Y, ALIGN, HEADING_SIZE, SMALL_SIZE, EASE,
    HOVER_MS, PRESS_MS, POPUP_MS, COLLAPSE_MS, TOAST_FADE_MS;

    public boolean color() {
        return ordinal() < RADIUS.ordinal() || (ordinal() >= FOCUS.ordinal() && ordinal() <= DIM.ordinal());
    }

    public String field() {
        return name().toLowerCase(Locale.ROOT);
    }
}
