package io.github.goomeui;

import static io.github.goomeui.Native.N;
import static io.github.goomeui.Native.b;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main handle. Opens an overlay, an app window or an offscreen surface, and everything else (drawing,
 * events, the ui) hangs off it. Close it when you are done.
 */
public final class Goome implements AutoCloseable {
    /** The goome version these bindings were built for. */
    public static final String VERSION = "0.5.6";

    /** Backend ids for Options.backend. AUTO picks the best one for the system, the others force one. */
    public static final int AUTO = 0, WAYLAND = 1, GNOME = 2, PORTAL = 3, X11 = 4, WIN32 = 5, MACOS = 6, TEST = 7, OFFSCREEN = 8;
    /**
     * Modes for Options.mode: see through overlay, normal app window, or an offscreen surface you read the
     * pixels from.
     */
    public static final int MODE_OVERLAY = 0, MODE_WINDOW = 1, MODE_OFFSCREEN = 2;
    /** Window flags for Options.windowFlags, or them together. Only used in window mode. */
    public static final int WINDOW_RESIZABLE = 1, WINDOW_BORDERLESS = 2, WINDOW_TRANSPARENT = 4, WINDOW_ON_TOP = 8, WINDOW_HIDDEN = 16, WINDOW_CENTER = 32, WINDOW_NO_TITLEBAR = 64, WINDOW_NO_CONSOLE = 128;

    /** Error codes. OK is 0, everything else is negative and shows up as a GoomeException subclass. */
    public static final int OK = 0, ENOTSUP = -1, EDENIED = -2, ENODISPLAY = -3, EGONE = -4, EAGAIN = -5, ENOMEM = -6, EARG = -7;

    /** Capability bits for caps() and can(). They tell you what the current backend can do. */
    public static final int CAP_CAPTURE = 1, CAP_GLOBAL_POINTER = 2, CAP_HOTKEYS = 4, CAP_HIDE_SELF = 8, CAP_KEYBOARD = 16, CAP_WINDOW = 32,
        CAP_CLIPBOARD = 64, CAP_CURSOR = 128, CAP_GRAB = 256;

    /** What the ui wants from the os, see Ui.wants() and Ui.autoGrab(). */
    public static final int WANT_MOUSE = 1, WANT_KEYBOARD = 2;

    /**
     * Event types, what Event.type() returns. CLOSE is the window close button, the app decides if it
     * quits. FOCUS has id 1 when gained and 0 when lost. DROP is a file dropped on the window. TEXT is
     * finished ime, dead key or compose text. PREEDIT is text still being composed.
     */
    public static final int NONE = 0, DOWN = 1, UP = 2, MOTION = 3, SCROLL = 4, KEY = 5, HOTKEY = 6, CONTROL = 7, MONITORS = 8, QUIT = 9, CLOSE = 10,
        FOCUS = 11, DROP = 12, TEXT = 13, PREEDIT = 14;

    /** Cursor shapes for cursor(int). */
    public static final int CURSOR_ARROW = 0, CURSOR_IBEAM = 1, CURSOR_HAND = 2, CURSOR_MOVE = 3, CURSOR_RESIZE_EW = 4, CURSOR_RESIZE_NS = 5,
        CURSOR_RESIZE_NWSE = 6, CURSOR_RESIZE_NESW = 7, CURSOR_WAIT = 8, CURSOR_NO = 9, CURSOR_HIDDEN = 10;
    /** The cursor names cursor(String) accepts, in the same order as the CURSOR_ numbers. */
    public static final List<String> CURSOR_NAMES = List.of("arrow", "ibeam", "hand", "move", "resize_ew", "resize_ns", "resize_nwse", "resize_nesw",
        "wait", "no", "hidden");

    /** Window edges for drag(int), or them together to resize from a corner. */
    public static final int EDGE_LEFT = 1, EDGE_RIGHT = 2, EDGE_TOP = 4, EDGE_BOTTOM = 8;
    /** Window states for state(int). */
    public static final int STATE_NORMAL = 0, STATE_MINIMIZED = 1, STATE_MAXIMIZED = 2, STATE_FULLSCREEN = 3;

