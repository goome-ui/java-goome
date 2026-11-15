package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FontTest {
    @Test
    void fonts() throws Exception {
        byte[] ttf = T.data("dejavu-sub.ttf"), otf = T.data("dejavu-sub.otf");
        assertThrows(GoomeException.class, () -> Font.fromBytesCopy("nope, not a font".getBytes()));
        assertThrows(GoomeException.class, () -> Font.fromBytes(java.util.Arrays.copyOf(ttf, 40)));
        Font f = Font.fromBytes(ttf), o = Font.fromBytesCopy(otf);
        TextSize m = f.measure("goome!", 20);
        assertTrue(m.w > 76.97 && m.w < 76.98 && m.h > 23.2 && m.h < 23.3, m.toString());
        for (Font font : new Font[] {f, o}) assertTrue(font.width("AV", 20) < font.width("A", 20) + font.width("V", 20) - 1);
        m = f.measure("a\nbb\n", 20);
        assertTrue(m.h > 3 * 23.2 && m.h < 3 * 23.3);
        assertTrue(f.lineHeight(20) > 0);
        String[] want = {"412c299f", "bcd42095"};
        IntBuffer[] bufs = new IntBuffer[2];
        Canvas[] keep = new Canvas[2];
        Font[] both = {f, o};
        for (int k = 0; k < 2; k++) {
            try (Canvas c = Canvas.create(200, 120, 240, 1.5)) {
                c.raw("kx1", 200);
                c.raw("ky1", 120);
                c.text(both[k], 13, 4, 2, "goome! Hié\nAVAWAY Ty.@ ☃", 0xFFFFFFFF);
                c.text(both[k], 30, 2.3, 24, "Hig", 0xC0FF8040);
                c.raw("kx0", 30);
                c.raw("ky0", 60);
                c.raw("kx1", 90);
                c.raw("ky1", 100);
                c.raw("nclip", 1);
                c.text(both[k], 40, 10, 50, "WoW", 0xFF2080FF);
                IntBuffer px = c.pixels();
                assertTrue(T.zeroCols(px, 240, 120, 200, 240));
                for (int i = 100 * 240; i < 120 * 240; i++) assertEquals(0, px.get(i));
                Rect d = c.drawn();
                assertTrue(d.x >= 0 && d.x + d.w <= 200 && d.y + d.h <= 100 && d.w > 0);
                assertEquals(want[k], T.hex(T.fnv(px, 240, 120, 240)));
            }
            keep[k] = Canvas.create(240, 120);
            keep[k].raw("kx1", 240);
            keep[k].raw("ky1", 120);
            keep[k].text(both[k], 31, 3, 3, "goome Hi\nmoge", 0xFFFFFFFF);
            bufs[k] = keep[k].pixels();
        }
        long ink = 0, diff = 0;
        for (int i = 0; i < bufs[0].limit(); i++) {
            ink += bufs[0].get(i) >>> 24;
            diff += Math.abs((bufs[0].get(i) >>> 24) - (bufs[1].get(i) >>> 24));
        }
        assertTrue(ink > 0 && diff * 50 < ink);
        keep[0].close();
        keep[1].close();
        try (Canvas c = Canvas.create(200, 120, 240, 1)) {
            c.raw("kx1", 200);
            c.raw("ky1", 120);
            for (int cut = 0; cut < otf.length; cut += 7) {
                byte[] bad = otf.clone();
                bad[cut] ^= 0x5A;
                Font broken;
                try {
                    broken = Font.fromBytes((cut & 1) != 0 ? bad : java.util.Arrays.copyOf(bad, Math.min(bad.length, cut + 12)));
                } catch (GoomeException e) {
                    broken = null;
                }
                c.text(broken, 20, 0, 0, "goome! AV", 0xFFFFFFFF);
                if (broken != null) broken.close();
            }
        }
        try (Canvas c = Canvas.create(200, 120, 240, 1)) {
            assertEquals(Font.measure(null, "abc", 12).w, c.text(null, 12, 0, 0, "ab\nabc", 0xFFFFFFFF));
        }
        f.close();
        f.close();
        o.close();
        assertThrows(GoomeException.Gone.class, () -> f.measure("x", 10));
    }

    @Test
    void builtin() {
        assertTrue(Font.builtinCount() >= 1);
        Font.setBuiltin(0);
        assertTrue(Font.lineHeight(null, 12) > 0);
    }

    @Test
    void loadFile() throws Exception {
        Path p = T.tmp("f.ttf");
        Files.write(p, T.data("dejavu-sub.ttf"));
        try (Font f = Font.load(p.toString())) {
            assertTrue(f.width("goome", 12) > 0);
        }
        try (Font f = Font.load(p.toString(), 0)) {
            assertTrue(f.width("goome", 12) > 0);
        }
        assertThrows(GoomeException.class, () -> Font.load(p.resolveSibling("gone.ttf").toString()));
    }
}
