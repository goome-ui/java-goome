package io.github.goomeui.examples;

import io.github.goomeui.Canvas;
import io.github.goomeui.Event;
import io.github.goomeui.Font;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Item;
import io.github.goomeui.ItemFlag;
import io.github.goomeui.Options;
import io.github.goomeui.Pacer;
import io.github.goomeui.TextSize;
import io.github.goomeui.Theme;
import io.github.goomeui.Ui;

public final class Anim {
    static final String[] ROW_NAMES = {"linear", "in", "out", "in out", "smooth", "back", "spring", "bounce"};
    static final int[] ROW_EASES = {Theme.EASE_LINEAR, Theme.EASE_IN, Theme.EASE_OUT, Theme.EASE_IN_OUT, Theme.EASE_SMOOTH, Theme.EASE_BACK,
        Theme.EASE_SPRING, Theme.EASE_BOUNCE};
    static final int[] ROW_COLORS = {0xFF7C8CFF, 0xFF4FB3F7, 0xFF3FC8B4, 0xFF6FDC5F, 0xFFE8D85A, 0xFFFFB443, 0xFFFF7A59, 0xFFFF6FA8};
    static final int[] MOODS = {0xFF2D6CDF, 0xFFE0483E, 0xFF21A66B, 0xFFF2B33D, 0x00000000};
    static final String[] MOOD_NAMES = {"blue", "red", "green", "gold", "see through"};

    boolean side, autoplay = true, on, kick;
    int mood, count;

    static int fade(int c, double k) {
        k = Math.max(0, Math.min(1, k));
        return (int) ((c >>> 24) * k + 0.5) << 24 | (c & 0xFFFFFF);
    }

    static void textAt(Canvas c, Font f, double size, double x, double cy, String s, int col, boolean center) {
        TextSize m = Font.measure(f, s, size);
        double h = m.h > 0 ? m.h : Font.lineHeight(f, size);
        c.text(f, size, center ? x - m.w / 2 : x, (int) (cy - h / 2 + 0.5), s, col);
    }

    void easeRows(Ui u) {
        Theme th = u.theme();
        for (int i = 0; i < ROW_NAMES.length; i++) {
            Item it = u.custom(ROW_NAMES[i], 0, 30, ItemFlag.NO_HIT);
            double k = u.anim(ROW_NAMES[i], side, 900, ROW_EASES[i]);
            Canvas c = u.canvas();
            if (c == null) continue;
            double tx = it.x() + 86, tw = it.w() - 96, bw = 46, run = (tw - bw) * 0.76, x = tx + (tw - bw - run) / 2 + k * run;
            textAt(c, th.font(), 13, it.x() + 4, it.y() + 15, ROW_NAMES[i], th.textDim(), false);
            c.rrect(tx, it.y() + 3, tw, 24, 12, 0x14FFFFFF);
            c.rect(tx + (tw - bw - run) / 2 + bw / 2, it.y() + 14, 1, 2, 0x30FFFFFF);
            c.rect(tx + (tw - bw - run) / 2 + run + bw / 2, it.y() + 14, 1, 2, 0x30FFFFFF);
            c.shadow(x, it.y() + 8, bw, 16, 8, 8, fade(ROW_COLORS[i], 0.45));
            c.rrect(x, it.y() + 6, bw, 18, 9, ROW_COLORS[i]);
        }
    }

    void colorCard(Ui u) {
        Theme th = u.theme();
        Item it = u.custom("mood", 0, 92);
        if (it.clicked()) mood = (mood + 1) % MOODS.length;
        u.tooltip("click for the next color");
        int col = u.animColor("mood", MOODS[mood], 600, Theme.EASE_IN_OUT);
        double lift = u.anim("mood lift", it.hovered(), 160, Theme.EASE_OUT);
        Canvas c = u.canvas();
        if (c == null) return;
        double y = it.y() - lift * 2;
        for (int i = 0; i < 12; i++) c.rect(it.x() + i * it.w() / 12, it.y() + 8, it.w() / 24, 76, 0x10FFFFFF);
        c.shadow(it.x(), y + 6, it.w(), 84, 14, 18, fade(col, 0.4 + lift * 0.2));
        c.rrect(it.x(), y, it.w(), 84, 14, col);
        c.ring(it.x() + 30, y + 42, 12, 2, 0x50FFFFFF);
        textAt(c, th.font(), 15, it.x() + 54, y + 42, String.format("%s  #%08X", MOOD_NAMES[mood], col), th.text(), false);
    }

