package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IconTest {
    static final String[] NAMES = {"base", "dressed", "as_is", "pushed", "theme_swap", "after"};
    static final int[][] MODES = {{0, 0, 0}, {1, 1, 0}, {1, 1, 1}, {3, 0, 0}, {4, 0, 0}, {0, 0, 0}};

    static int[] iconPx(int i) {
        int[] px = new int[256];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int dx = x - 8, dy = y - 8, v = (x * 7 + y * 13 + i * 29) & 255;
                int a = dx * dx + dy * dy < 40 + i ? 255 : (x + y + i) % 5 == 0 ? 120 : 0;
                px[y * 16 + x] = a << 24 | v << 16 | (255 - v) << 8 | (i * 11 & 255);
            }
        }
        return px;
    }

    static int[] skinPx() {
        int[] px = new int[144];
        for (int y = 0; y < 12; y++) for (int x = 0; x < 12; x++) px[y * 12 + x] = x < 3 || y < 3 || x > 8 || y > 8 ? 0xFF7B4FD0 : 0xFFF0E8FF;
        return px;
    }

    static final class Scene implements AutoCloseable {
        final Bitmap[] icons = new Bitmap[Icon.COUNT];
        final Bitmap heart = Bitmap.fromArgb(iconPx(40), 16, 16);
        final Bitmap skinBm = Bitmap.fromArgb(skinPx(), 12, 12);
        final Skin skin = new Skin(skinBm, 3);
        final Window win = new Window(6, 6, 288, 248);
        final Flag more = new Flag(true);
        boolean check = true, check2, on = true;
        int radio = 1, sel = 1, tab;
        double val = 0.4, num = 3;

        Scene() {
            for (int i = 0; i < Icon.COUNT; i++) icons[i] = Bitmap.fromArgb(iconPx(i), 16, 16);
        }

        void dress(Theme t, boolean on, int tint) {
            for (Icon i : Icon.values()) t.icon(i, on ? icons[i.ordinal()] : null);
            for (int k = Ui.SKIN_CHECKBOX; k < Ui.SKIN_COUNT; k++) t.skin(k, on ? skin : null);
            t.iconTint(tint);
        }

        void draw(Ui u, int mode) {
            Theme t = u.theme();
            if (u.window("icons", win, Ui.WIN_MOVE | Ui.WIN_RESIZE | Ui.WIN_COLLAPSE | Ui.WIN_CLOSE)) {
                Styled st = null;
                if (mode == 3) st = u.style().icon(Icon.CHECK, heart).skin(Ui.SKIN_CHECKBOX, skin).push();
                if (mode == 4) t.icon(Icon.CHECK, heart).skin(Ui.SKIN_CHECKBOX, skin);
                check = u.checkbox("Check", check);
                if (st != null) st.close();
                if (mode == 4) t.icon(Icon.CHECK, null).skin(Ui.SKIN_CHECKBOX, null);
                check2 = u.checkbox("Other", check2);
                radio = u.radio("A", radio, 0);
                u.sameLine();
                radio = u.radio("B", radio, 1);
                on = u.toggle("On", on);
                val = u.slider("Val", val, 0, 1, "%.2f");
                sel = u.combo("Pick", sel, "one", "two", "three");
                tab = u.tabs("t", tab, "One", "Two");
                num = u.number("N", num, 1, "%.0f");
                if (u.collapsing("More", more)) u.label("inside");
            }
            u.windowEnd();
        }

        @Override
        public void close() {
            for (Bitmap b : icons) b.close();
            heart.close();
            skinBm.close();
        }
    }

    static Map<String, String> run() {
        Map<String, String> out = new HashMap<>();
        try (Goome g = Goome.openOffscreen(300, 260, 2, "icons"); Scene s = new Scene(); Ui u = g.ui()) {
            u.theme().animMs(0).font(null).fontSize(14);
            for (int k = 0; k < MODES.length; k++) {
                s.dress(u.theme(), MODES[k][1] != 0, MODES[k][2]);
                for (int f = 0; f < 3; f++) {
                    for (Event e : g.events(0)) u.feed(e);
                    if (u.begin(0) == null) continue;
                    s.draw(u, MODES[k][0]);
                    u.end();
                }
                Image fr = g.lastFrame();
                out.put(NAMES[k], T.hex(T.fnv(fr.pixels(), fr.w, fr.h, fr.stride)));
            }
        }
        return out;
    }

    @Test
    void sameHashAsC() throws Exception {
        Map<String, String> got = run();
        assertNotEquals(got.get("base"), got.get("dressed"));
        assertNotEquals(got.get("dressed"), got.get("as_is"));
        assertNotEquals(got.get("base"), got.get("pushed"));
        assertEquals(got.get("pushed"), got.get("theme_swap"), "pop didnt put the checkbox icon and skin back");
        assertEquals(got.get("base"), got.get("after"), "icons leaked into the next frame");
        assertEquals("a12c0b6e", got.get("base"));
        String out = T.runC("tests/icons.c");
        if (out == null) return;
        for (String l : out.split("\\R")) {
            if (l.isBlank()) continue;
            String[] p = l.trim().split(" ");
            assertEquals(p[1], got.get(p[0]), p[0]);
        }
    }

    @Test
    void themeIconsAndSkins() {
        assertEquals(Icon.COUNT, Icon.values().length);
        assertEquals(21, Icon.PICKER.ordinal());
        try (Goome g = Goome.openOffscreen(100, 100, 1, "icons"); Ui u = g.ui(); Bitmap bm = Bitmap.fromArgb(iconPx(1), 16, 16)) {
            Theme t = u.theme();
            assertNull(t.icon(Icon.CLOSE));
            assertEquals(Theme.ICON_TINT, t.iconTint());
            t.icon(Icon.CLOSE, bm).iconTint(Theme.ICON_AS_IS);
            assertSame(bm, t.icon(Icon.CLOSE));
            assertEquals(Theme.ICON_AS_IS, t.iconTint());
            Skin s = new Skin(bm, 4);
            t.skin(Ui.SKIN_POPUP, s);
            assertSame(s, t.skin("skin_popup"));
            Theme c = t.copy();
            assertSame(bm, c.icon(Icon.CLOSE));
            assertSame(s, c.skin(Ui.SKIN_POPUP));
            assertThrows(IllegalArgumentException.class, () -> t.skin(Ui.SKIN_COUNT, null));
            assertThrows(IllegalArgumentException.class, () -> t.color("icon_tint"));
            bm.close();
            assertNull(t.icon(Icon.CLOSE));
            assertNull(t.skin(Ui.SKIN_POPUP));
            assertNull(c.icon(Icon.CLOSE));
        }
    }
}
