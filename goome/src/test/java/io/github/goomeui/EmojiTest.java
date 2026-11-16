package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmojiTest {
    static final String GRIN = "😀", TONE = "🏻", ZWJ = "‍", HEART = "❤", VS16 = "️", CAT = "🐱";
    static final String HEB = "ab אבג (12) c";

    static int hash(Font f, double size, String s, int argb, int dir, int w, int h) {
        try (Canvas c = Canvas.create(400, 120, 400, 1)) {
            c.raw("w", w);
            c.raw("h", h);
            c.raw("kx1", w);
            c.raw("ky1", h);
            Font.textDir(dir);
            try {
                c.text(f, size, 4, 4, s, argb);
            } finally {
                Font.textDir(Font.DIR_AUTO);
            }
            c.raw("h", 120);
            return T.fnv(c.pixels(), 400, 120, 400);
        }
    }

    static void near(double want, double got) {
        assertTrue(Math.abs(want - got) < 0.01, "want " + want + " got " + got);
    }

    @Test
    void sameAsC() throws Exception {
        try (Font f = Font.fromBytes(T.data("colr.ttf")); Font cb = Font.fromBytes(T.data("cbdt.ttf")); Font sb = Font.fromBytes(T.data("sbix.ttf"));
            Font d = Font.fromBytes(T.data("dejavu-sub.ttf"))) {
            near(24, f.width(GRIN + TONE, 30));
            near(24, f.width(GRIN + ZWJ + GRIN, 30));
            near(18, f.width(HEART, 30));
            near(24, f.width(HEART + VS16, 30));
            int[] got = new int[6];
            got[0] = hash(f, 30, "ab " + GRIN + " " + GRIN + TONE + " " + GRIN + ZWJ + GRIN + " " + HEART + " " + HEART + VS16, 0xFF2040A0, Font.DIR_AUTO,
                400, 60);
            try (Canvas c = Canvas.create(400, 120, 400, 1)) {
                c.text(cb, 16, 4, 4, CAT, 0xFFFFFFFF);
                c.text(cb, 64, 30, 4, CAT, 0xFFFFFFFF);
                c.text(sb, 40, 120, 4, CAT + CAT, 0x80FFFFFF);
                int mid = c.pixels().get((4 + 40) * 400 + 30 + 35);
                int g = mid >>> 8 & 255;
                assertTrue((mid >>> 24) == 255 && (mid >>> 16 & 255) > 200 && g > 100 && g < 210 && (mid & 255) < 120, Integer.toHexString(mid));
                got[1] = T.fnv(c.pixels(), 400, 120, 400);
            }
            d.fallback(f);
            d.fallback(d);
            assertEquals(java.util.List.of(f), d.fallbacks());
            near(d.width("goome! ", 20) + 16, d.width("goome! " + GRIN, 20));
            got[2] = hash(d, 20, "goome! " + GRIN + " Hi", 0xFFFFFFFF, Font.DIR_AUTO, 400, 40);
            got[3] = hash(f, 24, HEB, 0xFFFFFFFF, Font.DIR_AUTO, 400, 40);
            assertNotEquals(got[3], hash(f, 24, HEB, 0xFFFFFFFF, Font.DIR_RTL, 400, 40));
            assertEquals(got[3], hash(f, 24, HEB, 0xFFFFFFFF, Font.DIR_LTR, 400, 40));
            got[4] = hash(f, 24, HEB, 0xFFFFFFFF, Font.DIR_RTL, 400, 40);
            near(93.2, f.width("بلام ببب لم", 20));
            got[5] = hash(f, 30, "بلام ببب لم بَب", 0xFFFFFFFF, Font.DIR_AUTO, 400, 50);
            assertNotEquals(got[5], hash(f, 30, "ب لام ب ب ب لم", 0xFFFFFFFF, Font.DIR_AUTO, 400, 50));
            String[] want = {"3bab3875", "85fe883a", "d3add387", "b79518dd", "49a72d7d", "469719e1"};
            for (int i = 0; i < 6; i++) assertEquals(want[i], T.hex(got[i]), "emoji rtl hash " + i);
        }
    }

    static final int L = Goome.K_LEFT, R = Goome.K_RIGHT, H = Goome.K_HOME, E = Goome.K_END, S = Goome.SHIFT;
    static final String ALEF_BET = "\u05D0\u05D1\u05D2";

    static String caret(Font f, boolean edit, String text, int[] keys, int[] hash) {
        try (ImeTest.Run k = new ImeTest.Run(f, "end 100000", edit ? "" : text, edit ? text : "", edit ? 2 : 1)) {
            for (int i = 0; i < keys.length; i += 2) k.key(keys[i], keys[i + 1]);
            k.frame(0);
            if (hash != null) hash[0] = k.hash;
            return edit ? k.ed.text() : k.in;
        }
    }

    @Test
    void bidiCaret() throws Exception {
        try (Font f = Font.fromBytes(T.data("colr.ttf"))) {
            int[][] hs = new int[6][1];
            assertEquals("\u05D0a\u05D1\u05D2", caret(f, false, ALEF_BET, new int[] {H, 0, L, 0, 'a', 0}, hs[0]));
            assertEquals("\u05D0\u05D1b\u05D2", caret(f, false, ALEF_BET, new int[] {E, 0, R, 0, 'b', 0}, null));
            assertEquals("c" + ALEF_BET, caret(f, false, ALEF_BET, new int[] {H, 0, R, 0, 'c', 0}, null));
            assertEquals("abcz " + ALEF_BET, caret(f, false, "abc " + ALEF_BET, new int[] {E, 0, L, 0, 'z', 0}, null));
            assertEquals("ab \u05D0x\u05D1 cd", caret(f, false, "ab \u05D0\u05D1 cd", new int[] {H, 0, R, 0, R, 0, R, 0, R, 0, 'x', 0}, null));
            caret(f, false, "ab " + ALEF_BET + " cd", new int[] {H, 0, R, 0, R, 0, R, 0, R, S, R, S}, hs[1]);
            assertEquals(GRIN + TONE + "qa", caret(f, false, GRIN + TONE + "a", new int[] {H, 0, R, 0, 'q', 0}, null));
            assertEquals("\u05D0x\u05D1\u05D2\nabc", caret(f, true, ALEF_BET + "\nabc", new int[] {H, 0, L, 0, 'x', 0}, hs[2]));
            assertEquals(ALEF_BET + "\nyabc", caret(f, true, ALEF_BET + "\nabc", new int[] {H, 0, L, 0, L, 0, L, 0, L, 0, 'y', 0}, null));
            caret(f, true, "\u05D0\u05D1 ab \u05D2\n\u0628\u0628", new int[] {H, 0, L, S, L, S, L, S}, hs[3]);
            caret(f, false, "\u0628\u0644\u0627\u0645 12", new int[] {E, 0, L, S}, hs[4]);
            caret(f, false, "\u0628\u0644\u0627\u0645 12", new int[] {E, 0, R, S}, hs[5]);
            assertNotEquals(hs[4][0], hs[5][0]);
            String[] want = {"f96a8023", "8c422f63", "eb1c9776", "fb019b26", "31cd9204", "e15c011a"};
            for (int i = 0; i < 6; i++) assertEquals(want[i], T.hex(hs[i][0]), "bidi caret hash " + i);
        }
    }

    @Test
    void badDirection() {
        assertThrows(IllegalArgumentException.class, () -> Font.textDir(3));
        assertThrows(IllegalArgumentException.class, () -> Font.textDir(-1));
    }
}