    /** Key codes for Event.key(). Letters and digits are just the lowercase ascii value. */
    public static final int K_BACKSPACE = 8, K_TAB = 9, K_ENTER = 13, K_ESC = 27, K_SPACE = 32, K_DELETE = 127, K_F1 = 0x81, K_F12 = 0x8C;
    /** Arrow, home, end, page and insert key codes for Event.key(). */
    public static final int K_LEFT = 0x90, K_RIGHT = 0x91, K_UP = 0x92, K_DOWN = 0x93, K_HOME = 0x94, K_END = 0x95, K_PGUP = 0x96, K_PGDN = 0x97,
        K_INSERT = 0x98;
    /** Modifier bits in Event.mods(), also or-ed into hotkey combos. */
    public static final int SHIFT = 0x100, CTRL = 0x200, ALT = 0x400, SUPER = 0x800;

    private static final String[] BACKENDS = {"auto", "wayland", "gnome", "portal", "x11", "win32", "macos", "test", "offscreen"};

    private long g;
    private final Map<Integer, Integer> grabs = new HashMap<>();
    int began;
    int ended;
    private int dirtySeen = -1;
    private final List<Ui> uis = new ArrayList<>();
    private Canvas current;

    private Goome(long g) {
        this.g = g;
    }

    /** Opens a see through overlay with the default options. */
    public static Goome open() {
        return open(new Options());
    }

    /** Opens an overlay with this title. */
    public static Goome open(String title) {
        return open(new Options().title(title));
    }

    /** Opens goome the way the options say, overlay, window or offscreen. Throws a GoomeException if it cant. */
    public static Goome open(Options o) {
        try (Scratch s = Scratch.push()) {
            long err = s.i32(0);
            long h = Native.call(Fn.OPEN, o.write(s), err);
            if (h == 0) {
                int e = N.getInt(err);
                if (e == 0) e = ENODISPLAY;
                String what = o.mode == MODE_OFFSCREEN ? "an offscreen goome" : o.mode == MODE_WINDOW ? "an app window" : "an overlay";
                throw GoomeException.of(e, "cant open " + what + ": " + strerror(e));
            }
            return new Goome(h);
        }
    }

    /** Opens a normal resizable window, centered. Size is logical px. */
    public static Goome openApp(String title, int w, int h) {
        try (Scratch s = Scratch.push()) {
            long err = s.i32(0);
            long p = Native.call(Fn.OPEN_APP, s.str(title), w, h, err);
            if (p == 0) {
                int e = N.getInt(err);
                if (e == 0) e = ENODISPLAY;
                throw GoomeException.of(e, "cant open an app window: " + strerror(e));
            }
            return new Goome(p);
        }
    }

    /**
     * Opens an offscreen surface. Nothing shows up on screen, read the pixels with lastFrame(). Scale is
     * real px per logical px.
     */
    public static Goome openOffscreen(int w, int h, double scale) {
        return open(new Options().mode(MODE_OFFSCREEN).size(w, h).scale(scale));
    }

    /** Same as the other openOffscreen but with a title. */
    public static Goome openOffscreen(int w, int h, double scale, String title) {
        return open(new Options().mode(MODE_OFFSCREEN).size(w, h).scale(scale).title(title));
    }

    long h() {
        if (g == 0) throw new GoomeException.Gone("goome is already closed");
        return g;
    }

    private int check(int rc) {
        return GoomeException.check(rc, rc < 0 ? (error() != null ? error() : strerror(rc)) : null);
    }

    /** True once close() ran. */
    public boolean closed() {
        return g == 0;
    }

    /** Closes goome and every Ui made from it. Safe to call twice. */
    @Override
    public void close() {
        if (g == 0) return;
        for (Ui u : new ArrayList<>(uis)) u.close();
        uis.clear();
        if (current != null) current.drop();
        current = null;
        long h = g;
        g = 0;
        Native.call(Fn.CLOSE, h);
    }

    void forget(Ui u) {
        uis.remove(u);
    }

    void remember(Ui u) {
        uis.add(u);
    }

    /** The last error text from the native side, or null. */
    public String error() {
        return g == 0 ? null : Native.str(Native.call(Fn.ERROR, g));
    }

    /** The readable text for an error code. */
    public static String strerror(int code) {
        String s = Native.str(Native.call(Fn.STRERROR, code));
        return s != null ? s : "error " + code;
    }

    /** Name of the backend in use, like x11 or wayland. */
    public String backendName() {
        return Native.str(Native.call(Fn.BACKEND_NAME, h()));
    }

