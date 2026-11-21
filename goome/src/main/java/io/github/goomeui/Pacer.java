package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** Keeps a loop at a steady frame rate. Use next() as the poll timeout. */
public final class Pacer implements AutoCloseable {
    private long p = N.calloc(Layout.PACER.size);
    private int fps;

    /** A pacer for this many frames per second. */
    public Pacer(int fps) {
        this.fps = fps;
    }

    /** The frame rate. */
    public int fps() {
        return fps;
    }

    /** Changes the frame rate. */
    public void fps(int v) {
        fps = v;
    }

    /** Milliseconds to wait until the next frame is due, at the current fps. */
    public int next() {
        return next(fps);
    }

    /** Same, with a frame rate for this call. */
    public int next(int fps) {
        if (p == 0) throw new GoomeException.Gone("this pacer is closed");
        return Native.i(Fn.PACE, p, fps);
    }

    /** Frees the pacer. */
    @Override
    public void close() {
        if (p != 0) N.free(p);
        p = 0;
    }
}
