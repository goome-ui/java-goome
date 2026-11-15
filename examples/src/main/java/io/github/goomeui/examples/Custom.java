package io.github.goomeui.examples;

import io.github.goomeui.App;
import io.github.goomeui.Canvas;
import io.github.goomeui.Font;
import io.github.goomeui.Goome;
import io.github.goomeui.Item;
import io.github.goomeui.ItemFlag;
import io.github.goomeui.Options;
import io.github.goomeui.Radii;
import io.github.goomeui.Split;
import io.github.goomeui.Style;
import io.github.goomeui.Styled;
import io.github.goomeui.Theme;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.nio.DoubleBuffer;
import java.util.Locale;

public final class Custom {
    static final int[] SWATCHES = {0xFF6FDC5F, 0xFF3FC8B4, 0xFF4FB3F7, 0xFF7C8CFF, 0xFFB57CFF, 0xFFFF6FA8, 0xFFFF7A59, 0xFFFFB443, 0xFFE8D85A};
    static final String[] NODE_NAMES = {"Noise", "Blur", "Output"};
    static final String[] NODE_INFO = {"seed 42", "radius 6", "1280x720"};
    static final int[] NODE_COLORS = {0xFFB57CFF, 0xFF4FB3F7, 0xFFFF7A59};
    static final double[][] NODE_START = {{20, 40}, {240, 120}, {460, 40}};
    static final double[] KEYS = {1.2, 2.6, 5.0, 6.4, 7.7};
    static final String[] HZ = {"60", "150", "400", "1k", "2.4k", "6k", "15k"};
    static final double[] EQ_START = {0.7, 0.62, 0.45, 0.5, 0.58, 0.68, 0.74};
    static final String[] RANGES = {"Day", "Week", "Month", "Year"};
    static final String[] TABS = {"Files", "Search", "Git"};

    int seg = 1, sel = 0, pick = 1, tab = 0;
    boolean on = true, chk = true;
    double vol = 0.62, amt = 0.7;
    String name = "living room", find = "search, but round";
    final Split split = new Split(0.4, false);

    static String f(String fmt, Object... a) {
        return String.format(Locale.ROOT, fmt, a);
    }

    static double clamp(double v, double a, double b) {
        return v < a ? a : v > b ? b : v;
    }

    static double snap(double v, double step) {
        return Math.floor(v / step + 0.5) * step;
    }

    static int fade(int c, double k) {
        return (int) ((c >>> 24) * clamp(k, 0, 1) + 0.5) << 24 | (c & 0xFFFFFF);
    }

    static int mix(int a, int b, double t) {
        int out = 0;
        for (int s = 0; s < 32; s += 8) {
            double x = (a >>> s) & 255, y = (b >>> s) & 255;
            out |= (int) (x + (y - x) * clamp(t, 0, 1) + 0.5) << s;
        }
        return out;
    }

    static void textAt(Canvas c, Font font, double size, double x, double cy, String s, int col, boolean center) {
        var m = Font.measure(font, s, size);
        double h = m.h > 0 ? m.h : Font.lineHeight(font, size);
        c.text(font, size, center ? x - m.w / 2 : x, Math.floor(cy - h / 2 + 0.5), s, col);
    }