    /** Which way the native library is loaded, ffm or lwjgl. */
    public static String nativeKind() {
        return N.kind();
    }

    /** The CAP_ bits this backend supports. */
    public int caps() {
        return Native.i(Fn.CAPS, h());
    }

    /** True if the backend has that CAP_ bit. */
    public boolean can(int cap) {
        return (caps() & cap) != 0;
    }

    /** Every monitor, up to 32. In window and offscreen mode that is the one surface. */
    public List<Monitor> monitors() {
        try (Scratch s = Scratch.push()) {
            int size = Layout.MONITOR.size;
            long arr = s.alloc((long) size * 32);
            int n = check(Native.i(Fn.MONITORS, h(), arr, 32));
            List<Monitor> out = new ArrayList<>();
            for (int k = 0; k < Math.min(n, 32); k++) out.add(new Monitor(arr + (long) k * size));
            return out;
        }
    }

    /**
     * Grabs a monitor as an Image. The pixels are only good until the next grab of that monitor, copy()
     * them to keep them. Throws Again if the frame isnt ready yet.
     */
    public Image capture(int mon) {
        return capture(mon, null);
    }

    /** Grabs only the logical rect r of the monitor, null for the whole thing. */
    public Image capture(int mon, Rect r) {
        try (Scratch s = Scratch.push()) {
            long img = s.alloc(Layout.IMAGE.size);
            check(Native.i(Fn.CAPTURE, h(), mon, s.rect(r), img));
            int gen = grabs.getOrDefault(mon, 0) + 1;
            grabs.put(mon, gen);
            return new Image(img, () -> g != 0 && grabs.getOrDefault(mon, 0) == gen,
                "this grab is gone, a newer one replaced it. copy() it to keep the pixels");
        }
    }

    /** Like capture but retries when it says Again, polling waitMs between tries. */
    public Image captureWait(int mon, Rect r, int tries, int waitMs) {
        for (int k = 0;; k++) {
            try {
                return capture(mon, r);
            } catch (GoomeException.Again e) {
                if (k >= tries - 1) throw e;
                poll(waitMs);
            }
        }
    }

    /** How see through goome is at a spot, for masking our own overlay out of a grab. */
    public int alphaAt(double x, double y) {
        return Native.i(Fn.ALPHA_AT, h(), b(x), b(y));
    }

    /**
     * Starts a frame on a monitor. Returns null when there is nothing to draw on, skip the frame then.
     * Finish it with end().
     */
    public Canvas begin(int mon) {
        long p = Native.call(Fn.BEGIN, h(), mon);
        began++;
        if (current != null) current.drop();
        current = p == 0 ? null : new Canvas(p, false);
        return current;
    }

    /**
     * Sends the frame. The rects, plus anything added with Canvas.hit(), are where the overlay catches the
     * mouse, the rest clicks through.
     */
    public int end(Canvas c, Rect... hits) {
        List<Rect> all = new ArrayList<>(c.hits);
        if (hits != null) for (Rect r : hits) if (r != null) all.add(r);
        c.hits.clear();
        try (Scratch s = Scratch.push()) {
            long arr = 0;
            if (!all.isEmpty()) {
                arr = s.alloc((long) Layout.RECT.size * all.size());
                for (int k = 0; k < all.size(); k++) Rect.write(arr + (long) Layout.RECT.size * k, all.get(k));
            }
            long p = c.take();
            if (c == current) current = null;
            ended++;
            return check(Native.i(Fn.END, h(), p, arr, all.size()));
        }
    }

    /** begin, draw, end in one go. Skips the frame if begin gave nothing. */
    public void frame(int mon, java.util.function.Consumer<Canvas> draw, Rect... hits) {
        Canvas c = begin(mon);
        if (c == null) return;
        try {
            draw.accept(c);
        } finally {
            if (c.valid()) end(c, hits);
        }
    }

    /** Waits up to timeoutMs for an event. Returns null if none came. */
    public Event poll(int timeoutMs) {
        try (Scratch s = Scratch.push()) {
            long ev = s.alloc(Layout.EVENT.size);
            int rc = check(Native.i(Fn.POLL, h(), ev, timeoutMs));
            if (rc <= 0) return null;
            Event e = Event.read(ev);
            if (e.type == DROP) e.path = dropPath();
            if ((e.type == TEXT || e.type == PREEDIT) && Fn.EVENT_TEXT.present()) e.full = eventText();
            return e;
        }
    }

