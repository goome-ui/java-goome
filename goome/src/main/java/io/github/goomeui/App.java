package io.github.goomeui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The quickest way to a window with a ui in it. Set the callbacks, call run(), and it handles opening, the
 * event loop, pacing and cleanup.
 */
public final class App {
    private final String title;
    private final int width, height;
    private int flags, fps = 60, idleMs = 500;
    private boolean light, animate, running, every;
    private int dirty, frames;
    private Options opts;
    private Goome g;
    private Ui ui;
    private Pacer pace;
    private Consumer<Ui> draw;
    private Runnable start;
    private BooleanSupplier close;
    private final List<Predicate<Event>> handlers = new ArrayList<>();

    /** Makes an app. Nothing opens until start() or run(). */
    public App(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    /** Ui.APP_ flags for the root panel. */
    public App flags(int v) {
        flags = v;
        return this;
    }

    /** Frame rate while something is moving, 60 by default. */
    public App fps(int v) {
        fps = v;
        return this;
    }

    /** How long it sleeps waiting for an event when nothing moves, 500 by default. */
    public App idleMs(int v) {
        idleMs = v;
        return this;
    }

    /** Starts with the light theme instead of the dark one. */
    public App light(boolean v) {
        light = v;
        return this;
    }

    /** Open with these options instead of the plain openApp window. */
    public App options(Options o) {
        opts = o;
        return this;
    }

    /** Called every painted frame inside the app panel, draw your widgets here. */
    public App onFrame(Consumer<Ui> fn) {
        draw = fn;
        return this;
    }

    /** Looks at every event before the ui. Return true to swallow it. */
    public App onEvent(Predicate<Event> fn) {
        handlers.add(fn);
        return this;
    }

    /** Called when the window is closed, return false to keep it open. */
    public App onClose(BooleanSupplier fn) {
        close = fn;
        return this;
    }

    /** Runs once after the window is open. */
    public App onStart(Runnable fn) {
        start = fn;
        return this;
    }

    /** Keeps painting every frame, for things that move on their own. */
    public void animate(boolean on) {
        animate = on;
    }

    /** Asks for a repaint, use it after changing something outside an event. */
    public void redraw() {
        dirty = Math.max(dirty, 1);
    }

    /** Ends the loop after the current step. */
    public void quit() {
        running = false;
    }

    /** True between start() and quit(). */
    public boolean running() {
        return running;
    }

    /** How many frames have been painted. */
    public int frames() {
        return frames;
    }

    /** The Goome, null until started. */
    public Goome goome() {
        return g;
    }

    /** The Ui, null until started. */
    public Ui ui() {
        return ui;
    }

    /** Opens the window and runs onStart. For when you want your own loop with step(). */
    public App start() {
        if (opts != null) {
            Options o = opts;
            if (o.title == null) o.title(title);
            if (o.width == 0 && o.height == 0) o.size(width, height);
            o.mode(Goome.MODE_WINDOW);
            if (o.windowFlags == 0) o.windowFlags(Goome.WINDOW_RESIZABLE | Goome.WINDOW_CENTER);
            g = Goome.open(o);
        } else {
            g = Goome.openApp(title, width, height);
        }
        ui = g.ui();
        if (light) ui.theme().light();
        every = g.backendName().split(" ")[0].equals("test");
        pace = new Pacer(fps);
        dirty = 2;
        running = true;
        if (start != null) start.run();
        return this;
    }

    /**
     * Handles pending events and paints if needed. wait = true sleeps until something happens. Returns
     * false once the app is done.
     */
    public boolean step(boolean wait) {
        boolean busy = every || animate || dirty > 0 || ui.animating();
        if (dirty > 0) dirty--;
        int timeout = wait ? (busy ? pace.next() : idleMs) : 0;
        for (Event e = g.poll(timeout); e != null; e = g.poll(0)) {
            handle(e);
            dirty = Math.max(dirty, 1);
            if (!running) return false;
        }
        if (running && (busy || dirty > 0)) paint();
        return running;
    }

    private void handle(Event e) {
        for (Predicate<Event> h : handlers) if (h.test(e)) return;
        ui.feed(e);
        if (e.type() == Goome.QUIT) running = false;
        else if (e.type() == Goome.CLOSE && (close == null || close.getAsBoolean())) running = false;
    }

    private void paint() {
        if (ui.begin(0) == null) return;
        try {
            if (ui.app(title, flags) && draw != null) draw.accept(ui);
            ui.appEnd();
        } finally {
            ui.end();
        }
        frames++;
    }

    /** Closes the ui and the window. */
    public void stop() {
        running = false;
        if (ui != null) ui.close();
        if (g != null) g.close();
        if (pace != null) pace.close();
        ui = null;
        g = null;
        pace = null;
    }

    /** start(), loops step(true) until it ends, then stop(). */
    public void run() {
        start();
        try {
            while (step(true)) {
            }
        } finally {
            stop();
        }
    }
}
