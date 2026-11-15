package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class Scratch implements AutoCloseable {
    private static final int SIZE = 64 * 1024;
    private static final ThreadLocal<Scratch> MINE = ThreadLocal.withInitial(Scratch::new);

    private final long base = N.calloc(SIZE);
    private long top;
    private long[] marks = new long[16];
    private int[] bigMarks = new int[16];
    private int depth;
    private final List<Long> big = new ArrayList<>();

    static Scratch push() {
        Scratch s = MINE.get();
        if (s.depth == s.marks.length) {
            s.marks = java.util.Arrays.copyOf(s.marks, s.depth * 2);
            s.bigMarks = java.util.Arrays.copyOf(s.bigMarks, s.depth * 2);
        }
        s.marks[s.depth] = s.top;
        s.bigMarks[s.depth] = s.big.size();
        s.depth++;
        return s;
    }

    @Override
    public void close() {
        depth--;
        top = marks[depth];
        for (int k = big.size() - 1; k >= bigMarks[depth]; k--) N.free(big.remove(k));
    }

    long alloc(long n) {
        n = Math.max(8, (n + 15) & ~15L);
        if (top + n > SIZE) {
            long p = N.calloc(n);
            big.add(p);
            return p;
        }
        long p = base + top;
        top += n;
        N.zero(p, n);
        return p;
    }

    long str(String s) {
        if (s == null) return 0;
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        long p = alloc(b.length + 1);
        Native.put(p, b);
        return p;
    }

    long bytes(byte[] b) {
        long p = alloc(b.length + 1);
        Native.put(p, b);
        return p;
    }

    long i32(int v) {
        long p = alloc(4);
        N.putInt(p, v);
        return p;
    }

    long f64(double v) {
        long p = alloc(8);
        N.putDouble(p, v);
        return p;
    }

    long strs(String[] items) {
        long p = alloc(8L * Math.max(1, items.length));
        for (int k = 0; k < items.length; k++) N.putLong(p + 8L * k, str(items[k]));
        return p;
    }

    long rect(Rect r) {
        if (r == null) return 0;
        long p = alloc(Layout.RECT.size);
        Rect.write(p, r);
        return p;
    }

    long radii(Radii r) {
        long p = alloc(Layout.RADII.size);
        N.putDouble(p, r.tl());
        N.putDouble(p + 8, r.tr());
        N.putDouble(p + 16, r.br());
        N.putDouble(p + 24, r.bl());
        return p;
    }
}
