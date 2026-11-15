package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OffscreenTest {
    static final String DRAW_HASH = "5858302d";

    static Goome open() {
        return Goome.openOffscreen(200, 200, 2, "engine");
    }

    static String hash(Image f) {
        return T.hex(T.fnv(f.pixels(), f.w, f.h, f.stride));
    }

    static void draw(Goome g) {
        Canvas c = g.begin(0);
        c.rect(10, 10, 20, 20, 0x80FF0000);
        c.circle(120, 50, 30, 0xFF3A7AFE);
        c.rrect(40, 60, 70, 30, 8, 0xCC20C060);
        c.text(44, 66, "offscreen", 0xFFFFFFFF);
        c.hit(10, 10, 20, 20);
        g.end(c);
    }

    static Object[] uiRun(Goome g) {
        boolean check = false;
        String name = "hi";
        try (Ui u = g.ui()) {
            u.theme().animMs(0).font(null).fontSize(14);
            Window win = new Window(6, 6, 180, 0);
            double val = 0.25;
            for (int frame = 0; frame < 6; frame++) {
                if (frame == 1) g.pushClick(25, 64, 1);
                if (frame == 2) {
                    g.push(Event.down(90, 168, 1));
                    g.push(Event.up(90, 168, 1));
                }
                if (frame == 3) g.pushText("z");
                for (Event e : g.events(0)) u.feed(e);
                if (u.begin(0) == null) continue;
                if (u.window("engine", win, Ui.WIN_MOVE)) {
                    check = u.checkbox("Check", check);
                    val = u.slider("Val", val, 0, 1, "%.2f");
                    name = u.input("Name", name, 32);
                }
                u.windowEnd();
                u.end();
            }
        }
        return new Object[] {check, name};
    }

    @Test
    void basics() {
        try (Goome g = open()) {
            assertEquals("offscreen", g.backendName());
            assertEquals(Goome.MODE_OFFSCREEN, g.mode());
            assertTrue(g.can(Goome.CAP_CLIPBOARD) && g.can(Goome.CAP_CURSOR) && !g.can(Goome.CAP_WINDOW));
            Monitor m = g.monitors().get(0);
            assertEquals(200, m.area.w);
            assertEquals(200, m.area.h);
            assertEquals(2.0, m.scale);
            assertEquals(400, m.pw);
            assertEquals("engine", m.name);
            assertThrows(GoomeException.Again.class, g::lastFrame);
            draw(g);
            Image f = g.lastFrame();
            assertEquals(400, f.w);
            assertEquals(400, f.h);
            assertEquals(400, f.stride);
            assertEquals(200, f.area.w);
            assertEquals(0x80800000, f.pixel(25, 25));
            assertEquals(0, f.pixels().get(0));
            assertEquals(0x80, g.alphaAt(15, 15));
            int[] kept = f.copy();
            g.begin(0);
            assertFalse(f.fresh());
            assertThrows(GoomeException.Gone.class, f::pixels);
            assertEquals(0x80800000, kept[25 * 400 + 25]);
        }
    }

    @Test
    void sameHashAsC() throws Exception {
        String got, ui;
        Object[] res;
        boolean hit;
        try (Goome g = open()) {
            draw(g);
            got = hash(g.lastFrame());
            res = uiRun(g);
            ui = hash(g.lastFrame());
            hit = g.hit(10, 10);
        }
        assertEquals(DRAW_HASH, got);
        assertEquals(true, res[0]);
        assertEquals("hiz", res[1]);
        assertTrue(hit);
        String out = T.runC("tests/frame.c");
        if (out == null) return;
        Map<String, String> lines = new HashMap<>();
        for (String l : out.split("\\R")) if (!l.isBlank()) lines.put(l.substring(0, l.indexOf(' ')), l.substring(l.indexOf(' ') + 1));
        assertEquals(got, lines.get("draw"));
        assertEquals(ui + " 1 hiz 1", lines.get("ui"));
    }

    @Test
    void hit() {
        try (Goome g = open()) {
            draw(g);
            assertTrue(g.hit(15, 15));
            assertTrue(g.hit(10, 10));
            assertFalse(g.hit(9.5, 15));
            assertFalse(g.hit(30, 30));
            g.frame(0, c -> c.hit(0, 0, 4, 4), new Rect(100, 100, 5, 5));
            assertTrue(g.hit(102, 102) && g.hit(1, 1));
            assertFalse(g.hit(15, 15));
        }
    }

    @Test
    void push() {
        try (Goome g = open()) {
            g.push(Event.down(12, 13, 1));
            g.push(Event.key('q', 0, "q"));
            var evs = g.events(0);
            assertEquals(2, evs.size());
            assertEquals(Goome.DOWN, evs.get(0).type());
            assertEquals(Goome.KEY, evs.get(1).type());
            assertEquals(12.0, evs.get(0).x());
            assertEquals(13.0, evs.get(0).y());
            assertEquals("q", evs.get(1).text());
            long t = Goome.nowMs();
            assertNull(g.poll(200));
            assertTrue(Goome.nowMs() - t < 100);
            g.resize(300, 150);
            assertEquals(Goome.MONITORS, g.poll(0).type());
            assertArrayEquals(new int[] {300, 150}, g.size());
            Canvas c = g.begin(0);
            assertEquals(600, c.w());
            assertEquals(300, c.h());
            g.end(c);
            g.resize(300, 150, 1);
            assertEquals(300, g.monitors().get(0).pw);
            assertThrows(GoomeException.BadArg.class, () -> g.resize(0, 10, 1));
            assertThrows(GoomeException.NotSupported.class, g::pos);
            assertThrows(GoomeException.NotSupported.class, () -> g.capture(0, new Rect(0, 0, 10, 10)));
        }
    }

    @Test
    void clipboardCursor() {
        try (Goome g = open()) {
            assertEquals("", g.clipboard());
            g.clipboard("hello clipboard, héllo");
            assertEquals("hello clipboard, héllo", g.clipboard());
            assertEquals(Goome.CURSOR_ARROW, g.cursor());
            g.cursor(Goome.CURSOR_IBEAM);
            assertEquals(Goome.CURSOR_IBEAM, g.cursor());
            g.cursor("hand");
            assertEquals(Goome.CURSOR_HAND, g.cursor());
            assertThrows(GoomeException.BadArg.class, () -> g.cursor(99));
            assertThrows(IllegalArgumentException.class, () -> g.cursor("nope"));
            g.title("renamed");
            assertEquals("renamed", g.title());
        }
    }

    @Test
    void two() {
        try (Goome a = open(); Goome b = Goome.open(new Options().backend("offscreen"))) {
            assertArrayEquals(new int[] {960, 640}, b.size());
            Canvas ca = a.begin(0), cb = b.begin(0);
            assertNotSame(ca.pixels(), cb.pixels());
            a.end(ca);
            b.end(cb);
            assertEquals(960 * 640, b.lastFrame().copy().length);
        }
    }

    @Test
    void frameCopyAndDirty() {
        try (Goome g = open()) {
            assertNull(g.frameDirty());
            draw(g);
            Rect d = g.frameDirty();
            assertNotNull(d);
            assertTrue(d.contains(20, 20) && d.contains(240, 100), d.toString());
            assertNull(g.frameDirty());
            Image f = g.lastFrame();
            int stride = 400 * 4 + 16;
            ByteBuffer buf = ByteBuffer.allocateDirect(stride * 400);
            for (PixelFormat fmt : PixelFormat.values()) {
                g.frameCopy(buf, stride, fmt, null);
                ByteBuffer le = buf.duplicate().order(ByteOrder.LITTLE_ENDIAN);
                int p = f.pixel(25, 25);
                int at = 25 * stride + 25 * 4;
                int a = le.get(at + 3) & 255;
                assertEquals(p >>> 24, a, fmt.name());
                int r = le.get(at + (fmt == PixelFormat.BGRA || fmt == PixelFormat.BGRA_PREMUL ? 2 : 0)) & 255;
                int want = (p >>> 16) & 255;
                if (fmt == PixelFormat.BGRA || fmt == PixelFormat.RGBA) want = Math.min(255, (want * 255 + a / 2) / a);
                assertEquals(want, r, fmt.name());
                int blue = f.pixel(240, 100);
                int bat = 100 * stride + 240 * 4;
                assertEquals(blue & 255, le.get(bat + (fmt == PixelFormat.BGRA || fmt == PixelFormat.BGRA_PREMUL ? 0 : 2)) & 255, fmt.name());
            }
            ByteBuffer part = ByteBuffer.allocateDirect(30 * 4 * 30);
            g.frameCopy(part, 30 * 4, PixelFormat.BGRA_PREMUL, new Rect(20, 20, 30, 30));
            ByteBuffer le = part.order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(f.pixel(25, 25), le.getInt(5 * 120 + 5 * 4));
            assertEquals(f.pixel(49, 49), le.getInt(29 * 120 + 29 * 4));
            ByteBuffer big = ByteBuffer.allocateDirect(stride * 400);
            big.position(25 * stride + 25 * 4);
            g.frameCopy(big, stride, PixelFormat.BGRA_PREMUL, new Rect(25, 25, 1, 1));
            assertEquals(f.pixel(25, 25), big.order(ByteOrder.LITTLE_ENDIAN).getInt(25 * stride + 25 * 4));
            assertThrows(GoomeException.BadArg.class, () -> g.frameCopy(ByteBuffer.allocateDirect(1 << 20), 1600, PixelFormat.RGBA, new Rect(390, 0, 20, 5)));
            assertThrows(IllegalArgumentException.class, () -> g.frameCopy(ByteBuffer.allocateDirect(64), 1600, PixelFormat.RGBA, new Rect(0, 0, 20, 5)));
            assertThrows(IllegalArgumentException.class, () -> g.frameCopy(ByteBuffer.allocate(16), 1600, PixelFormat.RGBA, null));
            g.frame(0, c -> c.rect(100, 100, 10, 10, 0xFFFFFFFF));
            Rect small = g.frameDirty();
            assertNotNull(small);
            if (Fn.FRAME_DIRTY.present()) assertTrue(small.contains(205, 205) && small.w < 400, small.toString());
        }
    }
}
