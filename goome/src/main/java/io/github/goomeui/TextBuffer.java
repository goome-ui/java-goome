package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** Fixed size text storage for Ui.textEdit(). Close it when you are done. */
public final class TextBuffer implements AutoCloseable {
    private long p;
    private final int cap;

    /** An empty buffer holding up to capacity bytes. */
    public TextBuffer(int capacity) {
        this(capacity, "");
    }

    /** A buffer with starting text, cut if it doesnt fit. */
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

    /** How many bytes it holds. */
    public int capacity() {
        return cap;
    }

    /** The current text. */
    public String text() {
        return Native.chars(ptr(), cap);
    }

    /** Replaces the text, cut if it doesnt fit. */
    public void text(String s) {
        long q = ptr();
        byte[] b = Event.cut(s, cap);
        Native.put(q, b);
        N.putByte(q + b.length, (byte) 0);
    }

    /** Frees the buffer. */
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