    static void knob(Ui u, String name, double def, double scale, String fmt) {
        DoubleBuffer v = u.store(name, 2);
        if (v.get(1) == 0) v.put(0, def).put(1, 1);
        Item it = u.custom(name, 92, 112, ItemFlag.DRAG_Y, ItemFlag.FOCUSABLE);
        int key = it.key() & 0xFF;
        double val = v.get(0);
        if (it.held()) val = clamp(val - it.dy() / 180, 0, 1);
        if (key == Goome.K_UP || key == Goome.K_RIGHT) val = clamp(val + 0.05, 0, 1);
        if (key == Goome.K_DOWN || key == Goome.K_LEFT) val = clamp(val - 0.05, 0, 1);
        if (it.doubleClicked()) val = def;
        v.put(0, val);
        u.tooltip("drag up or down, arrows work too, double click resets it");

        Theme th = u.theme();
        Canvas c = u.canvas();
        if (c == null) return;
        double cx = it.x() + it.w() / 2, cy = it.y() + 40, r = 31, ri = 21;
        double a0 = 2.35619449, a1 = 7.06858347, at = a0 + (a1 - a0) * val;
        boolean lit = it.hovered() || it.held();
        if (lit) c.circle(cx, cy, r + 9, fade(th.accent(), it.held() ? 0.16 : 0.09));
        c.arc(cx, cy, r, 6, a0, a1, fade(th.text(), 0.09));
        if (val > 0.004) c.arc(cx, cy, r, 6, a0, at, th.accent());
        c.shadow(cx - ri, cy - ri + 4, 2 * ri, 2 * ri, ri, 10, 0x70000000);
        c.rrectGrad(cx - ri, cy - ri, 2 * ri, 2 * ri, Radii.of(ri), 0xFF3A3F49, 0xFF24272E, false);
        c.ring(cx, cy, ri - 0.5, 1, 0x1AFFFFFF);
        double px = cx + Math.cos(at) * (ri - 7), py = cy + Math.sin(at) * (ri - 7);
        c.line(cx + Math.cos(at) * (ri - 15), cy + Math.sin(at) * (ri - 15), px, py, 2.6, lit ? th.text() : mix(th.textDim(), th.text(), 0.5));
        textAt(c, th.font(), 14, cx, it.y() + 84, f(fmt, val * scale), th.text(), true);
        textAt(c, th.font(), 12.5, cx, it.y() + 103, name, th.textDim(), true);
    }

    static void timeline(Ui u, double ox, double oy) {
        DoubleBuffer t = u.store("playhead", 2);
        if (t.get(1) == 0) t.put(0, 2.4).put(1, 1);
        Theme th = u.theme();
        Font font = th.font();
        Item tr = u.custom("track", 0, 92, ItemFlag.NO_HIT);
        double x0 = tr.x() + 14, x1 = tr.x() + tr.w() - 14, secs = 10, per = (x1 - x0) / secs, ly0 = tr.y() + 34, ly1 = tr.y() + 61;
        Canvas c = u.canvas();
        if (c == null) return;
        c.rrect(tr.x(), tr.y(), tr.w(), tr.h(), 10, 0xFF1E2127);
        c.rrectLine(tr.x(), tr.y(), tr.w(), tr.h(), 10, 1, th.border());
        for (int i = 0; i <= 20; i++) {
            double x = x0 + i * per / 2;
            boolean odd = i % 2 == 1;
            c.rect(x - 0.5, tr.y() + (odd ? 18 : 12), 1, odd ? 5 : 11, fade(th.text(), odd ? 0.14 : 0.28));
            if (!odd && i < 20) c.text(font, 11, x + 4, tr.y() + 9, (i / 2) + "s", th.textDim());
        }
        c.rrect(x0 + 0.4 * per, ly0, 3.6 * per, 22, 6, fade(0xFFB57CFF, 0.30));
        c.rrectLine(x0 + 0.4 * per, ly0, 3.6 * per, 22, 6, 1, fade(0xFFB57CFF, 0.65));
        c.text(font, 12, x0 + 0.4 * per + 9, ly0 + 4, "intro.wav", 0xFFE6DAFF);
        c.rrect(x0 + 3.1 * per, ly1, 5.3 * per, 22, 6, fade(0xFF4FB3F7, 0.28));
        c.rrectLine(x0 + 3.1 * per, ly1, 5.3 * per, 22, 6, 1, fade(0xFF4FB3F7, 0.6));
        c.text(font, 12, x0 + 3.1 * per + 9, ly1 + 4, "drums.wav", 0xFFD6EEFF);

        u.pos(x0 + t.get(0) * per - 9 - ox, tr.y() + 4 - oy);
        Item hd;
        try (Styled s = u.style().radius(6).push()) {
            hd = u.custom("handle", 18, tr.h() - 8, ItemFlag.DRAG_X, ItemFlag.FOCUSABLE);
        }
        int key = hd.key() & 0xFF;
        double head = t.get(0);
        if (hd.held() && per > 0) head = clamp(head + hd.dx() / per, 0, secs);
        if (key == Goome.K_LEFT || key == Goome.K_RIGHT) head = clamp(snap(head, 0.5) + (key == Goome.K_RIGHT ? 0.5 : -0.5), 0, secs);
        if (hd.released()) head = snap(head, 0.1);
        t.put(0, head);
        u.tooltip("this one only moves sideways");
        double hx = x0 + head * per;

        c = u.canvas();
        if (c == null) return;
        for (double k : KEYS) {
            double kx = x0 + k * per, ky = ly1 + 11, d = 4.5;
            c.poly(new double[] {kx, ky - d, kx + d, ky, kx, ky + d, kx - d, ky}, k <= head ? th.accent() : 0xFFD6EEFF);
        }
        c.rect(x0, tr.y() + tr.h() - 5, hx - x0, 2, fade(th.accent(), 0.7));
        boolean lit = hd.hovered() || hd.held();
        c.rect(hx - 1, tr.y() + 20, 2, tr.h() - 25, th.accent());
        int hc = lit ? mix(th.accent(), 0xFFFFFFFF, 0.25) : th.accent();
        c.shadow(hx - 8, tr.y() + 7, 16, 13, 4, 6, 0x60000000);
        c.rrect(hx - 8, tr.y() + 5, 16, 13, 4, hc);
        c.triangle(hx - 6, tr.y() + 17, hx + 6, tr.y() + 17, hx, tr.y() + 23, hc);
        String s = f("%.1fs", head);
        double w = Font.measure(font, s, 11.5).w;
        double bx = clamp(hx + 12, tr.x() + 6, tr.x() + tr.w() - w - 18);
        c.rrect(bx, tr.y() + 4, w + 10, 16, 5, fade(th.accent(), 0.18));
        c.text(font, 11.5, bx + 5, tr.y() + 5, s, th.accent());
    }

