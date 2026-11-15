package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Pacer implements AutoCloseable {
    private long p = N.calloc(Layout.PACER.size);
    private int fps;

    public Pacer(int fps) {
        this.fps = fps;
    }

    public int fps() {
        return fps;
    }

    public void fps(int v) {
        fps = v;
    }

    public int next() {
        return next(fps);
    }

    public int next(int fps) {
        if (p == 0) throw new GoomeException.Gone("this pacer is closed");
        return Native.i(Fn.PACE, p, fps);
    }

    @Override
    public void close() {
        if (p != 0) N.free(p);
        p = 0;
    }
}
