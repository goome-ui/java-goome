package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BitmapTest {
    static final Object[][] IMAGES = {
        {"png-gray-1-bit.png", 9, 3, 0x01975AD8},
        {"png-palette-4-bit-trns-interlaced.png", 6, 6, 0xAF77E7E2},
        {"png-rgb-16-bit.png", 5, 4, 0xDD60C23E},
        {"png-gray-alpha-stored-interlaced.png", 5, 5, 0x52D68985},
        {"png-rgba-fixed-huffman-split.png", 8, 6, 0x66DB9DCD},
        {"png-gray-key.png", 7, 4, 0x843ED605},
        {"png-rgba-16-bit-interlaced.png", 9, 9, 0xDC2F91D5},
        {"jpeg-420.jpg", 16, 16, 0x32E2C9CE},
        {"jpeg-422-restarts.jpg", 13, 9, 0x0E230BAF},
        {"jpeg-444.jpg", 10, 10, 0x34FC5275},
        {"jpeg-progressive-420-restarts.jpg", 14, 11, 0x5BCEE932},
        {"jpeg-gray.jpg", 9, 7, 0xF010B3C8},
        {"jpeg-exif-rotated.jpg", 6, 10, 0x65909E25},
        {"bmp-4-bit.bmp", 5, 3, 0x9256D097},
        {"bmp-32-bit-alpha-top-down.bmp", 3, 2, 0x6BDB3A29},
        {"bmp-24-bit.bmp", 5, 3, 0x09D94C56},
        {"ppm.ppm", 3, 2, 0xC6FDF725},
        {"pgm-16-bit.pgm", 3, 1, 0x35029201},
        {"qoi.qoi", 6, 4, 0x23AF45AD},
    };

    @Test
    void cases() throws Exception {
        for (Object[] c : IMAGES) {
            String name = (String) c[0];
            byte[] raw = T.data(name);
            Bitmap b = Bitmap.fromBytes(raw);
            assertEquals(c[1], b.w(), name);
            assertEquals(c[2], b.h(), name);
            assertTrue(b.owned(), name);
            assertEquals(T.hex((Integer) c[3]), T.hex(T.fnv(b.pixels(), b.w(), b.h(), b.stride())), name);
            b.close();
            assertTrue(b.closed());
            b.close();
            Bitmap half;
            try {
                half = Bitmap.fromBytes(java.util.Arrays.copyOf(raw, raw.length / 2));
            } catch (GoomeException e) {
                half = null;
            }
            if (half != null) {
                assertTrue(half.w() > 0);
                half.close();
            }
        }
    }

    @Test
    void errors() throws Exception {
        GoomeException e = assertThrows(GoomeException.NotSupported.class, () -> Bitmap.fromBytes(T.data("jpeg-arithmetic.jpg")));
        assertTrue(e.getMessage().contains("arithmetic"), e.getMessage());
        assertThrows(GoomeException.NotSupported.class, () -> Bitmap.fromBytes("GIF89a......".getBytes()));
        assertThrows(GoomeException.class, () -> Bitmap.fromBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'}));
        assertThrows(GoomeException.class, () -> Bitmap.load("no/such/file.png"));
    }

    @Test
    void fromArgb() {
        Bitmap b = Bitmap.fromArgb(new int[] {0xFFFF0000, 0x80FFFFFF, 0x00123456, 0x40804020}, 2, 2);
        IntBuffer px = b.pixels();
        assertArrayEquals(new int[] {0xFFFF0000, 0x80808080, 0, 0x40201008}, new int[] {px.get(0), px.get(1), px.get(2), px.get(3)});
        b.close();
        assertThrows(GoomeException.Gone.class, b::pixels);
        assertThrows(IllegalArgumentException.class, () -> Bitmap.fromArgb(new int[] {1, 2}, 2, 2));
    }

    @Test
    void loadFile() throws Exception {
        Path p = T.tmp("x.qoi");
        Files.write(p, T.data("qoi.qoi"));
        try (Bitmap b = Bitmap.load(p.toString())) {
            assertEquals(6, b.w());
            assertEquals(4, b.h());
        }
    }
}
