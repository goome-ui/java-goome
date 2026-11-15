package io.github.goomeui.mc;

import static org.junit.jupiter.api.Assertions.*;

import io.github.goomeui.Goome;
import io.github.goomeui.Image;
import io.github.goomeui.Rect;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class GoomeViewTest {
    static final class Texture implements GoomeView.Sink {
        ByteBuffer px;
        int w, h, uploads, reallocs;
        Rect last;

        @Override
        public ByteBuffer pixels(int w, int h) {
            if (px == null || w != this.w || h != this.h) {
                px = ByteBuffer.allocateDirect(w * h * 4);
                this.w = w;
                this.h = h;
                reallocs++;
            }
            return px;
        }

        @Override
        public void uploaded(Rect dirty) {
            last = dirty;
            uploads++;
        }

        int rgba(int x, int y) {
            return px.duplicate().order(ByteOrder.LITTLE_ENDIAN).getInt((y * w + x) * 4);
        }
    }

    static int abgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    final Window win = new Window(10, 10, 220, 0);
    int clicks;

    void body(Ui u) {
        if (u.window("test", win, Ui.WIN_MOVE)) {
            if (u.button("Go")) clicks++;
        }
        u.windowEnd();
    }

    @Test
    void uploadsWhatGoomeDrew() {
        try (GoomeView v = new GoomeView(200, 150, 2)) {
            Texture t = new Texture();
            assertFalse(v.upload(t));
            v.frame(new Menu()::draw);
            assertTrue(v.upload(t));
            assertEquals(400, t.w);
            assertEquals(300, t.h);
            assertEquals(new Rect(0, 0, 400, 300), t.last);
            Image f = v.goome().lastFrame();
            for (int[] p : new int[][] {{0, 0}, {60, 60}, {100, 50}, {399, 299}}) assertEquals(abgr(f.pixel(p[0], p[1])), t.rgba(p[0], p[1]));
            int[] all = f.copy();
            for (int i = 0; i < all.length; i += 97) assertEquals(abgr(all[i]), t.rgba(i % 400, i / 400));
            assertFalse(v.upload(t));
            v.resize(300, 150, 1);
            v.frame(new Menu()::draw);
            assertTrue(v.upload(t));
            assertEquals(2, t.reallocs);
            assertEquals(new Rect(0, 0, 300, 150), t.last);
        }
    }

    @Test
    void eatsWhatGoomeWants() {
        try (GoomeView v = new GoomeView(400, 360, 1)) {
            v.ui().theme().font(null).fontSize(14);
            v.frame(this::body);
            assertTrue(v.over(35, 71));
            assertFalse(v.over(390, 350));
            assertTrue(v.mouseDown(35, 71, 1));
            assertTrue(v.mouseMoved(36, 71));
            assertTrue(v.mouseUp(35, 71, 1));
            v.frame(this::body);
            assertEquals(1, clicks);
            v.mouseMoved(390, 350);
            v.frame(this::body);
            assertFalse(v.mouseDown(390, 350, 1));
            assertFalse(v.mouseUp(390, 350, 1));
            assertFalse(v.scroll(390, 350, 1));
            assertFalse(v.wantsKeyboard());
            assertFalse(v.text("e", 0));
            assertFalse(v.key(Goome.K_ESC, 0));
            assertTrue(v.mouseDown(40, 20, 1));
            v.frame(this::body);
            assertTrue(v.mouseMoved(90, 40));
            v.frame(this::body);
            assertTrue(v.mouseUp(90, 40, 1));
            v.frame(this::body);
            assertEquals(60.0, win.x());
            assertEquals(30.0, win.y());
        }
    }

    @Test
    void typingGoesToGoome() {
        String[] text = {""};
        Window w = new Window(10, 10, 220, 0);
        try (GoomeView v = new GoomeView(400, 360, 1)) {
            v.ui().theme().font(null).fontSize(14);
            java.util.function.Consumer<Ui> body = u -> {
                if (u.window("t", w, Ui.WIN_MOVE)) text[0] = u.input("Text", text[0], 32);
                u.windowEnd();
            };
            v.frame(body);
            for (int y = 40; y < 140 && !v.wantsKeyboard(); y += 4) {
                assertTrue(v.mouseDown(120, y, 1));
                v.mouseUp(120, y, 1);
                v.frame(body);
            }
            assertTrue(v.wantsKeyboard());
            assertTrue(v.text("hi", 0));
            assertTrue(v.key(Goome.K_BACKSPACE, 0));
            assertTrue(v.text("!", 0));
            v.frame(body);
            assertEquals("h!", text[0]);
            v.pasteFrom("pasted");
            assertTrue(v.key('v', Goome.CTRL));
            v.frame(body);
            assertEquals("h!pasted", text[0]);
            assertNull(v.copied());
        }
    }

    @Test
    void keyMaps() {
        assertEquals('a', Keys.sdlScancode(4));
        assertEquals('z', Keys.sdlScancode(29));
        assertEquals('1', Keys.sdlScancode(30));
        assertEquals('0', Keys.sdlScancode(39));
        assertEquals(Goome.K_ESC, Keys.sdlScancode(41));
        assertEquals(Goome.K_F1 + 11, Keys.sdlScancode(69));
        assertEquals(Goome.K_LEFT, Keys.sdlScancode(80));
        assertEquals(Goome.SHIFT | Goome.CTRL, Keys.sdlMods(0x0001 | 0x0080));
        assertEquals(Goome.ALT | Goome.SUPER, Keys.sdlMods(0x0200 | 0x0400));
        assertEquals('g', Keys.glfwKey(71));
        assertEquals(Goome.K_ENTER, Keys.glfwKey(335));
        assertEquals(Goome.CTRL, Keys.glfwMods(2));
        assertTrue(Keys.printable('a', Goome.SHIFT));
        assertFalse(Keys.printable('a', Goome.CTRL));
        assertFalse(Keys.printable(Goome.K_ESC, 0));
    }

    @Test
    void hud() {
        Menu m = new Menu();
        m.hudCorner = 3;
        assertArrayEquals(new int[] {320 - Menu.HUD_W - 4, 200 - Menu.HUD_H - 4}, m.hudSpot(320, 200));
        try (GoomeView v = new GoomeView(Menu.HUD_W, Menu.HUD_H, 2)) {
            Texture t = new Texture();
            v.frame(u -> m.hud(u, () -> "x 1 y 2 z 3"));
            assertTrue(v.upload(t));
            assertEquals(Menu.HUD_W * 2, t.w);
            assertNotEquals(0, t.rgba(Menu.HUD_W, Menu.HUD_H) >>> 24);
        }
    }
}
