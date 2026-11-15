package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;

class DrawTest {
    static final double[] STAR = {80, 5, 98, 85, 40, 35, 120, 35, 62, 85};

    static final int[][] SHAPES = {
        {0xA7B3B4C5, 0x0FB29A82, 0x1A98CF16, 0x1E7B729F, 0x21FD3B05, 0x241CE7FD, 0x6910B0D5, 0x7180B28E, 0x6F33AC30, 0xB48ADFA3, 0x796B9105, 0x2D93DC92, 0x1F908FAD, 0x23A6CF0C},
        {0x73429A18, 0xD9BD97AB, 0xD031F56C, 0xD2D9FA9A, 0xD639A1A7, 0xA77C3E9E, 0x7BD3FEAD, 0xAC76561E, 0x5BD0648D, 0xD0AC70E1, 0x0BB5DDA0, 0xE7C304C7, 0xCCEF8860, 0xEFC1F212},
        {0x9175788F, 0x6E99EA87, 0x935CC8E1, 0x0B73F68D, 0x23891BE5, 0xA28476C1, 0xEA64F655, 0x87DC9430, 0x909D11B5, 0x0550BB7A, 0x6455EE86, 0xD8C08E2B, 0xFD0DD1FD, 0x28C27977},
    };

    static void shape(Canvas c, int k, Bitmap b) {
        switch (k) {
            case 0: c.rrect(10.3, 8.6, 120, 70.2, new Radii(4, 20, 0, 33), 0xE02060C0); break;
            case 1: c.rrectLine(10.3, 8.6, 120, 70.2, Radii.of(18), 2.5, 0xFF20A060); break;
            case 2: c.rrectGrad(10, 10, 130, 70, 12, 0xFF102040, 0x8080C0FF); break;
            case 3: c.rrectGrad(10, 10, 130, 70, Radii.of(35), 0xFFFF4080, 0x004080FF, true); break;
            case 4: c.shadow(30, 20, 90, 50, 10, 16, 0x80000000); break;
            case 5:
                c.circle(50.5, 50.2, 30.7, 0xFFFFC020);
                c.circle(120, 30, 1.3, 0xFF000000);
                break;
            case 6: c.ring(80, 50, 30, 5, 0xC0208080); break;
            case 7:
                c.arc(50, 50, 30, 6, 0.3, 4.5, 0xFF3060E0);
                c.arc(120, 50, 20, 30, 2, 2.5, 0xFF905020);
                break;
            case 8: c.poly(STAR, 0xFF2040A0); break;
            case 9: c.bitmap(b, 10, 10, 100.5, 75.3, 20, 0xFFFFFFFF); break;
            case 10: c.bitmap(b, 10, 10, 24, 16, 0, 0x80FF8080); break;
            case 11: c.bitmap(b, 10, 10, 9, 5, 2, 0xFFFFFFFF); break;
            case 12: c.bitmapPart(b, 4, 2, 8, 8, 100, 10, 40, 40, 0xFFFFFFFF); break;
            default:
                try (Canvas.Clip a = c.clip(20, 15, 80, 50)) {
                    c.rrect(0, 0, 160, 100, 30, 0xFF4080FF);
                    try (Canvas.Clip z = c.clip(50, 0, 100, 40)) {
                        c.circle(70, 40, 30, 0xFFFF4040);
                        c.text(30, 30, "clip", 0xFF000000, true);
                        c.shadow(40, 20, 60, 30, 8, 10, 0xFF000000);
                        c.bitmap(b, 40, 10, 48, 32, 6, 0xFFFFFFFF);
                    }
                    c.line(0, 0, 160, 100, 3, 0xFF00A000);
                    c.poly(STAR, 0x80000000);
                }
                c.frame(20, 15, 80, 50, 1, 0x80000000);
        }
    }

