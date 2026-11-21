package io.github.goomeui;

import static io.github.goomeui.Native.N;

/** Settings for Goome.open(Options). Every setter returns the Options so you can chain them. */
public final class Options {
    String title, stateDir, testFrames, testScript, testShots;
    int backend = Goome.AUTO, captureFps, mode = Goome.MODE_OVERLAY, width, height, minWidth, minHeight, windowFlags;
    boolean noCapture, hideSelf;
    double scale;

    /** The window title, null means goome. */
    public Options title(String v) {
        title = v;
        return this;
    }

    /** Forces a backend, one of the Goome backend ids. The GOOME_BACKEND env var wins over this. */
    public Options backend(int v) {
        backend = v;
        return this;
    }

    /** Forces a backend by name, like x11 or wayland. */
    public Options backend(String name) {
        backend = Goome.backendId(name);
        return this;
    }

    /** Where the portal token and the control fifo live, null means neither. */
    public Options stateDir(String v) {
        stateDir = v;
        return this;
    }

    /** Overlay only, never asks for screen access. */
    public Options noCapture(boolean v) {
        noCapture = v;
        return this;
    }

    /** Frame rate of the gnome and portal streams, 0 means 5. */
    public Options captureFps(int v) {
        captureFps = v;
        return this;
    }

    /** Keeps our overlay out of grabs where the os lets us. */
    public Options hideSelf(boolean v) {
        hideSelf = v;
        return this;
    }

    /** Test backend only, same as the GOOME_TEST_FRAMES env var. */
    public Options testFrames(String v) {
        testFrames = v;
        return this;
    }

    /** Test backend only, same as the GOOME_TEST_SCRIPT env var. */
    public Options testScript(String v) {
        testScript = v;
        return this;
    }

    /** Test backend only, same as the GOOME_TEST_SHOTS env var. */
    public Options testShots(String v) {
        testShots = v;
        return this;
    }

    /** One of the Goome MODE_ numbers. */
    public Options mode(int v) {
        mode = v;
        return this;
    }

    /** Window or offscreen size, logical px. 0 means 960x640. */
    public Options size(int w, int h) {
        width = w;
        height = h;
        return this;
    }

    /** Smallest size a window can be resized to, window mode only. 0 means no limit. */
    public Options minSize(int w, int h) {
        minWidth = w;
        minHeight = h;
        return this;
    }

    /** Goome WINDOW_ flags or-ed together, window mode only. */
    public Options windowFlags(int v) {
        windowFlags = v;
        return this;
    }

    /** Real px per logical px, offscreen only. 0 means 1. */
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