    /** Waits for the first event like poll, then takes everything else that is already queued. */
    public List<Event> events(int timeoutMs) {
        List<Event> out = new ArrayList<>();
        for (Event e = poll(timeoutMs); e != null; e = poll(0)) out.add(e);
        return out;
    }

    /** Where the mouse is right now, null if the backend cant say. */
    public Pointer pointer() {
        try (Scratch s = Scratch.push()) {
            long x = s.f64(0), y = s.f64(0), bt = s.i32(0);
            if (Native.i(Fn.POINTER, h(), x, y, bt) == 0) return null;
            return new Pointer(N.getDouble(x), N.getDouble(y), N.getInt(bt));
        }
    }

    /** Turns keyboard focus for the overlay on or off. */
    public void keyboard(boolean on) {
        check(Native.i(Fn.KEYBOARD, h(), b(on)));
    }

    /** Overlay: takes focus and the pointer until grab(false). Window: drags keep going outside it. */
    public void grab(boolean on) {
        check(Native.i(Fn.GRAB, h(), b(on)));
    }

    /** True while a grab is on. */
    public boolean grabbed() {
        return Native.i(Fn.GRABBED, h()) != 0;
    }

    /** Registers a global hotkey like ctrl+alt+g. It comes back as a HOTKEY event carrying this id. */
    public void hotkey(String combo, int id) {
        try (Scratch s = Scratch.push()) {
            check(Native.i(Fn.HOTKEY, h(), s.str(combo), id));
        }
    }

    /** Reads a combo string. Returns {key, mods}, throws if it makes no sense. */
    public static int[] parseCombo(String combo) {
        try (Scratch s = Scratch.push()) {
            long k = s.i32(0), m = s.i32(0);
            int rc = Native.i(Fn.PARSE_COMBO, s.str(combo), k, m);
            if (rc != OK) throw GoomeException.of(rc, "cant read the combo '" + combo + "'");
            return new int[] {N.getInt(k), N.getInt(m)};
        }
    }

    /** Path of the control fifo in the state dir, or null if there is none. */
    public String controlPath() {
        return Native.str(Native.call(Fn.CONTROL_PATH, h()));
    }

    /** Sends a line to a running goome through its state dir, it shows up there as a CONTROL event. */
    public static void send(String stateDir, String msg) {
        try (Scratch s = Scratch.push()) {
            int rc = Native.i(Fn.SEND, s.str(stateDir), s.str(msg));
            if (rc != OK) throw GoomeException.of(rc, "couldnt send '" + msg + "' to " + stateDir + ": " + strerror(rc));
        }
    }

    /** A millisecond clock for pacing frames. */
    public static long nowMs() {
        return Native.call(Fn.NOW_MS);
    }

    /** Makes an immediate mode Ui on this goome. Close it or let goome close it. */
    public Ui ui() {
        return new Ui(this);
    }

    /** One of the MODE_ numbers. */
    public int mode() {
        return Native.i(Fn.MODE, h());
    }

    /** Sets the window title. */
    public void title(String title) {
        try (Scratch s = Scratch.push()) {
            check(Native.i(Fn.WINDOW_TITLE, h(), s.str(title)));
        }
    }

    /** The window title, null in overlay mode. */
    public String title() {
        return mode() == MODE_OVERLAY ? null : monitors().get(0).name;
    }

    /** The surface size as {w, h}, logical px. */
    public int[] size() {
        Rect a = monitors().get(0).area;
        return new int[] {a.w, a.h};
    }

    /** Sets the window client size, logical px. */
    public void size(int w, int h) {
        check(Native.i(Fn.WINDOW_SIZE, h(), w, h));
        began++;
    }

    /** Resizes a window or an offscreen surface. */
    public void resize(int w, int h) {
        resize(w, h, 0);
    }

    /** Resizes an offscreen surface and sets its scale, in a window it just sets the size. */
    public void resize(int w, int h, double scale) {
        if (mode() == MODE_OFFSCREEN) check(Native.i(Fn.RESIZE, h(), w, h, b(scale)));
        else size(w, h);
        began++;
        dirtySeen = -1;
    }

