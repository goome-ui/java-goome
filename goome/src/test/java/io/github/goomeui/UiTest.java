package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class UiTest {
    static final String SCRIPT = "screen 400x360 1; 1 click 35,71; 2 click 29,106; 3 hold 40,157 6; 4 hover 120,157; "
        + "7 click 100,213; 8 key ab; 9 key \\n; 10 key zz; 11 click 120,273; 13 click 120,206; "
        + "15 click 300,20; 16 hold 100,20 20; 18 hover 150,40; end 22";

    @Test
    void scripted() {
        try (Goome g = Goome.open(new Options().backend(Goome.TEST).testScript(SCRIPT)); Ui u = g.ui()) {
            u.theme().font(null).fontSize(14);
            Window win = new Window(10, 10, 220, 0);
            double val = 0;
            boolean check = false, quit = false, kbSeen = false;
            int pick = 0, frames = 0, clicks = 0, downs = 0, keysOff = 0;
            String text = "";
            Map<Integer, String> hs = new HashMap<>();
            while (!quit && frames < 100) {
                for (Event e : g.events(0)) {
                    if (e.type() == Goome.QUIT) quit = true;
                    if (e.type() == Goome.DOWN) downs++;
                    if (e.type() == Goome.KEY && frames > 10) keysOff++;
                    u.feed(e);
                }
                Canvas c = u.begin(0);
                assertNotNull(c);
                if (u.window("test", win, Ui.WIN_MOVE)) {
                    if (u.button("Go")) clicks++;
                    check = u.checkbox("Check", check);
                    val = u.slider("Val", val, 0, 1);
                    text = u.input("Text", text, 16);
                    pick = u.combo("Pick", pick, "one", "two", "three");
                }
                u.windowEnd();
                java.nio.IntBuffer px = c.pixels();
                int cw = c.w(), ch = c.h(), cs = c.stride();
                u.end();
                String h = T.hex(T.fnv(px, cw, ch, cs));
                assertFalse(c.valid());
                if (frames == 8) kbSeen = !text.isEmpty();
                if (frames == 0 || frames == 12 || frames == 21) hs.put(frames, h);
                frames++;
            }
            assertEquals(1, clicks);
            assertTrue(check);
            assertTrue(val > 0.45 && val < 0.55, String.valueOf(val));
            assertTrue(kbSeen);
            assertEquals("ab", text);
            assertEquals(0, keysOff);
            assertEquals(1, pick);
            assertEquals(60.0, win.x());
            assertEquals(30.0, win.y());
            assertEquals(7, downs);
            assertEquals(Map.of(0, "51713294", 12, "b52f16c5", 21, "ea01692c"), hs);
        }
    }

    @Test
    void widgets() {
        try (Goome g = Goome.open(new Options().backend("test").testScript("screen 800x600 1; 2 click 60,80; end 6")); Ui u = g.ui()) {
            Theme t = u.theme();
            t.font(null);
            t.light();
            assertTrue(t.text() >>> 24 > 0);
            t.dark();
            t.accent(0xFF6FDC5F);
            assertEquals(0xFF6FDC5F, t.accent());
            t.radius(4).pad(8).animMs(0);
            assertEquals(4.0, t.radius());
            assertEquals(8.0, t.pad());
            assertThrows(IllegalArgumentException.class, () -> t.color("nope"));
            assertThrows(IllegalArgumentException.class, () -> t.size("font"));
            Window win = new Window(20, 20, 400, 0), stats = new Window(500, 20, 200, 0);
            int[] blue = new int[64];
            java.util.Arrays.fill(blue, 0xFF2A409A);
            try (Bitmap logo = Bitmap.fromArgb(blue, 8, 8)) {
                int tab = 0, n = 3, color = 0xFF4FA3FF, mode = 1;
                double vol = 0.5;
                boolean on = false;
                Flag more = new Flag(true);
                String name = "hi";
                for (int i = 0; i < 8; i++) {
                    for (Event e : g.events(0)) u.feed(e);
                    if (u.begin(0) == null) continue;
                    if (u.window("main", win, Ui.WIN_MOVE | Ui.WIN_RESIZE | Ui.WIN_COLLAPSE)) {
                        tab = u.tabs("tabs", tab, "A", "B");
                        u.pushId(tab);
                        if (u.scrollBegin("page", 0)) {
                            u.heading("Head");
                            u.row(2);
                            u.button("One");
                            u.button("Two");
                            u.tooltip("tip");
                            u.hovered();
                            u.label("100% sure %s %d");
                            u.wrapped("some long text that wraps around a bit");
                            u.separator();
                            u.space(4);
                            u.width(120);
                            on = u.toggle("Toggle", on);
                            mode = u.radio("Small", mode, 0);
                            u.sameLine();
                            mode = u.radio("Big", mode, 1);
                            vol = u.slider("Vol", vol, 0, 1, "%.2f");
                            n = u.sliderInt("N", n, 0, 10);
                            u.progress(vol, (int) (vol * 100) + "%");
                            u.progress(-1);
                            color = u.color("Color", color);
                            name = u.input("Name", name, 256);
                            if (u.collapsing("More", more)) for (int k = 0; k < 3; k++) u.selectable("Item " + k, k == 1);
                            u.collapsing("Self kept");
                            u.image(logo, 32, 32, 6);
                            u.imageButton("img", logo, 24, 24);
                        }
                        u.scrollEnd();
                        u.popId();
                    }
                    u.windowEnd();
                    u.window("Stats", stats, Ui.WIN_MOVE | Ui.WIN_CLOSE, () -> u.label("stats"));
                    u.hit(0, 0, 4, 4);
                    u.animating();
                    u.end();
                }
                assertTrue(stats.open());
                stats.open(false);
                u.begin(0);
                assertFalse(u.window("Stats", stats, Ui.WIN_CLOSE));
                u.windowEnd();
                u.end();
            }
        }
    }

    static int labelHash(Consumer<Ui> fn) {
        try (Goome g = Goome.open(new Options().backend("test").testScript("screen 300x200 1; end 3")); Ui u = g.ui()) {
            u.theme().font(null);
            Window win = new Window(10, 10, 200, 0);
            Canvas c = u.begin(0);
            if (u.window("w", win, 0)) fn.accept(u);
            u.windowEnd();
            java.nio.IntBuffer px = c.pixels();
            int cw = c.w(), ch = c.h(), cs = c.stride();
            u.end();
            return T.fnv(px, cw, ch, cs);
        }
    }

    @Test
    void labelIsNotAFormat() {
        int plain = labelHash(u -> {
            try (Scratch s = Scratch.push()) {
                Native.call(Fn.UI_LABEL, u.h(), s.str("100%% sure %%s"), 0);
            }
        });
        assertEquals(plain, labelHash(u -> u.label("100% sure %s")));
        assertEquals(plain, labelHash(u -> u.label("%d%% sure %s", 100, "%s")));
    }

    @Test
    void fontLifetime() throws Exception {
        try (Goome g = Goome.open(new Options().backend("test").testScript("end 3"))) {
            Ui u = g.ui();
            Font f = Font.fromBytes(T.data("dejavu-sub.ttf"));
            u.theme().font(f);
            assertSame(f, u.theme().font());
            f.close();
            assertNull(u.theme().font());
            Font sys = Font.system(null);
            if (sys != null) {
                u.theme().font(sys);
                u.frame(0, c -> u.window("w", new Window(40, 40, 300, 0), Ui.WIN_MOVE, () -> u.label("hi")));
                u.theme().font(null);
                sys.close();
            }
            u.close();
            u.close();
            assertThrows(GoomeException.Gone.class, () -> u.button("x"));
        }
        Goome g = Goome.open(new Options().backend("test").testScript("end 3"));
        Ui u = g.ui();
        g.close();
        assertTrue(u.closed());
    }

    @Test
    void everythingElse() throws Exception {
        try (Goome g = Goome.openOffscreen(640, 520, 1, "rest"); Ui u = g.ui(); TextBuffer doc = new TextBuffer(4096, "hello\nworld")) {
            u.theme().animMs(0).font(null).fontSize(14);
            Flag wrap = new Flag(false), tree = new Flag(true), modal = new Flag(true), checked = new Flag(false);
            Split pane = new Split(0.3, false);
            double speed = 1.5, count = 2;
            int drag = 4;
            byte[] dropped = null;
            for (int f = 0; f < 6; f++) {
                if (f == 2) g.pushClick(60, 12, 1);
                for (Event e : g.events(0)) u.feed(e);
                if (u.begin(0) == null) continue;
                if (u.app("rest", Ui.APP_MENUBAR | Ui.APP_STATUSBAR)) {
                    if (u.menubarBegin()) {
                        if (u.menuBegin("File")) {
                            u.menuItem("New", "ctrl+n");
                            u.menuSeparator();
                            u.menuItem("Wrap", null, wrap);
                            u.menuEnd();
                        }
                        u.menubarEnd();
                    }
                    if (u.splitBegin("main", pane)) {
                        if (u.tree("Tree", tree)) {
                            u.label("leaf");
                            u.treeEnd();
                        }
                        u.link("a link");
                        u.badge("new");
                        u.spinner(16);
                        u.splitNext();
                        u.textEdit("doc", doc, 80, Ui.EDIT_LINE_NUMBERS | (wrap.get() ? Ui.EDIT_WRAP : 0));
                        if (u.tableBegin("t", 2, 80, Ui.TABLE_SORTABLE | Ui.TABLE_STRIPES)) {
                            u.tableColumn("a", 0);
                            u.tableColumn("b", 60);
                            Sort s = u.tableSort(0, false);
                            assertEquals(0, s.col);
                            for (int r = 0; r < 3; r++) {
                                u.tableRow();
                                u.label("r" + r);
                                u.tableNext();
                                u.label("x");
                            }
                            u.tableEnd();
                        }
                        speed = u.dragFloat("Speed", speed, 0.1, 0, 10, "%.1f");
                        drag = u.dragInt("Drag", drag, 1, 0, 10);
                        count = u.number("Count", count, 1, "%.0f");
                        u.plotLines("lines", new float[] {1, 3, 2, 5}, 40);
                        u.plotBars("bars", new float[] {1, 3, 2, 5}, 0, 6, 40);
                        u.button("src");
                        u.dragSource("text", "dragged");
                        u.button("dst");
                        byte[] got = u.dropTarget("text", 64);
                        if (got != null) dropped = got;
                        if (u.scrollBegin("list", 60)) {
                            int[] r = u.listClip(1000, 0);
                            assertTrue(r[0] >= 0 && r[1] >= r[0] && r[1] <= 1000);
                            for (int k = r[0]; k < r[1]; k++) u.label("row " + k);
                        }
                        u.scrollEnd();
                        u.button("ctx");
                        if (u.contextBegin("ctx")) {
                            u.menuItem("Copy", null, checked);
                            u.contextEnd();
                        }
                        if (f == 1) u.popupOpen("pop");
                        if (u.popupBegin("pop")) {
                            u.label("popup");
                            if (f == 4) u.popupClose();
                            u.popupEnd();
                        }
                        u.splitEnd();
                    }
                    if (modal.get() && u.modalBegin("Sure?", modal)) {
                        u.label("really");
                        if (f == 3) modal.set(false);
                        u.modalEnd();
                    }
                    u.setFocus();
                    u.button("focus me");
                    u.focused();
                    if (f == 0) u.toast("hi", 1000);
                    u.statusbar("ready");
                }
                u.appEnd();
                u.end();
            }
            assertEquals("hello\nworld", doc.text());
            assertNull(dropped);
            assertTrue(u.wants() >= 0);
            u.autoGrab(0);
            assertEquals(0, u.autoGrab());
            assertNotNull(g.lastFrame());
        }
    }

    @Test
    void app() {
        App a = new App("app", 400, 300).options(new Options().backend("test").testScript("screen 1x1 1; 2 click 30,12; 4 key hello; 6 close; end 10"));
        int[] seen = new int[2];
        a.onFrame(u -> {
            u.label("frame " + a.frames());
            if (u.button("Hi")) seen[0]++;
        });
        a.onEvent(e -> {
            if (e.type() == Goome.KEY) seen[1]++;
            return false;
        });
        a.run();
        assertTrue(a.frames() > 3);
        assertFalse(a.running());
    }
}
