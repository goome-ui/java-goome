package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;

class AnythingTest {
    static final int KIT = 0, COMBO = 1, INPUT = 2;
    static final String KIT_HASH = "d064e9fc";

    static final class Kit implements AutoCloseable {
        final Goome g;
        final Ui u;
        final int scene;
        final Flag open = new Flag(true);
        final TextBuffer ed = new TextBuffer(32, "goome HiAVWTy");
        Theme push;
        String buf = "hello you", hint, hash;
        boolean on = true, focused, setFocus, pw;
        int rad = 1, sel, tab, flags, ret;
        double val = 0.4;

        Kit(int scene) {
            this.scene = scene;
            g = Goome.open(new Options().backend(Goome.TEST).mode(Goome.MODE_WINDOW).size(420, 700).testScript("end 100000"));
            u = g.ui();
            u.theme().font(null).fontSize(14).animMs(0);
            u.config().blinkMs(0);
        }

        void key(int key, int mods) {
            String text = key >= 32 && key < 127 && (mods & Goome.CTRL) == 0 ? String.valueOf((char) key) : "";
            g.push(Event.key(key, mods, text).button(1));
        }

        void frame() {
            for (Event e : g.events(0)) u.feed(e);
            Canvas c = u.begin(0);
            assertNotNull(c);
            if (push != null) u.pushTheme(push);
            if (scene == KIT) kit();
            else if (scene == COMBO) sel = u.combo("##c", sel, "one", "two", "three");
            else {
                u.button("Hi");
                if (setFocus) {
                    u.setFocus();
                    setFocus = false;
                }
                buf = pw ? u.password("in", buf, 24) : u.inputEx("in", buf, 24, flags, hint);
                ret = u.entered() ? 2 : u.changed() ? 1 : 0;
            }
            if (push != null) u.pop(1);
            IntBuffer px = c.pixels();
            int w = c.w(), h = c.h(), s = c.stride();
            u.end();
            hash = T.hex(T.fnv(px, w, h, s));
        }

        private void kit() {
            Window win = new Window(10, 10, 300, 680);
            if (u.window("kit", win, Ui.WIN_RESIZE)) {
                if (u.tree("tree", open)) {
                    u.label("leaf");
                    u.treeEnd();
                }
                u.plotLines("p", new float[] {1, 3, 2, 5, 4, 6}, 0, 7, 40);
                u.textEdit("ed", ed, 40, Ui.EDIT_MONO);
                u.row(2);
                u.link("goome");
                u.badge("Hi", 0);
                tab = u.tabs("t", tab, "A", "B", "C");
                u.heading("goome Hi");
                u.label("goome");
                u.button("Hi");
                on = u.checkbox("c", on);
                on = u.toggle("t", on);
                rad = u.radio("r", rad, 1);
                val = u.slider("s", val, 0, 1, "%.1f");
                if (!focused) {
                    u.setFocus();
                    focused = true;
                }
                buf = u.input("in", buf, 24);
                u.separator();
                for (int i = 0; i < 40; i++) u.label("row %d", i);
            }
            u.windowEnd();
        }

        @Override
        public void close() {
            ed.close();
            u.close();
            g.close();
        }
    }

    static String kit(Theme t) {
        try (Kit k = new Kit(KIT)) {
            k.push = t;
            for (int f = 0; f < 6; f++) k.frame();
            return k.hash;
        }
    }

    static String typed(int flags, String text, String hint, boolean focus, String type, int[] ret, String[] out) {
        try (Kit k = new Kit(INPUT)) {
            k.flags = flags;
            k.hint = hint;
            k.setFocus = focus;
            k.buf = text;
            k.g.clipboard("paste9");
            k.frame();
            for (int i = 0; type != null && i < type.length(); i++) {
                char c = type.charAt(i);
                if (c == '^') k.key(type.charAt(++i), Goome.CTRL);
                else if (c == '\n') k.key(Goome.K_ENTER, 0);
                else k.key(c, 0);
                k.frame();
                if (ret != null && k.ret != 0) ret[0] = k.ret;
            }
            k.frame();
            if (out != null) out[0] = k.buf + "|" + k.g.clipboard();
            return k.hash;
        }
    }