    /** The window position as {x, y}. Throws NotSupported where the os wont say, like wayland. */
    public int[] pos() {
        try (Scratch s = Scratch.push()) {
            long x = s.i32(0), y = s.i32(0);
            check(Native.i(Fn.WINDOW_POS, h(), x, y, 0));
            return new int[] {N.getInt(x), N.getInt(y)};
        }
    }

    /** Moves the window. */
    public void pos(int x, int y) {
        try (Scratch s = Scratch.push()) {
            check(Native.i(Fn.WINDOW_POS, h(), s.i32(x), s.i32(y), 1));
        }
    }

    /** The window state, one of the STATE_ numbers. */
    public int state() {
        return check(Native.i(Fn.WINDOW_GET_STATE, h()));
    }

    /** Sets the window state. */
    public void state(int st) {
        check(Native.i(Fn.WINDOW_STATE, h(), st));
    }

    /** Minimizes the window. */
    public void minimize() {
        state(STATE_MINIMIZED);
    }

    /** Maximizes the window, or puts it back if it already is. */
    public void maximize() {
        state(state() == STATE_MAXIMIZED ? STATE_NORMAL : STATE_MAXIMIZED);
    }

    /** Goes fullscreen or back to normal. */
    public void fullscreen(boolean on) {
        state(on ? STATE_FULLSCREEN : STATE_NORMAL);
    }

    /** Starts a native window move (0) or resize (EDGE_ bits) from the press that is happening now. */
    public void drag(int edges) {
        check(Native.i(Fn.WINDOW_DRAG, h(), edges));
    }

    /** Shows or hides the window. */
    public void show(boolean on) {
        check(Native.i(Fn.WINDOW_SHOW, h(), b(on)));
    }

    /** The last cursor shape asked for, offscreen embedders read this. */
    public int cursor() {
        return Native.i(Fn.CURSOR_SHAPE, h());
    }

    /** Sets the cursor, one of the CURSOR_ numbers. */
    public void cursor(int shape) {
        check(Native.i(Fn.CURSOR, h(), shape));
    }

    /** Sets the cursor by name from CURSOR_NAMES. */
    public void cursor(String name) {
        int k = CURSOR_NAMES.indexOf(name.toLowerCase(Locale.ROOT));
        if (k < 0) throw new IllegalArgumentException("no cursor called '" + name + "', pick one of " + String.join(", ", CURSOR_NAMES));
        cursor(k);
    }

    /** Reads the clipboard text. Can block for around 250ms on x11 and wayland. */
    public String clipboard() {
        int n = check(Native.i(Fn.CLIPBOARD_GET, h(), 0, 0));
        while (true) {
            try (Scratch s = Scratch.push()) {
                long buf = s.alloc(n + 1);
                int m = check(Native.i(Fn.CLIPBOARD_GET, h(), buf, n + 1));
                if (m <= n) return Native.str(buf, m);
                n = m;
            }
        }
    }

    /** Puts text on the clipboard. */
    public void clipboard(String text) {
        try (Scratch s = Scratch.push()) {
            check(Native.i(Fn.CLIPBOARD_SET, h(), s.str(text == null ? "" : text)));
        }
    }

    /** Full path of the last DROP event, good until the next poll. */
    public String dropPath() {
        return Native.str(Native.call(Fn.DROP_PATH, h()));
    }

    /** Full utf-8 text of the last TEXT or PREEDIT event, good until the next poll. */
    public String eventText() {
        String s = Native.str(Native.call(Fn.EVENT_TEXT, h()));
        return s == null ? "" : s;
    }

    /** Byte offset of the caret in the last preedit text. */
    public int preeditCursor() {
        return Native.i(Fn.PREEDIT_CURSOR, h());
    }

    /** Turns ime on or off. The caret rect (logical) is where the candidate window goes. */
    public void textInput(boolean on, Rect caret) {
        try (Scratch s = Scratch.push()) {
            check(Native.i(Fn.TEXT_INPUT, h(), b(on), s.rect(caret)));
        }
    }

    /** Turns ime on or off without saying where the caret is. */
    public void textInput(boolean on) {
        textInput(on, null);
    }

    /** Queues an event as if the os sent it. Works in every mode. */
    public void push(Event e) {
        try (Scratch s = Scratch.push()) {
            long p = s.alloc(Layout.EVENT.size);
            e.write(p);
            check(Native.i(Fn.PUSH, h(), p));
        }
    }

