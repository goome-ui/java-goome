package io.github.goomeui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A chained set of theme pushes from Ui.style(). Set what you want, call push(), and close it to pop them
 * again, so use it in try with resources.
 */
public final class Styled implements AutoCloseable {
    private final Ui ui;
    private final List<Consumer<Ui>> todo = new ArrayList<>();
    private int pushed;

    Styled(Ui ui) {
        this.ui = ui;
    }

    /** Queues a color change. Throws if the Style is a number. */
    public Styled color(Style what, int argb) {
        if (!what.color()) throw new IllegalArgumentException(what + " isnt a color, use num");
        todo.add(u -> u.pushColor(what, argb));
        return this;
    }

    /** Queues a number change. Throws if the Style is a color. */
    public Styled num(Style what, double v) {
        if (what.color()) throw new IllegalArgumentException(what + " is a color, use color");
        todo.add(u -> u.pushNum(what, v));
        return this;
    }

    /** Queues the color behind everything, app mode fills with it. */
    public Styled bg(int v) { return color(Style.BG, v); }
    /** Queues the window background color. */
    public Styled panel(int v) { return color(Style.PANEL, v); }
    /** Queues the outline color of windows and widgets. */
    public Styled border(int v) { return color(Style.BORDER, v); }
    /** Queues the normal text color. */
    public Styled text(int v) { return color(Style.TEXT, v); }
    /** Queues the quieter text color. */
    public Styled textDim(int v) { return color(Style.TEXT_DIM, v); }
    /** Queues the accent color, used for checked boxes, slider fill and the active tab. */
    public Styled accent(int v) { return color(Style.ACCENT, v); }
    /** Queues the color of text drawn on top of the accent. */
    public Styled accentText(int v) { return color(Style.ACCENT_TEXT, v); }
    /** Queues the widget background color while hovered. */
    public Styled hover(int v) { return color(Style.HOVER, v); }
    /** Queues the widget background color while pressed. */
    public Styled active(int v) { return color(Style.ACTIVE, v); }
    /** Queues the color inside inputs and sliders. */
    public Styled inputBg(int v) { return color(Style.INPUT_BG, v); }
    /** Queues the window shadow color. */
    public Styled shadow(int v) { return color(Style.SHADOW, v); }

    /** Queues the corner radius. */
    public Styled radius(double v) { return num(Style.RADIUS, v); }
    /** Queues the space inside windows. */
    public Styled pad(double v) { return num(Style.PAD, v); }
    /** Queues the space between widgets. */
    public Styled gap(double v) { return num(Style.GAP, v); }
    /** Queues the outline width. */
    public Styled borderWidth(double v) { return num(Style.BORDER_WIDTH, v); }
    /** Queues the text size, checkboxes and knobs grow with it. */
    public Styled fontSize(double v) { return num(Style.FONT_SIZE, v); }
    /** Queues how soft the shadows are. */
    public Styled shadowBlur(double v) { return num(Style.SHADOW_BLUR, v); }
    /** Queues the animation length in ms, 0 turns animation off. */
    public Styled animMs(double v) { return num(Style.ANIM_MS, v); }

    /** Queues the focus ring and input glow color, 0 uses the accent. */
    public Styled focus(int v) { return color(Style.FOCUS, v); }
    /** Queues the selected text color, 0 uses a faded accent. */
    public Styled select(int v) { return color(Style.SELECT, v); }
    /** Queues the text caret color, 0 uses the accent. */
    public Styled caret(int v) { return color(Style.CARET, v); }
    /** Queues the link color, 0 uses the accent. */
    public Styled link(int v) { return color(Style.LINK, v); }
    /** Queues the badge color when none is passed, 0 uses the accent. */
    public Styled badge(int v) { return color(Style.BADGE, v); }
    /** Queues the toast background color, 0 mixes panel and hover. */
    public Styled toast(int v) { return color(Style.TOAST, v); }
    /** Queues the tooltip background color, 0 mixes bg and hover. */
    public Styled tooltip(int v) { return color(Style.TOOLTIP, v); }
    /** Queues the color of the red close buttons, 0 uses the built in red. */
    public Styled danger(int v) { return color(Style.DANGER, v); }
    /** Queues the color of slider and toggle knobs, 0 is white. */
    public Styled knob(int v) { return color(Style.KNOB, v); }
    /** Queues the color drawn behind modals, 0 uses 0x73000000. */
    public Styled dim(int v) { return color(Style.DIM, v); }