    @Test
    void presetsAndMakeMatchC() {
        assertEquals(KIT_HASH, kit(null));
        String[] want = {"d064e9fc", "20984a84", "bedf5364", "dc6d4703", "f4f61716", "fd60aa64", "86f84612"};
        int all = 0x811C9DC5;
        for (int i = 0; i < Theme.PRESET_COUNT; i++) {
            Theme p = Theme.preset(i).fontSize(14);
            String h = kit(p);
            assertEquals(want[i], h, "preset " + i);
            all = (all ^ Integer.parseUnsignedInt(h, 16)) * 16777619;
        }
        assertThrows(IllegalArgumentException.class, () -> Theme.preset(Theme.PRESET_COUNT));
        Theme d = Theme.preset(Theme.PRESET_DARK);
        assertEquals(18, d.checkSize());
        assertEquals(38, d.toggleW());
        assertTrue(d.hoverMs() < 0);
        assertEquals(0, d.focus());
        int[][] makes = {{0xFF8B5CF6, 1, 12}, {0xFF2F7BF6, 0, 20}, {0xFFFFD60A, 1, 0}, {0x00E5484D, 0, 6}};
        String[] wantMake = {"35faaf7a", "5edb0223", "8fdeb769", "0a59d080"};
        Theme p = null;
        for (int i = 0; i < 4; i++) {
            p = Theme.make(makes[i][0], makes[i][1] != 0, makes[i][2]);
            assertEquals(makes[i][0] | 0xFF000000, p.accent());
            assertEquals(makes[i][2], p.radius());
            String h = kit(p.fontSize(14));
            assertEquals(wantMake[i], h, "make " + i);
            all = (all ^ Integer.parseUnsignedInt(h, 16)) * 16777619;
        }
        assertEquals(0xFFFFFFFF, p.accentText());
        assertEquals("315ea656", T.hex(all));
    }

    @Test
    void scaleGrowsEverythingAndComesBack() {
        try (Kit k = new Kit(KIT)) {
            Theme t = k.u.theme();
            k.u.scale(2);
            assertEquals(28, t.fontSize());
            assertEquals(20, t.pad());
            assertEquals(8, t.scrollW());
            assertEquals(6, t.focusGap());
            assertEquals(28, t.tipX());
            assertEquals(10, t.shadowY());
            assertEquals(18, t.checkSize());
            assertEquals(16, t.indent());
            assertEquals(0, t.align());
            k.frame();
            assertEquals("27b6a55e", k.hash);
            k.u.scale(0.5);
            assertEquals(14, t.fontSize());
            k.frame();
            assertEquals(KIT_HASH, k.hash);
            k.u.scale(0);
            k.u.scale(-1);
            assertEquals(14, t.fontSize());
        }
    }

    @Test
    void makeFromABaseKeepsItsFonts() throws Exception {
        try (Font f = Font.fromBytes(T.data("dejavu-sub.ttf"))) {
            Theme b = Theme.preset(Theme.PRESET_DARK).font(f).fontMono(f);
            Theme m = Theme.make(b, 0xFF8B5CF6, true, 12);
            assertSame(f, m.font());
            assertSame(f, m.fontMono());
            assertNull(m.fontSmall());
            assertEquals(0xFF8B5CF6, m.accent());
            assertSame(f, Theme.preset(b, Theme.PRESET_ROSE).font());
            assertNull(Theme.make(0xFF8B5CF6, true, 12).font());
        }
    }

    @Test
    void everyNewFieldReadsBack() {
        Theme t = Theme.preset(Theme.PRESET_DARK);
        t.focus(0xFF112233).dim(0xC0000000).caretW(8).toastFadeMs(2000).align(Theme.ALIGN_RIGHT).ease(Theme.EASE_LINEAR);
        assertEquals(0xFF112233, t.focus());
        assertEquals(0xC0000000, t.dim());
        assertEquals(8, t.caretW());
        assertEquals(2000, t.toastFadeMs());
        assertEquals(Theme.ALIGN_RIGHT, t.align());
        assertEquals(Theme.EASE_LINEAR, t.ease());
        for (Style s : Style.values()) {
            if (s.color()) t.color(s.field(), 0x12345678);
            else t.size(s.field(), 3.5);
            assertEquals(s.color() ? 0x12345678 : 3.5, s.color() ? t.color(s.field()) : t.size(s.field()), s.name());
        }
        assertTrue(Style.DIM.color() && Style.FOCUS.color() && !Style.CHECK_SIZE.color() && !Style.TOAST_FADE_MS.color());
        assertEquals(72, Style.values().length);
        assertThrows(IllegalArgumentException.class, () -> t.size("font_heading"));
        assertNull(t.fontHeading());
        assertNull(t.fontMono());
        assertNull(t.fontSmall());
    }