    void swatchStrip(Ui u) {
        for (int i = 0; i < SWATCHES.length; i++) {
            if (i > 0) u.sameLine();
            Item it = u.custom("sw" + i, 34, 34, ItemFlag.FOCUSABLE);
            if (it.clicked()) sel = i;
            Canvas c = u.canvas();
            if (c == null) continue;
            double lift = it.hovered() && !it.held() ? 2 : 0;
            if (i == sel) c.rrectLine(it.x() - 4, it.y() - 4 - lift, it.w() + 8, it.h() + 8, 13, 2, 0xFFFFFFFF);
            c.shadow(it.x(), it.y() + 3 - lift, it.w(), it.h(), 9, 8 + lift * 2, 0x66000000);
            c.rrectGrad(it.x(), it.y() - lift, it.w(), it.h(), Radii.of(9), mix(SWATCHES[i], 0xFFFFFFFF, 0.12), mix(SWATCHES[i], 0xFF000000, 0.12), false);
            c.rrectLine(it.x(), it.y() - lift, it.w(), it.h(), 9, 1, 0x26FFFFFF);
            if (i == sel) {
                double cx = it.x() + it.w() / 2, cy = it.y() + it.h() / 2 - lift;
                c.line(cx - 6, cy, cx - 2, cy + 4, 2.4, 0xE0141618);
                c.line(cx - 2, cy + 4, cx + 6.5, cy - 5, 2.4, 0xE0141618);
            }
        }
    }