    /** Queues the size of checkbox and radio boxes. */
    public Styled checkSize(double v) { return num(Style.CHECK_SIZE, v); }
    /** Queues the corner radius of checkboxes. */
    public Styled checkRadius(double v) { return num(Style.CHECK_RADIUS, v); }
    /** Queues the toggle track width. */
    public Styled toggleW(double v) { return num(Style.TOGGLE_W, v); }
    /** Queues the toggle track height. */
    public Styled toggleH(double v) { return num(Style.TOGGLE_H, v); }
    /** Queues the slider height. */
    public Styled sliderH(double v) { return num(Style.SLIDER_H, v); }
    /** Queues the slider track thickness. */
    public Styled sliderTrack(double v) { return num(Style.SLIDER_TRACK, v); }
    /** Queues the slider knob radius. */
    public Styled knobSize(double v) { return num(Style.KNOB_SIZE, v); }
    /** Queues the scrollbar width. */
    public Styled scrollW(double v) { return num(Style.SCROLL_W, v); }
    /** Queues the shortest a scrollbar thumb gets. */
    public Styled scrollMin(double v) { return num(Style.SCROLL_MIN, v); }
    /** Queues the height of every widget row, 0 means line height plus pad. */
    public Styled itemH(double v) { return num(Style.ITEM_H, v); }
    /** Queues the height of menu rows, 0 comes from the font. */
    public Styled menuItemH(double v) { return num(Style.MENU_ITEM_H, v); }
    /** Queues the height of tabs, 0 comes from the font. */
    public Styled tabH(double v) { return num(Style.TAB_H, v); }
    /** Queues the window title bar height, 0 comes from the font. */
    public Styled winTitleH(double v) { return num(Style.WIN_TITLE_H, v); }
    /** Queues the app mode title bar height, 0 comes from the font. */
    public Styled appTitleH(double v) { return num(Style.APP_TITLE_H, v); }
    /** Queues the app menu bar height, 0 comes from the font. */
    public Styled menubarH(double v) { return num(Style.MENUBAR_H, v); }
    /** Queues the app status bar height, 0 comes from the font. */
    public Styled statusbarH(double v) { return num(Style.STATUSBAR_H, v); }
    /** Queues the focus ring width. */
    public Styled focusW(double v) { return num(Style.FOCUS_W, v); }
    /** Queues the space between a widget and its focus ring. */
    public Styled focusGap(double v) { return num(Style.FOCUS_GAP, v); }
    /** Queues the separator thickness. */
    public Styled sepW(double v) { return num(Style.SEP_W, v); }
    /** Queues the size of the window resize grip. */
    public Styled grip(double v) { return num(Style.GRIP, v); }
    /** Queues the tree indent. */
    public Styled indent(double v) { return num(Style.INDENT, v); }
    /** Queues the plot line width. */
    public Styled plotW(double v) { return num(Style.PLOT_W, v); }
    /** Queues the spinner size when 0 is passed, 0 means line height. */
    public Styled spinner(double v) { return num(Style.SPINNER, v); }
    /** Queues the caret width, 0 is thin and a wide one makes a block caret. */
    public Styled caretW(double v) { return num(Style.CARET_W, v); }
    /** Queues the smallest width a window resizes to. */
    public Styled winMinW(double v) { return num(Style.WIN_MIN_W, v); }
    /** Queues the smallest height a window resizes to. */
    public Styled winMinH(double v) { return num(Style.WIN_MIN_H, v); }
    /** Queues the space between a combo or popup and what opened it. */
    public Styled popGap(double v) { return num(Style.POP_GAP, v); }
    /** Queues the offset of submenus. */
    public Styled menuGap(double v) { return num(Style.MENU_GAP, v); }
    /** Queues the horizontal padding inside tooltips. */
    public Styled tipPadX(double v) { return num(Style.TIP_PAD_X, v); }
    /** Queues the vertical padding inside tooltips. */
    public Styled tipPadY(double v) { return num(Style.TIP_PAD_Y, v); }
    /** Queues the horizontal tooltip offset from the mouse. */
    public Styled tipX(double v) { return num(Style.TIP_X, v); }
    /** Queues the vertical tooltip offset from the mouse. */
    public Styled tipY(double v) { return num(Style.TIP_Y, v); }
    /** Queues the tooltip corner radius. */
    public Styled tipRadius(double v) { return num(Style.TIP_RADIUS, v); }
    /** Queues the horizontal shadow offset. */
    public Styled shadowX(double v) { return num(Style.SHADOW_X, v); }
    /** Queues the vertical shadow offset. */
    public Styled shadowY(double v) { return num(Style.SHADOW_Y, v); }
    /**
     * Queues the text alignment of buttons and labels, one of the ALIGN_ numbers, auto is what each widget
     * normally does.
     */
    public Styled align(double v) { return num(Style.ALIGN, v); }
    /** Queues the heading text size, 0 means 1.3 times the font size. */
    public Styled headingSize(double v) { return num(Style.HEADING_SIZE, v); }
    /** Queues the size of badge and status bar text, 0 is a bit under the font size. */
    public Styled smallSize(double v) { return num(Style.SMALL_SIZE, v); }
    /** Queues the easing curve, one of the EASE_ numbers. */
    public Styled ease(double v) { return num(Style.EASE, v); }
    /** Queues how long hover fades take, below 0 follows animMs. */
    public Styled hoverMs(double v) { return num(Style.HOVER_MS, v); }
    /** Queues how long press fades take, below 0 follows animMs. */
    public Styled pressMs(double v) { return num(Style.PRESS_MS, v); }
    /** Queues how long popups and menus take to open, below 0 follows animMs. */
    public Styled popupMs(double v) { return num(Style.POPUP_MS, v); }
    /** Queues how long anything that folds open or shut takes, below 0 follows animMs. */
    public Styled collapseMs(double v) { return num(Style.COLLAPSE_MS, v); }
    /** Queues how long toasts take to fade out, below 0 follows animMs. */
    public Styled toastFadeMs(double v) { return num(Style.TOAST_FADE_MS, v); }

