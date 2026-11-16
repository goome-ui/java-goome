package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ImeTest {
    static final String NIHON = "日本", NIHON3 = "にほん";

    static final class Run implements AutoCloseable {
        final Goome g;
        final Ui u;
        final TextBuffer ed = new TextBuffer(96);
        String in = "", ctl = "";
        int flags, hash;

        Run(String script, String in, String ed, int focus) {
            this(null, script, in, ed, focus);
        }

        Run(Font font, String script, String in, String ed, int focus) {
            g = Goome.open(new Options().backend(Goome.TEST).mode(Goome.MODE_WINDOW).size(300, 200).testScript(script));
            u = g.ui();
            u.theme().font(font).fontSize(14).animMs(0);
            u.config().blinkMs(0);
            this.in = in;
            this.ed.text(ed);
            frame(focus);
            frame(0);
        }

        void frame(int focus) {
            for (Event e : g.events(0)) {
                if (e.type() == Goome.CONTROL && e.msg().startsWith("ime ")) ctl = e.msg();
                u.feed(e);
            }
            Canvas c = u.begin(0);
            if (c == null) return;
            if (focus == 1) u.setFocus();
            in = u.inputEx("in", in, 64, Ui.INPUT_NO_SELECT_ON_FOCUS | flags, null);
            if (focus == 2) u.setFocus();
            u.textEdit("ed", ed, 80, 0);
            java.nio.IntBuffer px = c.pixels();
            int w = c.w(), h = c.h(), s = c.stride();
            u.end();
            hash = T.fnv(px, w, h, s);
        }

        void push(Event e) {
            g.push(e);
            frame(0);
        }

        void key(int key, int mods) {
            String t = key >= 32 && key < 127 && (mods & Goome.CTRL) == 0 ? String.valueOf((char) key) : "";
            push(Event.key(key, mods, t));
        }

        @Override
        public void close() {
            ed.close();
            u.close();
            g.close();
        }
    }

    @Test
    void eventsFromTheScript() {
        String longt = "q".repeat(80);
        String sc = "1 text " + longt + ";2 preedit " + NIHON3 + " 3;3 preedit abc;4 preedit;end 10";
        try (Goome g = Goome.open(new Options().backend(Goome.TEST).mode(Goome.MODE_WINDOW).size(100, 100).testScript(sc))) {
            g.push(Event.typed("hi"));
            Event e = g.poll(0);
            assertEquals(Goome.TEXT, e.type());
            assertEquals("hi", e.msg());
            assertEquals("hi", e.fullText());
            assertEquals("hi", g.eventText());
            g.textInput(true, new Rect(10, 20, 1, 16));
            g.textInput(false);
            int got = 0;
            boolean quit = false;
            for (int f = 0; f < 20 && !quit; f++) {
                for (Event ev : g.events(0)) {
                    if (ev.type() == Goome.QUIT) quit = true;
                    if (ev.type() == Goome.TEXT) {
                        got |= 1;
                        assertEquals(63, ev.msg().length());
                        assertEquals(longt, ev.fullText());
                        assertEquals("text", ev.name());
                    }
                    if (ev.type() == Goome.PREEDIT && NIHON3.equals(ev.fullText())) {
                        got |= 2;
                        assertEquals(3, ev.caret());
                        assertEquals(3, g.preeditCursor());
                    }
                    if (ev.type() == Goome.PREEDIT && ev.msg().equals("abc")) {
                        got |= 4;
                        assertEquals(3, g.preeditCursor());
                    }
                    if (ev.type() == Goome.PREEDIT && ev.msg().isEmpty()) {
                        got |= 8;
                        assertEquals(0, g.preeditCursor());
                        assertEquals("", g.eventText());
                    }
                }
                g.frame(0, c -> { });
            }
            assertEquals(15, got);
        }
    }

    @Test
    void preeditAndCommit() {
        int[] h = new int[4];
        int edh;
        try (Run k = new Run("end 100000", "ab", "", 1)) {
            h[0] = k.hash;
            k.push(Event.preedit("xyz", 1));
            h[1] = k.hash;
            k.push(Event.preedit("xyz", 3));
            h[2] = k.hash;
            assertTrue(h[0] != h[1] && h[1] != h[2]);
            assertEquals("ab", k.in);
            k.push(Event.preedit("", 0));
            assertEquals(h[0], k.hash);
            k.push(Event.typed(NIHON));
            assertEquals("ab" + NIHON, k.in);
            k.key('c', 0);
            assertEquals("ab" + NIHON + "c", k.in);
            k.key('z', Goome.CTRL);
            assertEquals("ab" + NIHON, k.in);
            k.key('z', Goome.CTRL);
            assertEquals("ab" + NIHON + "c", k.in);
            k.key('a', Goome.CTRL);
            k.push(Event.typed("é"));
            assertEquals("é", k.in);
            k.key('z', Goome.CTRL);
            assertEquals("ab" + NIHON + "c", k.in);
        }
        try (Run k = new Run("end 100000", "", "line", 2)) {
            h[3] = k.hash;
            k.push(Event.preedit("kana", 2));
            assertNotEquals(h[3], k.hash);
            edh = k.hash;
            k.push(Event.preedit("", 0));
            k.push(Event.typed("かな\n"));
            assertEquals("かな\nline", k.ed.text());
            assertEquals("", k.in);
            k.key('z', Goome.CTRL);
            assertEquals("line", k.ed.text());
        }
        try (Run k = new Run("end 100000", "", "", 1)) {
            k.flags = Ui.INPUT_NUMBER;
            k.push(Event.typed("1a2.5"));
            assertEquals("12.5", k.in);
        }
        assertEquals("2e20540f", T.hex(h[1]));
        assertEquals("ceafcbad", T.hex(h[2]));
        assertEquals("c9c1add5", T.hex(edh));
    }

    @Test
    void longCommitAndCandidateWindow() {
        String longt = "w".repeat(90);
        try (Run k = new Run("4 text " + longt + ";6 ime;end 100000", "", "", 1)) {
            for (int f = 0; f < 8; f++) k.frame(0);
            assertEquals(longt.substring(0, 63), k.in);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("ime (\\d+) (-?\\d+),(-?\\d+) (\\d+)x(\\d+).*").matcher(k.ctl);
            assertTrue(m.matches(), k.ctl);
            assertEquals("1", m.group(1));
            assertTrue(Integer.parseInt(m.group(2)) > 0 && Integer.parseInt(m.group(3)) > 0 && Integer.parseInt(m.group(5)) >= 14, k.ctl);
        }
        try (Run k = new Run("3 ime;end 100000", "", "", 0)) {
            for (int f = 0; f < 5; f++) k.frame(0);
            assertTrue(k.ctl.startsWith("ime 0 "), k.ctl);
        }
    }
}