    static void faders(Ui u) {
        DoubleBuffer v = u.store("eq", 8);
        if (v.get(7) == 0) {
            for (int i = 0; i < 7; i++) v.put(i, EQ_START[i]);
            v.put(7, 1);
        }
        for (int i = 0; i < 7; i++) {
            if (i > 0) u.sameLine();
            Item it = u.custom("eq" + i, 28, 138, ItemFlag.DRAG_Y, ItemFlag.FOCUSABLE);
            double top = it.y() + 8, bot = it.y() + it.h() - 26, len = bot - top;
            int key = it.key() & 0xFF;
            double val = v.get(i);
            double knobY = top + (1 - val) * len;
            if (it.pressed() && (it.my() < knobY - 8 || it.my() > knobY + 8)) val = clamp(1 - (it.my() - top) / len, 0, 1);
            else if (it.held()) val = clamp(val - it.dy() / len, 0, 1);
            if (key == Goome.K_UP || key == Goome.K_DOWN) val = clamp(val + (key == Goome.K_UP ? 0.05 : -0.05), 0, 1);
            if (it.doubleClicked()) val = 0.5;
            v.put(i, val);
            Theme th = u.theme();
            Canvas c = u.canvas();
            if (c == null) continue;
            double cx = it.x() + it.w() / 2, ty = top + (1 - val) * len, mid = top + len / 2;
            c.rrect(cx - 2, top, 4, len, 2, fade(th.text(), 0.09));
            c.rect(cx - 6, mid - 0.5, 12, 1, fade(th.text(), 0.2));
            c.rrect(cx - 2, Math.min(ty, mid), 4, Math.abs(mid - ty), 2, th.accent());
            boolean lit = it.hovered() || it.held();
            c.shadow(cx - 11, ty - 5 + 2, 22, 10, 5, 6, 0x70000000);
            c.rrectGrad(cx - 11, ty - 5, 22, 10, Radii.of(5), lit ? 0xFFFFFFFF : 0xFFE4E7EC, lit ? 0xFFDDE1E7 : 0xFFB9BEC7, false);
            c.rect(cx - 5, ty - 0.5, 10, 1, fade(th.accent(), 0.9));
            textAt(c, th.font(), 11, cx, it.y() + it.h() - 9, HZ[i], lit ? th.text() : th.textDim(), true);
        }
    }

    static void xyPad(Ui u) {
        DoubleBuffer v = u.store("xy", 3);
        if (v.get(2) == 0) v.put(0, 0.62).put(1, 0.38).put(2, 1);
        Item it = u.custom("xy", 0, 138, ItemFlag.DRAG_X, ItemFlag.DRAG_Y);
        if (it.held()) v.put(0, clamp((it.mx() - it.x() - 8) / (it.w() - 16), 0, 1)).put(1, clamp((it.my() - it.y() - 8) / (it.h() - 16), 0, 1));
        Theme th = u.theme();
        Canvas c = u.canvas();
        if (c == null) return;
        double px = it.x() + 8 + v.get(0) * (it.w() - 16), py = it.y() + 8 + v.get(1) * (it.h() - 16);
        c.rrect(it.x(), it.y(), it.w(), it.h(), 12, 0xFF1B1E23);
        c.rrectGrad(it.x(), it.y(), it.w(), it.h(), Radii.of(12), fade(th.accent(), 0.0), fade(th.accent(), 0.10), true);
        for (int i = 1; i < 4; i++) {
            c.rect(it.x() + it.w() * i / 4, it.y() + 6, 1, it.h() - 12, 0x0FFFFFFF);
            c.rect(it.x() + 6, it.y() + it.h() * i / 4, it.w() - 12, 1, 0x0FFFFFFF);
        }
        c.rect(px - 0.5, it.y() + 4, 1, it.h() - 8, fade(th.accent(), 0.35));
        c.rect(it.x() + 4, py - 0.5, it.w() - 8, 1, fade(th.accent(), 0.35));
        c.circle(px, py, it.held() ? 18 : 14, fade(th.accent(), it.held() ? 0.22 : 0.14));
        c.circle(px, py, 7, th.accent());
        c.ring(px, py, 7, 2, 0xFFFFFFFF);
        c.text(th.font(), 11.5, it.x() + 12, it.y() + 9, f("cutoff %.2f  res %.2f", v.get(0), 1 - v.get(1)), th.textDim());
        c.rrectLine(it.x(), it.y(), it.w(), it.h(), 12, 1, it.hovered() || it.held() ? fade(th.accent(), 0.6) : th.border());
    }

