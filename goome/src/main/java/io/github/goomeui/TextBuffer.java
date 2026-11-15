package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class TextBuffer implements AutoCloseable {
    private long p;
    private final int cap;

    public TextBuffer(int capacity) {
        this(capacity, "");
    }

    public TextBuffer(int capacity, String text) {
        if (capacity < 1) throw new IllegalArgumentException("capacity has to be at least 1");
        cap = capacity;
        p = N.calloc(capacity);
        text(text);
    }

    long ptr() {
        if (p == 0) throw new GoomeException.Gone("this text buffer is closed");
        return p;
    }

    public int capacity() {
        return cap;
    }

    public String text() {
        return Native.chars(ptr(), cap);
    }

    public void text(String s) {
        long q = ptr();
        byte[] b = Event.cut(s, cap);
        Native.put(q, b);
        N.putByte(q + b.length, (byte) 0);
    }

    @Override
    public void close() {
        if (p != 0) N.free(p);
        p = 0;
    }

    @Override
    public String toString() {
        return p == 0 ? "TextBuffer(closed)" : text();
    }
}