    @Test
    void keymapLikeC() {
        try (Kit k = new Kit(KIT)) {
            Config cfg = k.u.config();
            assertEquals(500, cfg.tipDelayMs());
            assertEquals(450, cfg.dblClickMs());
            assertEquals(0, cfg.blinkMs());
            assertTrue(cfg.tabNav() && cfg.escCloses() && cfg.raiseOnClick() && !cfg.focusOnMouse());
            assertEquals(Goome.CURSOR_IBEAM, cfg.cursor(Config.CUR_INPUT));
            assertEquals('c', cfg.key(Config.ACT_COPY, 0));
            assertEquals(Goome.CTRL, cfg.mods(Config.ACT_COPY, 0));
            assertEquals('z', cfg.key(Config.ACT_REDO, 1));
            assertEquals(Goome.CTRL | Goome.SHIFT, cfg.mods(Config.ACT_REDO, 1));
            k.frame();
            k.key('c', Goome.CTRL);
            k.frame();
            assertEquals("hello you", k.g.clipboard());
            assertTrue(cfg.bind(Config.ACT_COPY, "ctrl+k"));
            assertTrue(cfg.bind(Config.ACT_SELECT_ALL, "alt+l, ctrl+q"));
            assertEquals(Goome.ALT, cfg.mods(Config.ACT_SELECT_ALL, 0));
            assertEquals('q', cfg.key(Config.ACT_SELECT_ALL, 1));
            assertEquals(0, cfg.key(Config.ACT_SELECT_ALL, 2));
            assertThrows(IllegalArgumentException.class, () -> cfg.bind(Config.ACT_COUNT, "ctrl+k"));
            assertFalse(cfg.bind(Config.ACT_UNDO, "ctrl+nope"));
            assertTrue(cfg.bind(Config.ACT_PASTE, null));
            assertEquals(0, cfg.key(Config.ACT_PASTE, 0));
            k.g.clipboard("zz");
            k.key('c', Goome.CTRL);
            k.frame();
            assertEquals("zz", k.g.clipboard());
            k.key('k', Goome.CTRL);
            k.frame();
            assertEquals("hello you", k.g.clipboard());
            k.key('v', Goome.CTRL);
            k.frame();
            assertEquals("hello you", k.buf);
            k.key(Goome.K_LEFT, 0);
            k.key(Goome.K_LEFT, 0);
            k.key('q', Goome.CTRL);
            k.key('x', 0);
            k.frame();
            assertEquals("x", k.buf);
            cfg.bind(Config.ACT_WORD_LEFT, "alt+left");
            k.buf = "ab cd";
            k.key(Goome.K_END, 0);
            k.key(Goome.K_LEFT, Goome.ALT);
            k.key('_', 0);
            k.frame();
            assertEquals("ab _cd", k.buf);
            k.key(Goome.K_LEFT, Goome.CTRL);
            k.key('-', 0);
            k.frame();
            assertEquals("ab -_cd", k.buf);
            cfg.reset();
            assertEquals(530, cfg.blinkMs());
            assertEquals('v', cfg.key(Config.ACT_PASTE, 0));
        }
    }

    @Test
    void escAndCursorFollowConfig() {
        try (Kit k = new Kit(COMBO)) {
            k.frame();
            k.g.push(Event.down(40, 20, 1));
            k.g.push(Event.up(40, 20, 1));
            k.frame();
            k.frame();
            String opened = k.hash;
            k.u.config().escCloses(false);
            k.key(Goome.K_ESC, 0);
            k.frame();
            assertEquals(opened, k.hash);
            k.u.config().escCloses(true);
            k.key(Goome.K_ESC, 0);
            k.frame();
            assertNotEquals(opened, k.hash);
            k.u.config().cursor(Config.CUR_BUTTON, Goome.CURSOR_HAND);
            k.g.push(Event.motion(40, 20));
            k.frame();
            assertEquals(Goome.CURSOR_HAND, k.g.cursor());
        }
    }

    @Test
    void passwordMasksAndWontCopy() {
        String plain = typed(0, "abc", null, false, null, null, null);
        String pw = typed(Ui.INPUT_PASSWORD, "abc", null, false, null, null, null);
        assertEquals("f92831cd", plain);
        assertEquals("839d2d42", pw);
        assertEquals(pw, typed(Ui.INPUT_PASSWORD, "xyz", null, false, null, null, null));
        assertEquals(plain, typed(0, "abc", "type here", false, null, null, null));
        assertNotEquals(typed(0, "", null, false, null, null, null), typed(0, "", "type here", false, null, null, null));
        String[] o = new String[1];
        typed(Ui.INPUT_PASSWORD, "secret", null, true, "^c", null, o);
        assertEquals("secret|paste9", o[0]);
        typed(Ui.INPUT_PASSWORD, "secret", null, true, "^xq", null, o);
        assertEquals("q|paste9", o[0]);
        typed(0, "secret", null, true, "^c", null, o);
        assertEquals("secret|secret", o[0]);
        typed(Ui.INPUT_READONLY, "keep", null, true, "zz^v^x", null, o);
        assertEquals("keep|keep", o[0]);
        typed(Ui.INPUT_NUMBER, "", null, true, "a1-.x2+^v", null, o);
        assertEquals("1-.2+9|paste9", o[0]);
        typed(Ui.INPUT_NO_SELECT_ON_FOCUS, "ab", null, true, "z", null, o);
        assertEquals("abz|paste9", o[0]);
        int[] ret = {0};
        typed(Ui.INPUT_ENTER_RETURNS, "ab", null, true, "\n", ret, null);
        assertEquals(2, ret[0]);
        ret[0] = 0;
        typed(0, "ab", null, true, "\n", ret, null);
        assertEquals(0, ret[0]);
        try (Kit k = new Kit(INPUT)) {
            k.pw = true;
            k.buf = "abc";
            k.frame();
            k.frame();
            assertEquals(pw, k.hash);
            assertNull(k.u.config().mask());
            k.u.config().mask("#");
            assertEquals("#", k.u.config().mask());
            k.frame();
            assertNotEquals(pw, k.hash);
        }
    }
}