    static void nodeBox(Canvas c, Theme th, int n, Item it, double x, double y, boolean top) {
        double w = it.w(), h = it.h();
        c.shadow(x, y + (top ? 10 : 5), w, h, 12, top ? 22 : 12, top ? 0x90000000 : 0x70000000);
        c.rrect(x, y, w, h, 12, 0xFF2A2E36);
        c.rrectGrad(x, y, w, 26, new Radii(12, 12, 0, 0), mix(NODE_COLORS[n], 0xFF2A2E36, 0.25), mix(NODE_COLORS[n], 0xFF2A2E36, 0.55), true);
        c.circle(x + 15, y + 13, 4, 0xFFFFFFFF);
        c.text(th.font(), 13.5, x + 26, y + 5, NODE_NAMES[n], 0xFFFFFFFF);
        c.text(th.font(), 12, x + 12, y + 35, NODE_INFO[n], th.textDim());
        boolean lit = it.held() || it.hovered();
        int edge = lit ? mix(th.accent(), 0xFFFFFFFF, it.held() ? 0.2 : 0) : 0x22FFFFFF;
        c.rrectLine(x, y, w, h, 12, lit ? 1.5 : 1, edge);
        c.circle(x, y + 41, 5.5, 0xFF1A1C21);
        c.ring(x, y + 41, 5, 2, NODE_COLORS[n]);
        c.circle(x + w, y + 41, 5.5, 0xFF1A1C21);
        c.ring(x + w, y + 41, 5, 2, NODE_COLORS[n]);
    }

    static void wire(Canvas c, double ax, double ay, double bx, double by, int from, int to) {
        double px = ax, py = ay, pull = clamp((bx - ax) * 0.5, 40, 200);
        for (int i = 1; i <= 32; i++) {
            double t = i / 32.0, s = 1 - t;
            double x = s * s * s * ax + 3 * s * s * t * (ax + pull) + 3 * s * t * t * (bx - pull) + t * t * t * bx;
            double y = s * s * s * ay + 3 * s * s * t * ay + 3 * s * t * t * by + t * t * t * by;
            c.line(px, py, x, y, 2.5, mix(from, to, t - 1 / 64.0));
            px = x;
            py = y;
        }
    }