    /** Queues a move, press and release at a spot. */
    public void pushClick(double x, double y, int button) {
        push(Event.motion(x, y));
        push(Event.down(x, y, button));
        push(Event.up(x, y, button));
    }

    /** Queues key events that type this text. */
    public void pushText(String text) {
        text.codePoints().forEach(cp -> push(Event.key(cp < 128 ? cp : 0, 0, new String(Character.toChars(cp)))));
    }

    /** The last frame end() sent, premultiplied argb. Good until the next begin(), copy() it to keep it. */
    public Image lastFrame() {
        try (Scratch s = Scratch.push()) {
            long img = s.alloc(Layout.IMAGE.size);
            check(Native.i(Fn.FRAME, h(), img));
            int gen = began;
            return new Image(img, () -> g != 0 && began == gen, "this frame is gone, begin() started a new one. copy() it to keep the pixels");
        }
    }

    /** True if the spot is inside the hit rects of the last end(). */
    public boolean hit(double x, double y) {
        return Native.i(Fn.HIT, h(), b(x), b(y)) != 0;
    }

    /**
     * The real px rect that changed since the last call, null if nothing did. The first one after open or
     * resize is the whole frame.
     */
    public Rect frameDirty() {
        if (Fn.FRAME_DIRTY.present()) {
            try (Scratch s = Scratch.push()) {
                long r = s.alloc(Layout.RECT.size);
                return Native.i(Fn.FRAME_DIRTY, h(), r) != 0 ? Rect.read(r) : null;
            }
        }
        if (dirtySeen == ended) return null;
        Image f;
        try {
            f = lastFrame();
        } catch (GoomeException.Again e) {
            return null;
        }
        dirtySeen = ended;
        return new Rect(0, 0, f.w, f.h);
    }

    /**
     * Copies rect r (real px, null for the whole frame) of the last frame into a direct buffer. Stride is
     * in bytes.
     */
    public void frameCopy(ByteBuffer dst, int dstStride, PixelFormat fmt, Rect r) {
        if (!dst.isDirect()) throw new IllegalArgumentException("frameCopy needs a direct ByteBuffer");
        if (Fn.FRAME_COPY.present()) {
            if (r != null && r.h > 0 && dst.remaining() < (long) (r.h - 1) * dstStride + r.w * 4L) {
                throw new IllegalArgumentException("dst has " + dst.remaining() + " bytes left, needs " + ((long) (r.h - 1) * dstStride + r.w * 4L));
            }
            if (r == null) {
                Image f = lastFrame();
                if (dst.remaining() < (long) (f.h - 1) * dstStride + f.w * 4L) throw new IllegalArgumentException("dst is too small for a " + f.w + "x" + f.h + " frame");
            }
            try (Scratch s = Scratch.push()) {
                check(Native.i(Fn.FRAME_COPY, h(), N.address(dst), dstStride, fmt.ordinal(), s.rect(r)));
            }
            return;
        }
        lastFrame().copyTo(dst, dstStride, fmt, r);
    }

    /** Writes pixels to a ppm file. Stride is in pixels. */
    public static void savePpm(String path, int[] px, int w, int h, int stride) {
        long n = h > 0 ? (long) (h - 1) * stride + w : 0;
        if (w < 0 || h < 0 || stride < w || px.length < n) throw new IllegalArgumentException("need " + n + " pixels for " + w + "x" + h + " stride " + stride + ", got " + px.length);
        try (Scratch s = Scratch.push()) {
            long mem = s.alloc(4L * Math.max(1, n));
            Native.view(mem, 4L * n).asIntBuffer().put(px, 0, (int) n);
            savePpm(path, mem, w, h, stride);
        }
    }

    static void savePpm(String path, long px, int w, int h, int stride) {
        try (Scratch s = Scratch.push()) {
            int rc = Native.i(Fn.SAVE_PPM, s.str(path), px, w, h, stride);
            if (rc != OK) throw GoomeException.of(rc, "couldnt write " + path);
        }
    }

    static int backendId(String name) {
        for (int k = 0; k < BACKENDS.length; k++) if (BACKENDS[k].equalsIgnoreCase(name)) return k;
        throw new IllegalArgumentException("no backend called '" + name + "', pick one of " + String.join(", ", BACKENDS));
    }

    @Override
    public String toString() {
        return g == 0 ? "Goome(closed)" : "Goome(" + backendName() + ")";
    }
}