    static Bitmap makeBitmap() {
        int[] bp = new int[27 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 24; x++) {
                int a = x * 255 / 23;
                int v = ((x / 4 + y / 4) & 1) != 0 ? 0xFFFFFF : 0x2050C0;
                bp[y * 27 + x] = a << 24 | ((v >> 16 & 255) * a / 255) << 16 | ((v >> 8 & 255) * a / 255) << 8 | (v & 255) * a / 255;
            }
        }
        return Bitmap.wrap(bp, 24, 16, 27);
    }

    @Test
    void basic() {
        try (Canvas c = Canvas.create(160, 100, 200, 1.5)) {
            c.roundRect(5, 5, 60, 40, 8, 0xC0203040);
            c.line(0, 0, 100, 60, 3, 0xFFFF0000);
            c.triangle(10, 50, 60, 50, 35, 10, 0x8000FF00);
            c.text(8, 8, "goome!", 0xFFFFFFFF, true);
            c.frame(70, 10, 30, 30, 2, 0xFF00FFFF);
            IntBuffer px = c.pixels();
            for (int y = 0; y < 100; y++) assertEquals(0, px.get(y * 200 + 170));
            Rect d = c.drawn();
            assertTrue(d.w > 0 && d.h > 0);
            assertEquals("b93b60cf", T.hex(T.fnv(px, 200, 100, 200)));
            c.clear();
            assertEquals(T.hex(T.fnv(IntBuffer.wrap(new int[200 * 100]), 200, 100, 200)), T.hex(T.fnv(c.pixels(), 200, 100, 200)));
            assertTrue(c.textWidth("abc") > 0);
        }
    }

    @Test
    void shapes() {
        try (Bitmap b = makeBitmap()) {
            double[] scales = {1, 1.25, 2};
            for (int s = 0; s < 3; s++) {
                double sc = scales[s];
                String[] got = new String[14], want = new String[14];
                for (int k = 0; k < 14; k++) {
                    int w = (int) (160 * sc), h = (int) (100 * sc);
                    try (Canvas c = Canvas.create(w, h, w + 3, sc)) {
                        shape(c, k, b);
                        IntBuffer px = c.pixels();
                        got[k] = T.hex(T.fnv(px, w + 3, h, w + 3));
                        want[k] = T.hex(SHAPES[s][k]);
                        assertTrue(c.drawn().w > 0);
                        assertTrue(T.zeroCols(px, w + 3, h, w, w + 3));
                        for (int i = 0; i < px.limit(); i++) {
                            int p = px.get(i), a = p >>> 24;
                            assertFalse((p >> 16 & 255) > a || (p >> 8 & 255) > a || (p & 255) > a, "not premultiplied");
                        }
                    }
                    if (k == 13) clipped(b, sc, w, h);
                }
                assertArrayEquals(want, got, "scale " + sc);
            }
        }
    }

    private static void clipped(Bitmap b, double sc, int w, int h) {
        int x0 = (int) (20 * sc + 0.5), y0 = (int) (15 * sc + 0.5), x1 = (int) (100 * sc + 0.5), y1 = (int) (65 * sc + 0.5);
        try (Canvas c = Canvas.create(w, h, w + 3, sc)) {
            c.clipPush(20, 15, 80, 50);
            shape(c, 8, b);
            shape(c, 4, b);
            shape(c, 9, b);
            c.text(0, 20, "outside", 0xFFFFFFFF, true);
            Rect d = c.drawn();
            assertTrue(d.x >= x0 && d.y >= y0 && d.x + d.w <= x1 && d.y + d.h <= y1 && d.w > 0);
            IntBuffer px = c.pixels();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if ((x < x0 || x >= x1 || y < y0 || y >= y1) && px.get(y * (w + 3) + x) != 0) fail("drew outside the clip");
                }
            }
            c.clipPop();
            assertEquals(0, c.clipDepth());
        }
    }

    @Test
    void imageAndBlit() {
        try (Canvas src = Canvas.create(20, 20); Canvas dst = Canvas.create(40, 40)) {
            int[] green = new int[16];
            java.util.Arrays.fill(green, 0xFF00FF00);
            src.image(0, 0, 20, 20, green, 4, 4);
            assertEquals(0xFF00FF00, src.pixels().get(0));
            dst.blit(src, 1.0, 10, 10);
            assertEquals(0xFF00FF00, dst.pixel(10, 10));
            assertEquals(0, dst.pixel(0, 0));
            dst.erase(0, 0, 40, 40);
            assertEquals(0, dst.pixel(10, 10));
            dst.image(0, 0, 2, 1, new int[] {0xFFFF0000, 0xFF0000FF}, 2, 1);
            assertEquals(0xFFFF0000, dst.pixel(0, 0));
            assertEquals(0xFF0000FF, dst.pixel(1, 0));
        }
    }

    @Test
    void closedCanvas() {
        Canvas c = Canvas.create(10, 10);
        c.close();
        c.close();
        assertFalse(c.valid());
        assertThrows(GoomeException.Gone.class, () -> c.rect(0, 0, 1, 1, -1));
    }
}