    static void nodeArea(Ui u, double ox, double oy) {
        int count = NODE_NAMES.length;
        Item ar = u.custom("area", 0, 232, ItemFlag.NO_HIT);
        Item[] it = new Item[count];
        DoubleBuffer[] pos = new DoubleBuffer[count];
        int top = -1;
        for (int i = 0; i < count; i++) {
            DoubleBuffer p = pos[i] = u.store(NODE_NAMES[i], 3);
            if (p.get(2) == 0) p.put(0, NODE_START[i][0]).put(1, NODE_START[i][1]).put(2, 1);
            u.pos(ar.x() - ox + p.get(0), ar.y() - oy + p.get(1));
            it[i] = u.custom(NODE_NAMES[i], 150, 58, ItemFlag.DRAG_X, ItemFlag.DRAG_Y);
            double x = p.get(0), y = p.get(1);
            if (it[i].held()) {
                x += it[i].dx();
                y += it[i].dy();
                top = i;
            }
            x = clamp(x, 8, ar.w() - it[i].w() - 8);
            y = clamp(y, 8, ar.h() - it[i].h() - 8);
            if (it[i].released()) {
                x = clamp(snap(x, 20), 0, ar.w() - it[i].w());
                y = clamp(snap(y, 20), 0, ar.h() - it[i].h());
            }
            p.put(0, x).put(1, y);
        }
        Theme th = u.theme();
        Canvas c = u.canvas();
        if (c == null) return;
        c.rrect(ar.x(), ar.y(), ar.w(), ar.h(), 12, 0xFF17191E);
        c.clipPush(ar.x() + 1, ar.y() + 1, ar.w() - 2, ar.h() - 2);
        for (double y = ar.y() + 20; y < ar.y() + ar.h(); y += 20) {
            for (double x = ar.x() + 20; x < ar.x() + ar.w(); x += 20) c.rect(x - 1, y - 1, 2, 2, 0x16FFFFFF);
        }
        if (top >= 0) {
            double sx = clamp(snap(pos[top].get(0), 20), 0, ar.w() - it[top].w()), sy = clamp(snap(pos[top].get(1), 20), 0, ar.h() - it[top].h());
            c.rrect(ar.x() + sx, ar.y() + sy, it[top].w(), it[top].h(), 12, fade(th.accent(), 0.08));
            c.rrectLine(ar.x() + sx, ar.y() + sy, it[top].w(), it[top].h(), 12, 1.5, fade(th.accent(), 0.45));
        }
        for (int i = 0; i + 1 < count; i++) {
            wire(c, ar.x() + pos[i].get(0) + it[i].w(), ar.y() + pos[i].get(1) + 41, ar.x() + pos[i + 1].get(0), ar.y() + pos[i + 1].get(1) + 41,
                NODE_COLORS[i], NODE_COLORS[i + 1]);
        }
        for (int i = 0; i < count; i++) if (i != top) nodeBox(c, th, i, it[i], ar.x() + pos[i].get(0), ar.y() + pos[i].get(1), false);
        if (top >= 0) nodeBox(c, th, top, it[top], ar.x() + pos[top].get(0), ar.y() + pos[top].get(1), true);
        c.text(th.font(), 12, ar.x() + 14, ar.y() + ar.h() - 24, "drag the boxes, they snap to the grid when you let go", fade(th.textDim(), 0.8));
        c.clipPop();
        c.rrectLine(ar.x(), ar.y(), ar.w(), ar.h(), 12, 1, th.border());
    }

    void buttonRows(Ui u) {
        u.heading("Buttons your way");
        u.wrapped("push a radius, a color or per corner radii, then pop it. only the widgets in between change.");
        u.button("Normal");
        u.sameLine();
        u.pushNum(Style.RADIUS, 0);
        u.button("Square");
        u.pop();
        u.sameLine();
        u.pushNum(Style.RADIUS, 999);
        u.button("Pill");
        u.pop();
        u.sameLine();
        u.pushRadii(16, 3, 16, 3);
        u.button("Leaf");
        u.pop();

        u.pushNum(Style.GAP, 1);
        for (int i = 0; i < RANGES.length; i++) {
            double l = i == 0 ? 9 : 2, r = i == RANGES.length - 1 ? 9 : 2;
            if (i > 0) u.sameLine();
            Styled s = u.style().radii(l, r, r, l);
            if (seg == i) s.inputBg(0xFF3B4250).border(0x40FFFFFF);
            else s.text(0xFF969CA6).border(0x00000000);
            try (Styled pushed = s.push()) {
                u.size(74, 0);
                if (u.button(RANGES[i])) seg = i;
            }
        }
        u.pop();
        u.space(8);

        try (Styled s = u.style().inputBg(0xFF6FDC5F).hover(0xFF86E878).active(0xFF58C24A).text(0xFF0F1D0C).border(0x00000000).radius(999).push()) {
            u.button("Save it");
        }
        u.sameLine();
        try (Styled s = u.style().inputBg(0x00000000).hover(0x24E5534B).active(0x40E5534B).text(0xFFFF7A72).border(0x80E5534B).borderWidth(1.5).push()) {
            u.button("Delete");
        }
        u.sameLine();
        try (Styled s = u.style().fontSize(19).pad(14).radii(4, 18, 4, 18).push()) {
            u.button("Big one");
        }

        try (Styled tabs = u.style().radii(12, 12, 0, 0).gap(2).push()) {
            for (int i = 0; i < TABS.length; i++) {
                if (i > 0) u.sameLine();
                boolean mine = tab == i;
                try (Styled s = u.style().inputBg(mine ? 0xFF2F333C : 0xFF1F2228).border(mine ? 0x30FFFFFF : 0x00000000)
                    .text(mine ? 0xFFE9EBEE : 0xFF8C929C).push()) {
                    if (u.button(TABS[i])) tab = i;
                }
            }
        }
    }

