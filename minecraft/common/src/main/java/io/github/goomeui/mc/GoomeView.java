package io.github.goomeui.mc;

import io.github.goomeui.Event;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Image;
import io.github.goomeui.PixelFormat;
import io.github.goomeui.Rect;
import io.github.goomeui.Ui;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public final class GoomeView implements AutoCloseable {
    public interface Sink {
        ByteBuffer pixels(int w, int h);

        void uploaded(Rect dirty);
    }

    private final Goome g;
    private final Ui ui;
    private int held;
    private int lastW = -1, lastH = -1;
    private String clip = "";

    public GoomeView(int w, int h, double scale) {
        g = Goome.openOffscreen(Math.max(1, w), Math.max(1, h), scale > 0 ? scale : 1, "minecraft");
        ui = g.ui();
        ui.autoGrab(0);
    }

    public Goome goome() {
        return g;
    }

    public Ui ui() {
        return ui;
    }

    public void resize(int w, int h, double scale) {
        g.resize(Math.max(1, w), Math.max(1, h), scale > 0 ? scale : 1);
        for (Event e : g.events(0)) ui.feed(e);
    }

    public boolean wantsMouse() {
        return ui.wantsMouse();
    }

    public boolean wantsKeyboard() {
        return ui.wantsKeyboard();
    }

    public boolean over(double x, double y) {
        return g.hit(x, y);
    }

    public boolean mouseMoved(double x, double y) {
        ui.feed(Event.motion(x, y));
        return held != 0 || ui.wantsMouse();
    }

    public boolean mouseDown(double x, double y, int button) {
        boolean mine = g.hit(x, y) || ui.wantsMouse();
        ui.feed(Event.motion(x, y));
        ui.feed(Event.down(x, y, button));
        mine |= ui.wantsMouse();
        if (mine) held |= 1 << button;
        return mine;
    }

    public boolean mouseUp(double x, double y, int button) {
        boolean mine = (held & (1 << button)) != 0;
        held &= ~(1 << button);
        ui.feed(Event.up(x, y, button));
        return mine;
    }

    public boolean scroll(double x, double y, double amount) {
        int dy = amount > 0 ? -1 : amount < 0 ? 1 : 0;
        if (dy == 0) return false;
        boolean mine = g.hit(x, y) || ui.wantsMouse();
        if (mine) ui.feed(Event.scroll(x, y, dy));
        return mine;
    }

    public boolean key(int key, int mods) {
        if (key == 0) return ui.wantsKeyboard();
        ui.feed(Event.key(key, mods, ""));
        return ui.wantsKeyboard();
    }

    public boolean text(String s, int mods) {
        boolean mine = ui.wantsKeyboard();
        if (!mine) return false;
        s.codePoints().forEach(cp -> {
            int k = cp < 128 ? Character.toLowerCase(cp) : 0;
            ui.feed(Event.key(k, mods, new String(Character.toChars(cp))));
        });
        return true;
    }

    public void focus(boolean gained) {
        ui.feed(Event.focus(gained));
        if (!gained) held = 0;
    }

    public void pasteFrom(String text) {
        if (text == null) text = "";
        if (!text.equals(clip)) {
            clip = text;
            g.clipboard(text);
        }
    }

    public String copied() {
        String now = g.clipboard();
        if (now.equals(clip)) return null;
        clip = now;
        return now;
    }

    public int cursor() {
        return g.cursor();
    }

    public void frame(Consumer<Ui> body) {
        for (Event e : g.events(0)) ui.feed(e);
        if (ui.begin(0) == null) return;
        try {
            body.accept(ui);
        } finally {
            ui.end();
        }
    }

    public boolean upload(Sink sink) {
        Rect d = g.frameDirty();
        if (d == null) return false;
        Image f;
        try {
            f = g.lastFrame();
        } catch (GoomeException.Again e) {
            return false;
        }
        boolean fresh = f.w != lastW || f.h != lastH;
        ByteBuffer px = sink.pixels(f.w, f.h);
        if (fresh) d = new Rect(0, 0, f.w, f.h);
        ByteBuffer at = px.duplicate();
        at.position((d.y * f.w + d.x) * 4);
        g.frameCopy(at, f.w * 4, PixelFormat.RGBA_PREMUL, d);
        lastW = f.w;
        lastH = f.h;
        sink.uploaded(d);
        return true;
    }

    @Override
    public void close() {
        ui.close();
        g.close();
    }
}
