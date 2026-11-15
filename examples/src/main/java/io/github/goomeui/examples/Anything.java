package io.github.goomeui.examples;

import io.github.goomeui.Bitmap;
import io.github.goomeui.Canvas;
import io.github.goomeui.Config;
import io.github.goomeui.Event;
import io.github.goomeui.Font;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Icon;
import io.github.goomeui.Options;
import io.github.goomeui.Pacer;
import io.github.goomeui.Radii;
import io.github.goomeui.Skin;
import io.github.goomeui.Style;
import io.github.goomeui.Theme;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Anything {
    static final int RETRO = 0, SOFT = 1, HUD = 2, DENSE = 3, NLOOK = 4;
    static final String[] TABS = {"Play", "Stats", "Gear"};
    static final String[] MODES = {"Story", "Arena", "Sandbox"};
    static final String[] STARTS = {"theme_make", "Dark", "Light", "Midnight", "Paper", "Mint", "High contrast", "Rose"};
    static final String[] CODE = {"Theme t = Theme.make(u.theme(), 0xFF8B5CF6, true, 12);", "u.pushTheme(t);"};
    static final String[] NAMES = {"guest_01", "Mia", "NOVA-7", "render_pass_b"};

    static final class Card {
        int tab, level, mode;
        boolean sound, online;
        double volume;
        String name;
    }

    final Card[] cards = new Card[NLOOK + 1];
    final Theme[] looks = new Theme[NLOOK];
    final Theme[] starts = new Theme[STARTS.length];
    final List<Bitmap> sprites = new ArrayList<>();
    final Bitmap[] term = new Bitmap[7], hud = new Bitmap[8];
    Bitmap frame, plate, tab;
    Font mono, bold;
    int start;
    boolean focusOnce = true, running = true;
    String pass = "hunter22", code = "", server = "eu-west-2";
    double zoom = 1;

    Bitmap sprite(int w, int h, Consumer<Canvas> draw) {
        try (Canvas c = Canvas.create(w, h)) {
            draw.accept(c);
            IntBuffer px = c.pixels();
            int[] a = new int[w * h];
            px.get(0, a);
            Bitmap b = Bitmap.wrap(a, w, h, w);
            sprites.add(b);
            return b;
        }
    }

    static void diamond(Canvas c, double cx, double cy, double r, int col) {
        c.poly(new double[] {cx, cy - r, cx + r, cy, cx, cy + r, cx - r, cy}, col);
    }

    void termIcons() {
        term[0] = sprite(12, 12, c -> c.rect(2, 2, 8, 8, 0xFFFFFFFF));
        term[1] = sprite(10, 10, c -> c.rect(2, 2, 6, 6, 0xFFFFFFFF));
        term[2] = sprite(12, 12, c -> c.rect(0, 0, 12, 12, 0xFFFFFFFF));
        term[3] = sprite(12, 12, c -> {
            c.rect(1, 3, 10, 2, 0xFFFFFFFF);
            c.rect(3, 5, 6, 2, 0xFFFFFFFF);
            c.rect(5, 7, 2, 2, 0xFFFFFFFF);
        });
        term[4] = sprite(12, 12, c -> {
            c.rect(3, 1, 2, 10, 0xFFFFFFFF);
            c.rect(5, 3, 2, 6, 0xFFFFFFFF);
            c.rect(7, 5, 2, 2, 0xFFFFFFFF);
        });
        term[5] = sprite(12, 12, c -> {
            c.rect(0, 0, 12, 12, 0xFFFFFFFF);
            c.rect(3, 3, 6, 6, 0xFF000000);
        });
        term[6] = sprite(12, 12, c -> {
            c.rect(0, 0, 12, 12, 0xFF1FA64C);
            c.rect(1, 1, 10, 10, 0xFF031408);
        });
    }

    void hudArt() {
        hud[0] = sprite(16, 16, c -> diamond(c, 8, 8, 6, 0xFFFFFFFF));
        hud[1] = sprite(16, 16, c -> diamond(c, 8, 8, 5, 0xFFFFFFFF));
        hud[2] = sprite(20, 20, c -> {
            diamond(c, 10, 10, 9, 0xFFFFFFFF);
            diamond(c, 10, 10, 5.5, 0xFF0A1A24);
        });
        hud[3] = sprite(12, 12, c -> c.triangle(1, 3, 11, 3, 6, 10, 0xFFFFFFFF));
        hud[4] = sprite(12, 12, c -> c.triangle(3, 1, 10, 6, 3, 11, 0xFFFFFFFF));
        hud[5] = sprite(12, 12, c -> {
            c.line(2, 2, 10, 10, 1.6, 0xFFFFFFFF);
            c.line(10, 2, 2, 10, 1.6, 0xFFFFFFFF);
        });
        hud[6] = sprite(18, 18, c -> {
            c.rect(0, 0, 18, 18, 0xC0081C28);
            c.frame(0.5, 0.5, 17, 17, 1.2, 0xFF39E1FF);
        });
        hud[7] = sprite(18, 18, c -> {
            diamond(c, 9, 9, 8.6, 0xFF39E1FF);
            diamond(c, 9, 9, 7.2, 0xF0081C28);
        });
        frame = sprite(24, 24, c -> {
            c.rect(0, 0, 24, 24, 0xA00A1622);
            c.frame(0.5, 0.5, 23, 23, 1, 0x5539E1FF);
            c.rect(0, 0, 8, 2, 0xFF39E1FF);
            c.rect(0, 0, 2, 8, 0xFF39E1FF);
            c.rect(16, 22, 8, 2, 0xFF39E1FF);
            c.rect(22, 16, 2, 8, 0xFF39E1FF);
        });
        plate = sprite(24, 24, c -> {
            c.poly(new double[] {6, 0, 24, 0, 24, 18, 18, 24, 0, 24, 0, 6}, 0xC0103346);
            c.poly(new double[] {6.8, 1, 23, 1, 23, 17.6, 17.6, 23, 1, 23, 1, 6.8}, 0xFF0D2533);
            c.line(6, 0.5, 23.5, 0.5, 1, 0xFF39E1FF);
            c.line(0.5, 6, 0.5, 23.5, 1, 0x9939E1FF);
            c.line(0.5, 6, 6, 0.5, 1, 0xFF39E1FF);
        });
        tab = sprite(24, 24, c -> c.poly(new double[] {5, 0, 24, 0, 24, 19, 19, 24, 0, 24, 0, 5}, 0xFF39E1FF));
    }

    Theme retro() {
        Theme t = Theme.preset(Theme.PRESET_DARK);
        t.bg(0xFF000000).panel(0xFF020A04).border(0xFF1F7A3A).text(0xFF3DFF7A).textDim(0xFF1FA64C);
        t.accent(0xFF3DFF7A).accentText(0xFF001A08).hover(0xFF0A2A14).active(0xFF124422).inputBg(0xFF031408);
        t.shadow(0x5533FF77).shadowBlur(18).shadowY(0);
        t.radius(0).checkRadius(0).tipRadius(0).borderWidth(1).animMs(0).ease(Theme.EASE_LINEAR);
        t.font(mono).fontMono(mono).fontHeading(mono).fontSize(15).headingSize(17);
        t.caretW(9).caret(0xB03DFF7A).select(0x663DFF7A).knob(0xFF3DFF7A).focusW(1).focusGap(2);
        t.toggleW(34).toggleH(18).sliderTrack(2).knobSize(6);
        t.icon(Icon.CHECK, term[0]).icon(Icon.RADIO_DOT, term[1]).icon(Icon.SLIDER_KNOB, term[2]).icon(Icon.TOGGLE_KNOB, term[5]);
        t.icon(Icon.ARROW_DOWN, term[3]).icon(Icon.ARROW_RIGHT, term[4]);
        Skin box = new Skin(term[6], 3);
        t.skin(Ui.SKIN_CHECKBOX, box).skin(Ui.SKIN_RADIO, box).skin(Ui.SKIN_TOGGLE, box);
        return t;
    }

    static Theme soft() {
        Theme t = Theme.make(0xFF2F7BF6, false, 20);
        t.bg(0xFFF2F4F8).panel(0xFFFFFFFF).border(0x14000000).inputBg(0xFFEEF1F6).hover(0xFFE3E8F0);
        t.fontSize(17).headingSize(22).pad(14).gap(10).itemH(44).tabH(42).winTitleH(48);
        t.checkSize(22).checkRadius(11).toggleW(46).toggleH(28).sliderH(28).knobSize(12).sliderTrack(8);
        t.shadow(0x22203060).shadowBlur(34).shadowY(10).focusW(3).focusGap(4);
        t.animMs(260).hoverMs(200).pressMs(160).popupMs(320).ease(Theme.EASE_BACK);
        return t;
    }

    Theme hud() {
        Theme t = Theme.preset(Theme.PRESET_DARK);
        t.bg(0x00000000).panel(0x8A06121C).border(0x6639E1FF).text(0xFFE6FBFF).textDim(0xFF7FC4D6);
        t.accent(0xFF39E1FF).accentText(0xFF041820).hover(0xFF12384A).active(0xFF1A4C63).inputBg(0xB0081C28);
        t.shadow(0x5539E1FF).shadowBlur(26).shadowY(0).radius(0).checkRadius(0).tipRadius(0);
        if (bold != null) t.font(bold);
        t.fontSize(16).headingSize(22).itemH(38).winTitleH(42);
        t.knob(0xFF39E1FF).knobSize(9).sliderTrack(4).checkSize(18).ease(Theme.EASE_OUT).animMs(160);
        t.skin(Ui.SKIN_PANEL, new Skin(frame, 8)).skin(Ui.SKIN_BUTTON, new Skin(plate, 8)).skin(Ui.SKIN_INPUT, new Skin(plate, 8));
        t.skin(Ui.SKIN_TAB, new Skin(tab, 7)).skin(Ui.SKIN_CHECKBOX, new Skin(hud[6], 3)).skin(Ui.SKIN_RADIO, new Skin(hud[7], 0));
        t.icon(Icon.CHECK, hud[0]).icon(Icon.RADIO_DOT, hud[1]).icon(Icon.SLIDER_KNOB, hud[2]).icon(Icon.TOGGLE_KNOB, hud[2]);
        t.icon(Icon.ARROW_DOWN, hud[3]).icon(Icon.ARROW_RIGHT, hud[4]).icon(Icon.CLOSE, hud[5]);
        return t;
    }

    static Theme dense() {
        Theme t = Theme.preset(Theme.PRESET_DARK);
        t.bg(0xFF1B1C1F).panel(0xFF232428).border(0xFF34363C).text(0xFFD5D7DC).textDim(0xFF8A8F98);
        t.accent(0xFF4C8DF6).accentText(0xFFFFFFFF).hover(0xFF2E3036).active(0xFF383B42).inputBg(0xFF1A1B1E);
        t.shadow(0x66000000).shadowBlur(10).shadowY(3).radius(3).checkRadius(2).tipRadius(3);
        t.fontSize(12).headingSize(13).pad(6).gap(6).itemH(24).tabH(24).winTitleH(26);
        t.checkSize(13).toggleW(26).toggleH(14).sliderH(14).knobSize(5).sliderTrack(3);
        t.scrollW(3).focusW(1).focusGap(1).animMs(70).tipPadX(6).tipPadY(4);
        return t;
    }

    static void widgets(Ui u, Card c, boolean big, boolean focus) {
        c.tab = u.tabs("tabs", c.tab, TABS);
        u.heading("Player one");
        if (focus) u.setFocus();
        c.name = u.input("Name", c.name, 48);
        c.volume = u.slider("Volume", c.volume, 0, 100, "%.0f");
        u.row(2);
        c.sound = u.checkbox("Sound", c.sound);
        c.online = u.toggle("Online", c.online);
        c.level = u.radio("Easy", c.level, 0);
        u.sameLine();
        c.level = u.radio("Normal", c.level, 1);
        u.sameLine();
        c.level = u.radio("Hard", c.level, 2);
        if (!big) {
            c.mode = u.combo("Mode", c.mode, MODES);
            u.progress(c.volume / 100, "loading");
        }
        u.row(2);
        u.button("Start");
        u.button("Quit");
    }

    static void scene(Ui u, double x, double y, double w, double h) {
        Canvas c = u.canvas();
        if (c == null) return;
        c.rrectGrad(x, y, w, h, Radii.of(14), 0xFF1B2747, 0xFFE0794F, false);
        c.circle(x + w * 0.72, y + h * 0.62, 46, 0xFFFFC27A);
        c.poly(new double[] {x, y + h, x, y + h * 0.7, x + w * 0.22, y + h * 0.56, x + w * 0.45, y + h * 0.74, x + w * 0.7, y + h * 0.6, x + w, y + h * 0.72,
            x + w, y + h}, 0xFF1A1F38);
        c.poly(new double[] {x, y + h, x, y + h * 0.86, x + w * 0.3, y + h * 0.8, x + w * 0.62, y + h * 0.9, x + w, y + h * 0.82, x + w, y + h}, 0xFF0D1022);
    }

    void look(Ui u, int k, String title, double x, double y, double w, double h) {
        u.pushTheme(looks[k]);
        u.scale(zoom);
        u.pushId(title);
        if (u.window(title, new Window(x, y, w, h), 0)) {
            widgets(u, cards[k], k == SOFT || k == HUD, k == RETRO && focusOnce);
            if (k == RETRO) focusOnce = false;
        }
        u.windowEnd();
        u.popId();
        u.pop();
    }

    void simple(Ui u, double x, double y, double w, double h) {
        u.pushTheme(starts[start]);
        u.scale(zoom);
        if (u.window("The simple path", new Window(x, y, w, h), 0)) {
            u.wrapped("two lines, one accent color, dark or light and a corner radius:");
            u.pushFont(mono);
            u.pushNum(Style.FONT_SIZE, 12);
            for (String line : CODE) u.label(line);
            u.pop(2);
            u.space(4);
            start = u.combo("Start from", start, STARTS);
            zoom = u.slider("UI size", zoom, 0.8, 1.3, "%.2fx");
            u.separator();
            Card c = cards[NLOOK];
            u.row(2);
            c.name = u.input("Name", c.name, 48);
            pass = u.inputEx("Password", pass, 32, Ui.INPUT_PASSWORD | Ui.INPUT_ENTER_RETURNS, "8 letters or more");
            if (u.entered()) u.toast("logged in", 1500);
            u.row(2);
            code = u.inputEx("Room code", code, 8, Ui.INPUT_NUMBER, "numbers only");
            server = u.inputEx("Server", server, 16, Ui.INPUT_READONLY, null);
            u.row(2);
            c.sound = u.checkbox("Sound", c.sound);
            c.online = u.toggle("Online", c.online);
            u.separator();
            u.label("one push changes one widget");
            u.row(3);
            u.pushNum(Style.RADIUS, 0);
            u.button("Square");
            u.pop();
            u.pushNum(Style.RADIUS, 99);
            u.pushNum(Style.ALIGN, Theme.ALIGN_LEFT);
            u.button("Pill");
            u.pop(2);
            u.pushColor(Style.ACCENT, 0xFFF59E0B);
            u.pushNum(Style.CHECK_SIZE, 24);
            u.pushNum(Style.CHECK_RADIUS, 12);
            c.sound = u.checkbox("Big", c.sound);
            u.pop(3);
            u.separator();
            u.label("every preset, same widgets");
            for (int i = 0; i < Theme.PRESET_COUNT; i++) {
                u.pushTheme(starts[i + 1]);
                u.pushId(STARTS[i + 1]);
                u.row(3);
                u.badge(STARTS[i + 1], 0);
                u.button("Open");
                u.toggle("##t", (i & 1) != 0);
                u.popId();
                u.pop();
            }
        }
        u.windowEnd();
        u.pop();
    }

    void setup(Ui u) {
        mono = Font.system("monospace");
        bold = Font.system("sans-serif:bold");
        termIcons();
        hudArt();
        looks[RETRO] = retro();
        looks[SOFT] = soft();
        looks[HUD] = hud();
        looks[DENSE] = dense();
        for (Theme t : looks) if (!t.hasFont()) t.font(u.theme().font());
        starts[0] = Theme.make(u.theme(), 0xFF8B5CF6, true, 12);
        for (int i = 0; i < Theme.PRESET_COUNT; i++) starts[i + 1] = Theme.preset(u.theme(), i);
        for (int i = 0; i <= NLOOK; i++) {
            Card c = cards[i] = new Card();
            c.sound = true;
            c.online = i != DENSE;
            c.level = 1 + (i & 1);
            c.volume = 35 + i * 12;
            c.mode = i % 3;
            c.tab = i % 3;
            c.name = i < NLOOK ? NAMES[i] : "sev";
        }
        Config cfg = u.config();
        cfg.tipDelayMs(300).cursor(Config.CUR_BUTTON, Goome.CURSOR_HAND);
        cfg.bind(Config.ACT_SELECT_ALL, "ctrl+a super+a ctrl+l");
    }

    void frame(Ui u) {
        u.app("anything", Ui.APP_NO_PAD);
        scene(u, 452, 496, 420, 428);
        u.appEnd();
        look(u, RETRO, "> retro terminal_", 16, 16, 420, 0);
        look(u, SOFT, "Soft and round", 452, 16, 420, 0);
        look(u, HUD, "GAME HUD", 476, 520, 372, 0);
        look(u, DENSE, "dense pro tool", 16, 456, 420, 0);
        simple(u, 888, 16, 416, 908);
    }

    public static void main(String[] args) {
        Goome g;
        try {
            g = Goome.open(new Options().title("goome can look like anything").mode(Goome.MODE_WINDOW).size(1320, 940).minSize(900, 600)
                .windowFlags(Goome.WINDOW_RESIZABLE | Goome.WINDOW_CENTER));
        } catch (GoomeException e) {
            System.err.println("cant open a window: " + e.getMessage());
            System.exit(1);
            return;
        }
        Anything a = new Anything();
        try (g; Ui u = g.ui(); Pacer pace = new Pacer(60)) {
            a.setup(u);
            boolean test = g.backendName().startsWith("test");
            while (a.running) {
                int wait = test || u.animating() ? pace.next() : 500;
                for (Event e = g.poll(wait); e != null; e = g.poll(0)) {
                    if (e.type() == Goome.QUIT || e.type() == Goome.CLOSE) a.running = false;
                    u.feed(e);
                }
                if (!a.running) break;
                if (u.begin(0) == null) continue;
                try {
                    a.frame(u);
                } finally {
                    u.end();
                }
            }
        } finally {
            for (Bitmap b : a.sprites) b.close();
            if (a.mono != null) a.mono.close();
            if (a.bold != null) a.bold.close();
        }
    }
}
