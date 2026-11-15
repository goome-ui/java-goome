package io.github.goomeui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

    public App(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public App flags(int v) {
        flags = v;
        return this;
    }

    public App fps(int v) {
        fps = v;
        return this;
    }

    public App idleMs(int v) {
        idleMs = v;
        return this;
    }

    public App light(boolean v) {
        light = v;
        return this;
    }

    public App options(Options o) {
        opts = o;
        return this;
    }

    public App onFrame(Consumer<Ui> fn) {
        draw = fn;
        return this;
    }

    public App onEvent(Predicate<Event> fn) {
        handlers.add(fn);
        return this;
    }

    public App onClose(BooleanSupplier fn) {
        close = fn;
        return this;
    }

    public App onStart(Runnable fn) {
        start = fn;
        return this;
    }

    public void animate(boolean on) {
        animate = on;
    }

    public void redraw() {
        dirty = Math.max(dirty, 1);
    }

    public void quit() {
        running = false;
    }

    public boolean running() {
        return running;
    }

    public int frames() {
        return frames;
    }

    public Goome goome() {
        return g;
    }

    public Ui ui() {
        return ui;
    }

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

    public void stop() {
        running = false;
        if (ui != null) ui.close();
        if (g != null) g.close();
        if (pace != null) pace.close();
        ui = null;
        g = null;
        pace = null;
    }

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
