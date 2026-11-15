package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomTest {
    static final class Kit implements AutoCloseable {
        final Goome g;
        Ui u;
        int mode, push, rets;
        Set<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
        int stn = 1;
        Item it, pos, flow, sized;
        DoubleBuffer st, st2;
        String buf = "hi";
        String hash;
        Runnable extra;

        Kit() {
            g = Goome.open(new Options().backend(Goome.TEST).mode(Goome.MODE_WINDOW).size(300, 200).testScript("end 100000"));
            fresh();
        }

        void fresh() {
            if (u != null) u.close();
            u = g.ui();
            u.theme().font(null).fontSize(14);
        }

        void ev(Event e) {
            g.push(e);
        }

        int px(int x, int y) {
            Image f = g.lastFrame();
            if (x < 0 || y < 0 || x >= f.w || y >= f.h) return 0;
            return f.pixel(x, y);
        }

        void frame() {
            for (Event e : g.events(0)) u.feed(e);
            Canvas c = u.begin(0);
            assertNotNull(c);
            if (mode == 0) {
                if (push == 1) u.pushNum(Style.RADIUS, 0);
                if (push == 2) u.pushRadii(14, 0, 14, 0);
                if (push == 3) {
                    u.pushColor(Style.INPUT_BG, 0xFFC03030);
                    u.pushNum(Style.BORDER_WIDTH, 3);
                }
                if (push == 4) {
                    Theme t = u.theme().copy();
                    t.light();
                    u.pushTheme(t);
                }
                if (push == 5) u.pushNum(Style.FONT_SIZE, 22);
                if (push == 6) {
                    u.pushNum(Style.RADIUS, 0);
                    u.pushNum(Style.PAD, 30);
                }
                u.button("Push");
                buf = u.input("In", buf, 16);
                if (push >= 1 && push <= 5) u.pop(push == 3 ? 2 : 1);
                if (push == 7) u.pop(3);
                u.button("After");
            } else if (mode == 1) {
                u.pos(37, 21);
                pos = u.custom("p", 50, 30, ItemFlag.NO_HIT);
                flow = u.custom("f", 0, 0, ItemFlag.NO_HIT);
                u.size(120, 40);
                sized = u.custom("s", 0, 0, ItemFlag.NO_HIT);
                u.pos(150, 60);
                it = u.custom("k", 60, 60, flags);
                if (u.changed()) rets++;
                st = u.store("st", stn);
                u.pushId("other");
                st2 = u.store("st", 1);
                u.popId();
            } else if (mode == 2) {
                Canvas r = u.canvas();
                if (r != null) {
                    r.rect(30, 30, 20, 20, 0xFFFF00FF);
                    r.rect(240, 150, 20, 20, 0xFFFF00FF);
                }
                Window w = new Window(20, 20, 200, 120);
                if (u.window("cv", w, 0)) {
                    Canvas wc = u.canvas();
                    if (wc != null) {
                        wc.rect(40, 70, 150, 40, 0xFFFF00FF);
                        wc.rect(0, 0, 400, 300, 0x00000000);
                        wc.rect(250, 20, 30, 30, 0xFF00FFFF);
                    }
                    u.space(26);
                    u.button("Over");
                }
                u.windowEnd();
            } else {
                extra.run();
            }
            IntBuffer px = c.pixels();
            int w = c.w(), h = c.h(), s = c.stride();
            u.end();
            hash = T.hex(T.fnv(px, w, h, s));
        }

        @Override
        public void close() {
            u.close();
            g.close();
        }
    }

    @Test
    void pushChangesPixelsThenPopRestores() {
        try (Kit k = new Kit()) {
            k.frame();
            k.frame();
            String base = k.hash;
            String[] hs = new String[8];
            for (int i = 1; i <= 7; i++) {
                if (i == 6) continue;
                k.push = i;
                k.frame();
                hs[i] = k.hash;
                k.push = 0;
                k.frame();
                assertEquals(base, k.hash, "push " + i + " leaked into the next frame");
            }
            for (int i = 1; i <= 5; i++) assertNotEquals(base, hs[i], "push " + i);
            for (int i = 1; i < 5; i++) assertNotEquals(hs[i], hs[i + 1]);
            assertTrue(k.g.error().contains("pop"), k.g.error());
            assertEquals(base, hs[7]);
            assertEquals("3aad9f4b", base);
            assertEquals("6f70852f", hs[1]);
            assertEquals("d88ed7d3", hs[2]);
            assertEquals("539c0ad8", hs[3]);
            assertEquals("45c11e92", hs[4]);
        }
    }

    @Test
    void unbalancedPushGetsReset() {
        try (Kit k = new Kit()) {
            k.frame();
            k.frame();
            String base = k.hash;
            k.fresh();
            k.push = 6;
            k.frame();
            assertTrue(k.g.error().contains("style stack"), k.g.error());
            k.push = 0;
            k.frame();
            k.frame();
            assertEquals(base, k.hash);
            k.u.pop(2);
            k.u.pushNum(Style.RADIUS, 0);
            k.frame();
            assertEquals(base, k.hash);
        }
    }

    @Test
    void builderPushesAndPopsOnClose() {
        try (Kit k = new Kit()) {
            k.frame();
            k.frame();
            String base = k.hash;
            k.mode = 3;
            String[] got = new String[3];
            k.extra = () -> {
                try (Styled s = k.u.style().radius(0).push()) {
                    assertEquals(1, s.pushed());
                    k.u.button("Push");
                    k.buf = k.u.input("In", k.buf, 16);
                }
                k.u.button("After");
            };
            k.frame();
            got[0] = k.hash;
            k.extra = () -> {
                try (Styled s = k.u.style().inputBg(0xFFC03030).borderWidth(3).push()) {
                    assertEquals(2, s.pushed());
                    k.u.button("Push");
                    k.buf = k.u.input("In", k.buf, 16);
                }
                k.u.button("After");
            };
            k.frame();
            got[1] = k.hash;
            k.extra = () -> {
                Theme t = k.u.theme().copy();
                t.light();
                try (Styled s = k.u.style().theme(t).push()) {
                    k.u.button("Push");
                    k.buf = k.u.input("In", k.buf, 16);
                }
                k.u.button("After");
            };
            k.frame();
            got[2] = k.hash;
            assertEquals("6f70852f", got[0]);
            assertEquals("539c0ad8", got[1]);
            assertEquals("45c11e92", got[2]);
            k.mode = 0;
            k.frame();
            assertEquals(base, k.hash);
            assertThrows(IllegalArgumentException.class, () -> k.u.style().color(Style.RADIUS, 1));
            assertThrows(IllegalArgumentException.class, () -> k.u.style().num(Style.ACCENT, 1));
            assertThrows(IllegalArgumentException.class, () -> k.u.pushColor(Style.PAD, 1));
            assertThrows(IllegalArgumentException.class, () -> k.u.pushNum(Style.TEXT, 1));
        }
    }

    @Test
    void fontAndSkinPushes() throws Exception {
        try (Kit k = new Kit(); Font f = Font.fromBytes(T.data("dejavu-sub.ttf"))) {
            k.frame();
            k.frame();
            String base = k.hash;
            int[] red = new int[64];
            java.util.Arrays.fill(red, 0xFFC03030);
            try (Bitmap bm = Bitmap.fromArgb(red, 8, 8)) {
                k.mode = 3;
                k.extra = () -> {
                    try (Styled s = k.u.style().font(f).push()) {
                        k.u.button("Push");
                        k.buf = k.u.input("In", k.buf, 16);
                    }
                    k.u.button("After");
                };
                k.frame();
                assertNotEquals(base, k.hash);
                String withFont = k.hash;
                k.extra = () -> {
                    k.u.pushSkin(Ui.SKIN_BUTTON, new Skin(bm, 2));
                    k.u.button("Push");
                    k.u.pop();
                    k.buf = k.u.input("In", k.buf, 16);
                    k.u.button("After");
                };
                k.frame();
                assertNotEquals(base, k.hash);
                assertNotEquals(withFont, k.hash);
                assertThrows(IllegalArgumentException.class, () -> k.u.pushSkin(Ui.SKIN_COUNT, null));
                k.mode = 0;
                k.frame();
                assertEquals(base, k.hash);
            }
        }
    }

    @Test
    void posSizeAndStore() {
        try (Kit k = new Kit()) {
            k.mode = 1;
            k.stn = 3;
            k.frame();
            assertEquals(47, k.pos.x());
            assertEquals(31, k.pos.y());
            assertEquals(50, k.pos.w());
            assertEquals(30, k.pos.h());
            assertEquals(10, k.flow.x());
            assertEquals(10, k.flow.y());
            assertEquals(280, k.flow.w());
            assertEquals(120, k.sized.w());
            assertEquals(40, k.sized.h());
            assertTrue(k.sized.y() > k.flow.y());
            assertEquals(160, k.it.x());
            assertEquals(70, k.it.y());
            assertFalse(k.it.hovered());
            assertEquals(0, k.rets);
            assertEquals(3, k.st.capacity());
            assertEquals(0, k.st.get(0));
            assertEquals(0, k.st.get(2));
            k.st.put(0, 4.5).put(2, -1);
            k.st2.put(0, 9);
            k.frame();
            assertEquals(4.5, k.st.get(0));
            assertEquals(-1, k.st.get(2));
            assertEquals(9, k.st2.get(0));
            k.stn = 6;
            k.frame();
            assertEquals(4.5, k.st.get(0));
            assertEquals(-1, k.st.get(2));
            assertEquals(0, k.st.get(5));
            assertThrows(IllegalArgumentException.class, () -> k.u.store("x", 0));
        }
    }

    @Test
    void pressDragAxisLock() {
        try (Kit k = new Kit()) {
            k.mode = 1;
            k.frame();
            k.flags = EnumSet.of(ItemFlag.DRAG_Y);
            k.ev(Event.motion(190, 100));
            k.frame();
            assertTrue(k.it.hovered());
            assertFalse(k.it.pressed());
            assertFalse(k.g.grabbed());
            k.ev(Event.down(190, 100, 1));
            k.frame();
            assertTrue(k.it.pressed() && k.it.held());
            assertTrue(k.g.grabbed());
            k.rets = 0;
            k.ev(Event.motion(203, 117));
            k.frame();
            assertTrue(k.it.held() && !k.it.pressed());
            assertEquals(0, k.it.dx());
            assertEquals(17, k.it.dy());
            assertEquals(17, k.it.dragY());
            assertEquals(0, k.it.dragX());
            assertEquals(1, k.rets);
            k.ev(Event.motion(260, 180));
            k.frame();
            assertTrue(k.it.held() && !k.it.hovered());
            assertEquals(63, k.it.dy());
            assertEquals(80, k.it.dragY());
            assertTrue(k.g.grabbed());
            k.ev(Event.up(260, 180, 1));
            k.frame();
            assertTrue(k.it.released() && !k.it.clicked() && !k.it.held());
            assertFalse(k.g.grabbed());
            k.frame();
            assertFalse(k.it.released());
            assertEquals(0, k.it.dragY());

            k.flags = EnumSet.of(ItemFlag.DRAG_X);
            k.ev(Event.motion(190, 100));
            k.ev(Event.down(190, 100, 1));
            k.frame();
            k.ev(Event.motion(180, 140));
            k.frame();
            assertEquals(-10, k.it.dx());
            assertEquals(0, k.it.dy());
            assertEquals(-10, k.it.dragX());
            assertEquals(0, k.it.dragY());
            k.flags = EnumSet.of(ItemFlag.DRAG_X, ItemFlag.DRAG_Y);
            k.ev(Event.motion(185, 120));
            k.frame();
            assertEquals(5, k.it.dx());
            assertEquals(-20, k.it.dy());
            assertEquals(-5, k.it.dragX());
            assertEquals(20, k.it.dragY());
            k.ev(Event.up(185, 120, 1));
            k.frame();
            assertTrue(k.it.released() && k.it.clicked());
        }
    }

    @Test
    void clicksRepeatAndFocus() {
        try (Kit k = new Kit()) {
            k.mode = 1;
            k.ev(Event.motion(185, 120));
            for (int i = 0; i < 32; i++) k.frame();
            k.ev(Event.down(185, 120, 1));
            k.frame();
            assertTrue(k.it.pressed() && !k.it.focused());
            assertFalse(k.g.grabbed());
            k.ev(Event.up(185, 120, 1));
            k.frame();
            assertTrue(k.it.clicked() && !k.it.doubleClicked());
            k.ev(Event.down(185, 120, 1));
            k.frame();
            assertTrue(k.it.doubleClicked());
            k.ev(Event.up(185, 120, 1));
            k.ev(Event.down(185, 120, 3));
            k.frame();
            assertTrue(k.it.rightClicked() && !k.it.pressed());
            k.ev(Event.up(185, 120, 3));
            k.frame();

            k.flags = EnumSet.of(ItemFlag.REPEAT);
            int clicks = 0;
            k.ev(Event.down(185, 120, 1));
            for (int i = 0; i < 40; i++) {
                k.frame();
                if (k.it.clicked()) clicks++;
            }
            k.ev(Event.up(185, 120, 1));
            k.frame();
            if (k.it.clicked()) clicks++;
            assertTrue(clicks >= 4 && clicks <= 8, String.valueOf(clicks));

            k.flags = EnumSet.of(ItemFlag.FOCUSABLE);
            k.ev(Event.motion(5, 195));
            k.frame();
            assertFalse(k.it.focused());
            k.ev(Event.key(Goome.K_TAB, 0, null));
            k.frame();
            k.frame();
            assertTrue(k.it.focused());
            assertTrue(k.u.wantsKeyboard());
            k.ev(Event.key(Goome.K_LEFT, Goome.SHIFT, null));
            k.frame();
            assertEquals(Goome.K_LEFT | Goome.SHIFT, k.it.key());
            assertFalse(k.it.clicked());
            k.ev(Event.key(Goome.K_ENTER, 0, null));
            k.frame();
            assertEquals(Goome.K_ENTER, k.it.key());
            assertTrue(k.it.clicked());
            k.ev(Event.key(Goome.K_ESC, 0, null));
            k.frame();
            assertEquals(Goome.K_ESC, k.it.key());
            assertFalse(k.it.focused());
            k.frame();
            assertFalse(k.it.focused());
            assertEquals(0, k.it.key());
            k.ev(Event.motion(190, 100));
            k.ev(Event.down(190, 100, 1));
            k.ev(Event.up(190, 100, 1));
            k.frame();
            k.frame();
            assertTrue(k.it.focused());
            k.ev(Event.down(20, 190, 1));
            k.ev(Event.up(20, 190, 1));
            k.frame();
            k.frame();
            assertFalse(k.it.focused());

            k.flags = EnumSet.of(ItemFlag.NO_HIT);
            k.ev(Event.motion(190, 100));
            k.ev(Event.down(190, 100, 1));
            k.frame();
            assertFalse(k.it.hovered() || k.it.pressed());
            k.ev(Event.up(190, 100, 1));
            k.frame();
        }
    }

    @Test
    void canvasDrawsInPlaceAndClips() {
        try (Kit k = new Kit()) {
            k.mode = 2;
            k.ev(Event.motion(290, 190));
            k.frame();
            k.frame();
            assertEquals(0xFFFF00FF, k.px(185, 75));
            assertEquals(0xFFFF00FF, k.px(245, 155));
            assertNotEquals(0xFFFF00FF, k.px(45, 45));
            assertNotEquals(0xFF00FFFF, k.px(265, 35));
            assertEquals(0xFFFF00FF, k.px(189, 100));
            assertNotEquals(0xFFFF00FF, k.px(200, 100));
            int over = 0;
            for (int x = 45; x < 75; x++) if (k.px(x, 95) != 0xFFFF00FF) over++;
            assertTrue(over > 20, String.valueOf(over));
            k.mode = 3;
            Canvas[] keep = new Canvas[1];
            k.extra = () -> keep[0] = k.u.canvas();
            k.frame();
            assertNotNull(keep[0]);
            assertFalse(keep[0].valid());
        }
    }

    @Test
    void hitRects() {
        try (Goome g = Goome.open(new Options().backend(Goome.TEST).testScript("screen 300x200 1; end 100000")); Ui u = g.ui()) {
            for (int i = 0; i < 2; i++) {
                if (u.begin(0) == null) break;
                u.pos(100, 50);
                u.custom("hit", 40, 30);
                u.pos(10, 150);
                u.custom("nohit", 40, 30, ItemFlag.NO_HIT);
                u.end();
            }
            assertTrue(g.hit(125, 75));
            assertFalse(g.hit(105, 45));
            assertFalse(g.hit(30, 175));
        }
    }
}
