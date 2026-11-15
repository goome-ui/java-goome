package io.github.goomeui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Styled implements AutoCloseable {
    private final Ui ui;
    private final List<Consumer<Ui>> todo = new ArrayList<>();
    private int pushed;

    Styled(Ui ui) {
        this.ui = ui;
    }

    public Styled color(Style what, int argb) {
        if (!what.color()) throw new IllegalArgumentException(what + " isnt a color, use num");
        todo.add(u -> u.pushColor(what, argb));
        return this;
    }

    public Styled num(Style what, double v) {
        if (what.color()) throw new IllegalArgumentException(what + " is a color, use color");
        todo.add(u -> u.pushNum(what, v));
        return this;
    }

    public Styled bg(int v) { return color(Style.BG, v); }
    public Styled panel(int v) { return color(Style.PANEL, v); }
    public Styled border(int v) { return color(Style.BORDER, v); }
    public Styled text(int v) { return color(Style.TEXT, v); }
    public Styled textDim(int v) { return color(Style.TEXT_DIM, v); }
    public Styled accent(int v) { return color(Style.ACCENT, v); }
    public Styled accentText(int v) { return color(Style.ACCENT_TEXT, v); }
    public Styled hover(int v) { return color(Style.HOVER, v); }
    public Styled active(int v) { return color(Style.ACTIVE, v); }
    public Styled inputBg(int v) { return color(Style.INPUT_BG, v); }
    public Styled shadow(int v) { return color(Style.SHADOW, v); }

    public Styled radius(double v) { return num(Style.RADIUS, v); }
    public Styled pad(double v) { return num(Style.PAD, v); }
    public Styled gap(double v) { return num(Style.GAP, v); }
    public Styled borderWidth(double v) { return num(Style.BORDER_WIDTH, v); }
    public Styled fontSize(double v) { return num(Style.FONT_SIZE, v); }
    public Styled shadowBlur(double v) { return num(Style.SHADOW_BLUR, v); }
    public Styled animMs(double v) { return num(Style.ANIM_MS, v); }

    public Styled focus(int v) { return color(Style.FOCUS, v); }
    public Styled select(int v) { return color(Style.SELECT, v); }
    public Styled caret(int v) { return color(Style.CARET, v); }
    public Styled link(int v) { return color(Style.LINK, v); }
    public Styled badge(int v) { return color(Style.BADGE, v); }
    public Styled toast(int v) { return color(Style.TOAST, v); }
    public Styled tooltip(int v) { return color(Style.TOOLTIP, v); }
    public Styled danger(int v) { return color(Style.DANGER, v); }
    public Styled knob(int v) { return color(Style.KNOB, v); }
    public Styled dim(int v) { return color(Style.DIM, v); }

    public Styled checkSize(double v) { return num(Style.CHECK_SIZE, v); }
    public Styled checkRadius(double v) { return num(Style.CHECK_RADIUS, v); }
    public Styled toggleW(double v) { return num(Style.TOGGLE_W, v); }
    public Styled toggleH(double v) { return num(Style.TOGGLE_H, v); }
    public Styled sliderH(double v) { return num(Style.SLIDER_H, v); }
    public Styled sliderTrack(double v) { return num(Style.SLIDER_TRACK, v); }
    public Styled knobSize(double v) { return num(Style.KNOB_SIZE, v); }
    public Styled scrollW(double v) { return num(Style.SCROLL_W, v); }
    public Styled scrollMin(double v) { return num(Style.SCROLL_MIN, v); }
    public Styled itemH(double v) { return num(Style.ITEM_H, v); }
    public Styled menuItemH(double v) { return num(Style.MENU_ITEM_H, v); }
    public Styled tabH(double v) { return num(Style.TAB_H, v); }
    public Styled winTitleH(double v) { return num(Style.WIN_TITLE_H, v); }
    public Styled appTitleH(double v) { return num(Style.APP_TITLE_H, v); }
    public Styled menubarH(double v) { return num(Style.MENUBAR_H, v); }
    public Styled statusbarH(double v) { return num(Style.STATUSBAR_H, v); }
    public Styled focusW(double v) { return num(Style.FOCUS_W, v); }
    public Styled focusGap(double v) { return num(Style.FOCUS_GAP, v); }
    public Styled sepW(double v) { return num(Style.SEP_W, v); }
    public Styled grip(double v) { return num(Style.GRIP, v); }
    public Styled indent(double v) { return num(Style.INDENT, v); }
    public Styled plotW(double v) { return num(Style.PLOT_W, v); }
    public Styled spinner(double v) { return num(Style.SPINNER, v); }
    public Styled caretW(double v) { return num(Style.CARET_W, v); }
    public Styled winMinW(double v) { return num(Style.WIN_MIN_W, v); }
    public Styled winMinH(double v) { return num(Style.WIN_MIN_H, v); }
    public Styled popGap(double v) { return num(Style.POP_GAP, v); }
    public Styled menuGap(double v) { return num(Style.MENU_GAP, v); }
    public Styled tipPadX(double v) { return num(Style.TIP_PAD_X, v); }
    public Styled tipPadY(double v) { return num(Style.TIP_PAD_Y, v); }
    public Styled tipX(double v) { return num(Style.TIP_X, v); }
    public Styled tipY(double v) { return num(Style.TIP_Y, v); }
    public Styled tipRadius(double v) { return num(Style.TIP_RADIUS, v); }
    public Styled shadowX(double v) { return num(Style.SHADOW_X, v); }
    public Styled shadowY(double v) { return num(Style.SHADOW_Y, v); }
    public Styled align(double v) { return num(Style.ALIGN, v); }
    public Styled headingSize(double v) { return num(Style.HEADING_SIZE, v); }
    public Styled smallSize(double v) { return num(Style.SMALL_SIZE, v); }
    public Styled ease(double v) { return num(Style.EASE, v); }
    public Styled hoverMs(double v) { return num(Style.HOVER_MS, v); }
    public Styled pressMs(double v) { return num(Style.PRESS_MS, v); }
    public Styled popupMs(double v) { return num(Style.POPUP_MS, v); }
    public Styled collapseMs(double v) { return num(Style.COLLAPSE_MS, v); }
    public Styled toastFadeMs(double v) { return num(Style.TOAST_FADE_MS, v); }

    public Styled radii(Radii r) {
        todo.add(u -> u.pushRadii(r));
        return this;
    }

    public Styled radii(double tl, double tr, double br, double bl) {
        return radii(new Radii(tl, tr, br, bl));
    }

    public Styled font(Font f) {
        todo.add(u -> u.pushFont(f));
        return this;
    }

    public Styled skin(int kind, Skin s) {
        todo.add(u -> u.pushSkin(kind, s));
        return this;
    }

    public Styled icon(Icon which, Bitmap bm) {
        todo.add(u -> u.pushIcon(which, bm));
        return this;
    }

    public Styled theme(Theme t) {
        todo.add(u -> u.pushTheme(t));
        return this;
    }

    public Styled push() {
        for (Consumer<Ui> f : todo) {
            f.accept(ui);
            pushed++;
        }
        todo.clear();
        return this;
    }

    public int pushed() {
        return pushed;
    }

    @Override
    public void close() {
        if (pushed > 0 && !ui.closed()) ui.pop(pushed);
        pushed = 0;
    }
}
