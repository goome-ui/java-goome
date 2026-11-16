package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AnimTest {
    static final class Run {
        Goome g;
        Ui u;
        boolean on, dropped;
        double box, sp, last;
        int col, hash;

        void frame() {
            for (Event e : g.events(0)) u.feed(e);
            Canvas c = u.begin(0);
            if (c == null) return;
            Canvas r = u.canvas();
            box = u.anim("box", on ? 200 : 10, 160, Theme.EASE_OUT);
            sp = u.anim("spring", on, 320, Theme.EASE_SPRING);
            col = u.animColor("col", on ? 0xFF20D060 : 0, 200, Theme.EASE_LINEAR);
            if (!dropped) last = u.anim("gone", on ? 5 : 0, 100, Theme.EASE_LINEAR);
            if (r != null) {
                r.rect(box, 20, 60, 40, 0xFFE0A030);
                r.rrect(20 + sp * 180, 90, 50, 30, 15, 0xFF4FB3F7);
                r.rect(20, 140, 260, 40, col);
            }
            java.nio.IntBuffer px = c.pixels();
            int w = c.w(), h = c.h(), s = c.stride();
            u.end();
            hash = T.fnv(px, w, h, s);
        }
    }

    @Test
    void eases() {
        assertEquals(0.25, Ui.ease(Theme.EASE_LINEAR, 0.25));
        assertEquals(0.125, Ui.ease(Theme.EASE_IN, 0.5));
        assertEquals(0.875, Ui.ease(Theme.EASE_OUT, 0.5));
        assertEquals(0.0625, Ui.ease(Theme.EASE_IN_OUT, 0.25));
        assertEquals(0.9375, Ui.ease(Theme.EASE_IN_OUT, 0.75));
        assertEquals(0.5, Ui.ease(Theme.EASE_SMOOTH, 0.5));
        assertEquals(0.47265625, Ui.ease(Theme.EASE_BOUNCE, 0.25));
        assertEquals(0, Ui.ease(Theme.EASE_OUT, -3));
        assertEquals(1, Ui.ease(Theme.EASE_IN, 7));
        for (int e = Theme.EASE_SMOOTH; e <= Theme.EASE_BOUNCE; e++) {
            if (e == Theme.EASE_CUSTOM || e == Theme.EASE_BACK) continue;
            assertEquals(0, Ui.ease(e, 0), "ease " + e);
            assertEquals(1, Ui.ease(e, 1), "ease " + e);
        }
        double peak = 0, low = 1, bpeak = 0;
        for (int i = 1; i < 100; i++) {
            double v = Ui.ease(Theme.EASE_SPRING, i / 100.0), b = Ui.ease(Theme.EASE_BOUNCE, i / 100.0);
            peak = Math.max(peak, v);
            if (i > 30) low = Math.min(low, v);
            bpeak = Math.max(bpeak, b);
            assertTrue(b >= 0);
        }
        assertTrue(peak > 1.1 && peak < 1.25 && low < 1 && low > 0.9 && bpeak <= 1);
        double half = Ui.ease(Theme.EASE_SPRING, 0.5);
        assertTrue(half > 0.95 && half < 1.05 && Ui.ease(Theme.EASE_SPRING, 0.999) > 0.997);
    }

    @Test
    void framesMatchC() {
        Run k = new Run();
        try (Goome g = Goome.open(new Options().backend(Goome.TEST).mode(Goome.MODE_WINDOW).size(300, 200).testScript("end 100000")); Ui u = g.ui()) {
            k.g = g;
            k.u = u;
            k.frame();
            assertEquals(10, k.box);
            assertEquals(0, k.sp);
            assertEquals(0, k.col);
            assertEquals(0, k.last);
            k.frame();
            assertFalse(u.animating());
            int[] hs = new int[4];
            k.on = true;
            double prev = k.box, over = 0, mid = -1;
            int moving = 0;
            for (int f = 0; f < 60; f++) {
                k.frame();
                if (f == 1) hs[0] = k.hash;
                if (f == 4) hs[1] = k.hash;
                if (f == 8) hs[2] = k.hash;
                assertTrue(k.box >= prev);
                prev = k.box;
                over = Math.max(over, k.sp);
                if (f == 6) mid = k.col & 0xFFFFFFFFL;
                if (u.animating()) moving = f + 1;
            }
            hs[3] = k.hash;
            assertEquals(200, k.box);
            assertEquals(1, k.sp);
            assertEquals(0xFF20D060, k.col);
            assertEquals(5, k.last);
            assertTrue(over > 1.05 && moving >= 19 && moving <= 22 && !u.animating(), "over " + over + " moving " + moving);
            long m = (long) mid;
            assertTrue((m >> 24) > 0x70 && (m >> 24) < 0x90 && ((m >> 8) & 255) == 0xD0 && ((m >> 16) & 255) == 0x20, Long.toHexString(m));
            k.on = false;
            k.frame();
            assertTrue(u.animating());
            assertEquals(200, k.box);
            k.frame();
            assertTrue(k.box < 200 && k.box > 10);
            k.on = true;
            k.frame();
            assertTrue(k.box < 200 && u.animating());
            k.dropped = true;
            for (int i = 0; i < 1600; i++) k.frame();
            k.dropped = false;
            k.on = false;
            k.frame();
            assertEquals(0, k.last);
            String[] want = {"0226f2f5", "906d3ce5", "db5848ad", "12bea72d"};
            for (int i = 0; i < 4; i++) assertEquals(want[i], T.hex(hs[i]), "anim hash " + i);
        }
    }
}
