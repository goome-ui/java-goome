package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class CoreTest {
    @Test
    void combos() {
        assertArrayEquals(new int[] {'h', Goome.ALT}, Goome.parseCombo("alt+h"));
        assertArrayEquals(new int[] {Goome.K_F1 + 4, Goome.CTRL | Goome.SHIFT}, Goome.parseCombo("Ctrl+Shift+F5"));
        assertArrayEquals(new int[] {'/', Goome.ALT | Goome.SHIFT}, Goome.parseCombo("alt+?"));
        assertArrayEquals(new int[] {' ', Goome.SUPER}, Goome.parseCombo("super+space"));
        assertArrayEquals(new int[] {Goome.K_LEFT, Goome.SUPER}, Goome.parseCombo("cmd+left"));
        assertArrayEquals(new int[] {'h', Goome.SHIFT}, Goome.parseCombo("H"));
        for (String c : new String[] {"alt+", "alt+f13", ""}) assertThrows(GoomeException.class, () -> Goome.parseCombo(c));
    }

    @Test
    void pacer() {
        try (Pacer p = new Pacer(50)) {
            assertEquals(0, p.next());
            int w = p.next();
            assertTrue(w > 0 && w <= 20, String.valueOf(w));
        }
        long t = Goome.nowMs();
        assertTrue(t >= 0);
        assertTrue(Goome.nowMs() >= t);
    }

    static final String SCRIPT = "screen 640x360 2; 1 click 20,20; 1 click 300,300; 2 hotkey alt+j; 3 key Hi\\b; 4 send ping; 5 scroll 30,30 -1; 6 hold 25,25 8; end 10";

    @Test
    void scripted() throws Exception {
        Goome g = Goome.open(new Options().title("t").backend(Goome.TEST).testScript(SCRIPT));
        Image again;
        try {
            assertEquals("test", g.backendName().split(" ")[0]);
            List<Monitor> mons = g.monitors();
            assertEquals(1, mons.size());
            Monitor m = mons.get(0);
            assertEquals(640, m.area.w);
            assertEquals(2.0, m.scale);
            assertEquals(1280, m.pw);
            g.hotkey("alt+j", 7);
            g.keyboard(true);
            int down = 0, up = 0, hot = 0, keys = 0, ctl = 0, scroll = 0, held = 0, frames = 0;
            StringBuilder text = new StringBuilder();
            boolean quit = false;
            while (!quit && frames < 100) {
                Canvas c = g.begin(0);
                assertNotNull(c);
                assertEquals(2.0, c.scale());
                assertEquals(1280, c.w());
                c.rect(10, 10, 40, 40, 0xFF336699);
                c.hit(10, 10, 40, 40);
                g.end(c);
                assertFalse(c.valid());
                frames++;
                Pointer p = g.pointer();
                if (p != null && p.buttons != 0) held++;
                for (Event e : g.events(1)) {
                    switch (e.type()) {
                        case Goome.DOWN: down++; break;
                        case Goome.UP: up++; break;
                        case Goome.HOTKEY: if (e.id() == 7) hot++; break;
                        case Goome.KEY: keys++; text.append(e.text()); break;
                        case Goome.CONTROL: if (e.msg().equals("ping")) ctl++; break;
                        case Goome.SCROLL: if (e.dy() == -1) scroll++; break;
                        case Goome.QUIT: quit = true; break;
                        default:
                    }
                    assertNotNull(e.toString());
                }
            }
            assertEquals(2, down);
            assertEquals(2, up);
            assertEquals(1, hot);
            assertEquals(3, keys);
            assertEquals("Hi", text.toString());
            assertEquals(1, ctl);
            assertEquals(1, scroll);
            assertTrue(quit);
            assertTrue(held >= 1);

            Image img = g.capture(0, new Rect(10, 10, 40, 40));
            assertEquals(80, img.w);
            assertEquals(80, img.h);
            assertTrue(img.hasSelf);
            assertEquals(0xFF336699, img.pixel(0, 0));
            assertEquals(0xFF336699, img.pixels().get(0));
            assertEquals(80 * img.stride, img.pixels().limit());
            int[] kept = img.copy();
            assertEquals(80 * 80, kept.length);
            assertEquals(0xFF336699, kept[0]);
            assertEquals(255, g.alphaAt(20, 20));
            assertEquals(0, g.alphaAt(200, 200));

            again = g.capture(0);
            assertFalse(img.fresh());
            assertThrows(GoomeException.Gone.class, img::pixels);
            assertEquals(1280, again.w);
            assertEquals(720, again.h);
            Path p = T.tmp("grab.ppm");
            again.savePpm(p.toString());
            byte[] head = Files.readAllBytes(p);
            assertEquals('P', head[0]);
            assertEquals('6', head[1]);
        } finally {
            g.close();
        }
        assertTrue(g.closed());
        assertThrows(GoomeException.Gone.class, g::monitors);
        assertFalse(again.fresh());
    }

    @Test
    void badBackend() {
        assertThrows(IllegalArgumentException.class, () -> new Options().backend("nope"));
    }

    @Test
    void framesMissing() {
        GoomeException e = assertThrows(GoomeException.class,
            () -> Goome.open(new Options().backend("test").testFrames(T.ROOT.resolve("no/such/frames").toString())));
        assertEquals(Goome.ENODISPLAY, e.code());
        assertTrue(e instanceof GoomeException.NoDisplay);
    }

    @Test
    void closeTwice() {
        Goome g = Goome.open(new Options().backend(Goome.TEST).testScript("end 3"));
        Canvas c = g.begin(0);
        g.close();
        g.close();
        assertFalse(c.valid());
        assertThrows(GoomeException.Gone.class, () -> c.rect(0, 0, 1, 1, -1));
        assertThrows(GoomeException.Gone.class, () -> g.poll(0));
    }

    @Test
    void frameHelper() {
        try (Goome g = Goome.open(new Options().backend(Goome.TEST).testScript("end 3"))) {
            assertNull(g.controlPath());
            Canvas[] seen = new Canvas[1];
            g.frame(0, c -> {
                seen[0] = c;
                c.rect(0, 0, 10, 10, 0xFFFF0000);
            }, new Rect(0, 0, 10, 10));
            assertFalse(seen[0].valid());
            assertEquals(255, g.alphaAt(5, 5));
        }
    }

    @Test
    void strerror() {
        assertFalse(Goome.strerror(Goome.ENOTSUP).isEmpty());
    }

    @Test
    void control() throws Exception {
        Path d = T.tmp("state");
        Files.createDirectories(d);
        Goome g;
        try {
            g = Goome.open(new Options().backend(Goome.TEST).stateDir(d.toString()).testScript("end 50"));
        } catch (GoomeException e) {
            Assumptions.abort("no state dir here: " + e.getMessage());
            return;
        }
        try (Goome gg = g) {
            Assumptions.assumeTrue(gg.controlPath() != null, "no control fifo on this os");
            Goome.send(d.toString(), "hello there");
            String got = null;
            for (int k = 0; k < 40 && got == null; k++) {
                gg.frame(0, c -> {});
                for (Event e : gg.events(5)) if (e.type() == Goome.CONTROL) got = e.msg();
            }
            assertEquals("hello there", got);
        }
    }

    @Test
    void events() {
        Event e = Event.key('q', Goome.CTRL, "q");
        assertEquals("Event(key key 113 mods 0x200 text 'q')", e.toString());
        assertEquals("down", Event.down(1, 2, 1).name());
        assertEquals(6, Event.cut("éééé", 8).length);
    }
}