    void springToggle(Ui u, String id) {
        Theme th = u.theme();
        Item it = u.custom(id, 64, 36, ItemFlag.FOCUSABLE);
        int key = it.key() & 0xFF;
        if (it.clicked() || key == ' ' || key == Goome.K_ENTER) on = !on;
        double k = u.anim(id, on, 520, Theme.EASE_SPRING);
        double squish = u.anim("squish", it.held(), 220, Theme.EASE_SPRING);
        int track = u.animColor(id, on ? th.accent() : 0xFF3A3F49, 260, Theme.EASE_OUT);
        Canvas c = u.canvas();
        if (c == null) return;
        double x = it.x() + 2, y = it.y() + 4, w = 60, h = 28, r = 11, kw = 2 * r + squish * 8;
        if (it.focused()) c.rrect(x - 3, y - 3, w + 6, h + 6, 17, fade(th.focus() != 0 ? th.focus() : th.accent(), 0.6));
        c.rrect(x, y, w, h, 14, track);
        double kx = x + 3 + k * (w - 6 - kw);
        c.shadow(kx, y + 5, kw, 2 * r, r, 6, 0x60000000);
        c.rrect(kx, y + 3, kw, 2 * r, r, 0xFFF4F5F7);
    }

    void bell(Ui u) {
        Theme th = u.theme();
        Item it = u.custom("bell", 140, 64);
        if (it.clicked()) {
            count++;
            kick = true;
        }
        double drop = u.anim("drop", kick ? -46 : 0, kick ? 0 : 900, Theme.EASE_BOUNCE);
        double pop = u.anim("pop", kick ? 0.4 : 1, kick ? 0 : 500, Theme.EASE_SPRING);
        double shown = u.anim("shown", count > 0, 200, Theme.EASE_OUT);
        kick = false;
        Canvas c = u.canvas();
        if (c == null) return;
        int bg = u.animColor("bell bg", it.hovered() ? 0xFF2E333C : 0xFF262A31, 140, Theme.EASE_OUT);
        c.rrect(it.x(), it.y() + 8, 120, 52, 12, bg);
        textAt(c, th.font(), 14, it.x() + 60, it.y() + 34, "notify me", th.text(), true);
        if (shown <= 0.01) return;
        double r = 13 * pop, cx = it.x() + 116, cy = it.y() + 10 + drop;
        c.shadow(cx - r, cy - r + 3, 2 * r, 2 * r, r, 8, fade(0x90000000, shown));
        c.circle(cx, cy, r, fade(0xFFE0483E, shown));
        if (pop > 0.6) textAt(c, th.font(), 12 * pop, cx, cy, String.valueOf(count), fade(0xFFFFFFFF, shown), true);
    }

    public static void main(String[] args) {
        Goome g;
        try {
            g = Goome.open(new Options().title("Animations").mode(Goome.MODE_WINDOW).size(880, 760).minSize(620, 700)
                .windowFlags(Goome.WINDOW_RESIZABLE | Goome.WINDOW_CENTER));
        } catch (GoomeException e) {
            System.err.println("cant open a window: " + e.getMessage());
            System.exit(1);
            return;
        }
        Anim a = new Anim();
        try (g; Ui u = g.ui(); Pacer pace = new Pacer(60)) {
            boolean running = true, test = g.backendName().startsWith("test");
            long frame = 0, flipped = 0;
            while (running) {
                int wait = test || a.autoplay || u.animating() ? pace.next() : 500;
                for (Event e = g.poll(wait); e != null; e = g.poll(0)) {
                    if (e.type() == Goome.QUIT || e.type() == Goome.CLOSE) running = false;
                    u.feed(e);
                }
                if (!running) break;
                long now = test ? frame * 16 : Goome.nowMs();
                frame++;
                if (a.autoplay && now - flipped >= 1500) {
                    flipped = now;
                    a.side = !a.side;
                    if (a.side) {
                        a.mood = (a.mood + 1) % MOODS.length;
                        a.on = !a.on;
                        a.count++;
                        a.kick = true;
                    }
                }
                if (u.begin(0) == null) continue;
                try {
                    u.app("Animations", Ui.APP_STATUSBAR);
                    u.heading("Every ease, same 900 ms");
                    if (u.button("Swap sides")) {
                        a.side = !a.side;
                        flipped = now;
                    }
                    u.sameLine();
                    a.autoplay = u.toggle("Play by itself", a.autoplay);
                    u.space(4);
                    a.easeRows(u);
                    u.space(8);
                    u.heading("Color fade");
                    a.colorCard(u);
                    u.space(8);
                    u.heading("Spring and bounce");
                    a.springToggle(u, "springy");
                    u.sameLine();
                    u.label(a.on ? "on, and it wobbled a bit" : "off");
                    u.sameLine();
                    u.space(40);
                    u.sameLine();
                    a.bell(u);
                    u.statusbar(u.animating() ? "moving" : "still");
                } finally {
                    u.end();
                }
            }
        }
    }
}
