package io.github.goomeui;

import static io.github.goomeui.Native.N;

public final class Options {
    String title, stateDir, testFrames, testScript, testShots;
    int backend = Goome.AUTO, captureFps, mode = Goome.MODE_OVERLAY, width, height, minWidth, minHeight, windowFlags;
    boolean noCapture, hideSelf;
    double scale;

    public Options title(String v) {
        title = v;
        return this;
    }

    public Options backend(int v) {
        backend = v;
        return this;
    }

    public Options backend(String name) {
        backend = Goome.backendId(name);
        return this;
    }

    public Options stateDir(String v) {
        stateDir = v;
        return this;
    }

    public Options noCapture(boolean v) {
        noCapture = v;
        return this;
    }

    public Options captureFps(int v) {
        captureFps = v;
        return this;
    }

    public Options hideSelf(boolean v) {
        hideSelf = v;
        return this;
    }

    public Options testFrames(String v) {
        testFrames = v;
        return this;
    }

    public Options testScript(String v) {
        testScript = v;
        return this;
    }

    public Options testShots(String v) {
        testShots = v;
        return this;
    }

    public Options mode(int v) {
        mode = v;
        return this;
    }

    public Options size(int w, int h) {
        width = w;
        height = h;
        return this;
    }

    public Options minSize(int w, int h) {
        minWidth = w;
        minHeight = h;
        return this;
    }

    public Options windowFlags(int v) {
        windowFlags = v;
        return this;
    }

    public Options scale(double v) {
        scale = v;
        return this;
    }

    long write(Scratch s) {
        Layout l = Layout.OPTS;
        long p = s.alloc(l.size);
        N.putLong(p + l.at("title"), s.str(title));
        N.putInt(p + l.at("backend"), backend);
        N.putLong(p + l.at("state_dir"), s.str(stateDir));
        N.putInt(p + l.at("no_capture"), noCapture ? 1 : 0);
        N.putInt(p + l.at("capture_fps"), captureFps);
        N.putInt(p + l.at("hide_self"), hideSelf ? 1 : 0);
        N.putLong(p + l.at("test_frames"), s.str(testFrames));
        N.putLong(p + l.at("test_script"), s.str(testScript));
        N.putLong(p + l.at("test_shots"), s.str(testShots));
        N.putInt(p + l.at("mode"), mode);
        N.putInt(p + l.at("width"), width);
        N.putInt(p + l.at("height"), height);
        N.putInt(p + l.at("min_width"), minWidth);
        N.putInt(p + l.at("min_height"), minHeight);
        N.putInt(p + l.at("window_flags"), windowFlags);
        N.putDouble(p + l.at("scale"), scale);
        return p;
    }
}