    void moreWidgets(Ui u) {
        u.heading("Every widget reads it");
        try (Styled s = u.style().accent(0xFFFF6FA8).accentText(0xFF2A0614).push()) {
            u.row(2);
            chk = u.checkbox("Pink", chk);
            chk = u.toggle("Also pink", chk);
            amt = u.slider("Amount", amt, 0, 1, "%.2f");
        }
        try (Styled s = u.style().radius(999).push()) {
            find = u.input("##find", find, 64);
        }
        try (Styled s = u.style().radius(0).accent(0xFF4FB3F7).accentText(0xFF06202E).push()) {
            u.progress(amt, "square corners");
        }
    }

    void lightCard(Ui u) {
        Item at = u.custom("light-spot", 0, 262, ItemFlag.NO_HIT);
        Theme t = u.theme().copy();
        t.light();
        t.radius(8).shadow(0x60000000);
        u.pushTheme(t);
        Window win = new Window(at.x(), at.y(), at.w(), at.h());
        if (u.window("A whole other theme", win, 0)) {
            on = u.toggle("Wifi", on);
            vol = u.slider("Volume", vol, 0, 1, "%.2f");
            u.row(2);
            pick = u.radio("Small", pick, 0);
            pick = u.radio("Large", pick, 1);
            name = u.input("##name", name, 64);
            u.pushNum(Style.RADIUS, 999);
            u.button("Cancel");
            u.sameLine();
            try (Styled s = u.style().inputBg(t.accent()).hover(0xFF55C944).active(0xFF3AA12B).text(0xFFFFFFFF).border(0x00000000).push()) {
                u.button("Apply");
            }
            u.pop();
        }
        u.windowEnd();
        u.pop();
    }

    void frame(Ui u) {
        if (!u.splitBegin("main", split)) return;
        buttonRows(u);
        u.space(4);
        moreWidgets(u);
        u.space(6);
        lightCard(u);
        u.splitNext();

        u.pos(0, 0);
        Item o = u.custom("##origin", 1, 1, ItemFlag.NO_HIT);
        u.pushColor(Style.ACCENT, SWATCHES[sel]);
        u.heading("Custom widgets");
        u.pos(176, 3);
        u.badge("pinned with ui.pos", 0x26FFFFFF);
        u.space(2);
        knob(u, "Gain", 0.62, 12, "%.1f dB");
        u.sameLine();
        knob(u, "Tone", 0.35, 100, "%.0f%%");
        u.sameLine();
        knob(u, "Mix", 0.8, 100, "%.0f%%");
        u.sameLine();
        knob(u, "Width", 0.5, 200, "%.0f%%");
        u.space(4);
        timeline(u, o.x(), o.y());
        u.space(4);
        swatchStrip(u);
        u.space(4);
        nodeArea(u, o.x(), o.y());
        u.space(4);
        faders(u);
        u.sameLine();
        xyPad(u);
        double head = u.store("playhead", 2).get(0), gain = u.store("Gain", 2).get(0);
        u.pop();
        u.splitEnd();
        u.statusbar(f("gain %.1f dB   playhead %.1fs", gain * 12, head));
    }

    public static void main(String[] args) {
        Custom demo = new Custom();
        new App("Custom", 1120, 800)
            .options(new Options().minSize(800, 560))
            .flags(Ui.APP_STATUSBAR | Ui.APP_NO_PAD)
            .onFrame(demo::frame)
            .run();
    }
}