    /** Queues per corner radii. */
    public Styled radii(Radii r) {
        todo.add(u -> u.pushRadii(r));
        return this;
    }

    /** Queues per corner radii, top left, top right, bottom right, bottom left. */
    public Styled radii(double tl, double tr, double br, double bl) {
        return radii(new Radii(tl, tr, br, bl));
    }

    /** Queues a font, null means the built in one. */
    public Styled font(Font f) {
        todo.add(u -> u.pushFont(f));
        return this;
    }

    /** Queues a skin for one of the Ui SKIN_ numbers. */
    public Styled skin(int kind, Skin s) {
        todo.add(u -> u.pushSkin(kind, s));
        return this;
    }

    /** Queues your own bitmap for an icon. */
    public Styled icon(Icon which, Bitmap bm) {
        todo.add(u -> u.pushIcon(which, bm));
        return this;
    }

    /** Queues a whole theme. */
    public Styled theme(Theme t) {
        todo.add(u -> u.pushTheme(t));
        return this;
    }

    /** Does the queued pushes now. Returns this. */
    public Styled push() {
        for (Consumer<Ui> f : todo) {
            f.accept(ui);
            pushed++;
        }
        todo.clear();
        return this;
    }

    /** How many pushes it has done and not popped yet. */
    public int pushed() {
        return pushed;
    }

    /** Pops what push() did. */
    @Override
    public void close() {
        if (pushed > 0 && !ui.closed()) ui.pop(pushed);
        pushed = 0;
    }
}
