package io.github.goomeui.examples;

import io.github.goomeui.Canvas;
import io.github.goomeui.Event;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Monitor;
import io.github.goomeui.Pacer;
import io.github.goomeui.Rect;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Hello {
    public static void main(String[] args) {
        Goome g;
        try {
            g = Goome.open("goome hello");
        } catch (GoomeException e) {
            System.err.println("cant open an overlay: " + Goome.strerror(e.code()));
            System.exit(1);
            return;
        }
        try (g; Pacer pace = new Pacer(30)) {
            List<Monitor> mons = g.monitors();
            System.out.println("backend: " + g.backendName());
            for (int i = 0; i < mons.size(); i++) {
                Monitor m = mons.get(i);
                System.out.printf("monitor %d: %s %dx%d at %d,%d scale %.2f%n", i, m.name, m.area.w, m.area.h, m.area.x, m.area.y, m.scale);
            }
            try {
                g.hotkey("alt+h", 1);
            } catch (GoomeException e) {
                System.out.println("no global hotkeys here: " + g.error());
            }

            double bx = mons.get(0).area.x + 80, by = mons.get(0).area.y + 80;
            int clicks = 0;
            boolean hidden = false, typing = false, running = true;
            String typed = "";
            while (running) {
                int wait = pace.next();
                for (Event ev = g.poll(wait); ev != null; ev = g.poll(pace.next())) {
                    if (ev.type() == Goome.QUIT) running = false;
                    if (ev.type() == Goome.DOWN) {
                        System.out.printf("click %.0f,%.0f%n", ev.x(), ev.y());
                        if (ev.x() > bx + 250 && ev.y() < by + 30) running = false;
                        else if (ev.y() > by + 80) g.keyboard(typing = !typing);
                        else clicks++;
                    }
                    if (ev.type() == Goome.KEY) {
                        System.out.printf("key %d mods %x text '%s'%n", ev.key(), ev.mods(), ev.text());
                        if (ev.key() == Goome.K_ESC) running = false;
                        else if (ev.key() == Goome.K_BACKSPACE && !typed.isEmpty()) typed = typed.substring(0, typed.length() - 1);
                        else if (!ev.text().isEmpty() && (typed + ev.text()).getBytes(StandardCharsets.UTF_8).length < 40) typed += ev.text();
                    }
                    if (ev.type() == Goome.HOTKEY || (ev.type() == Goome.CONTROL && ev.msg().equals("hide"))) hidden = !hidden;
                    if (ev.type() == Goome.SCROLL) by += ev.dy() * 10;
                }
                Canvas c = g.begin(0);
                if (c == null) continue;
                if (!hidden) {
                    c.roundRect(bx, by, 280, 120, 14, 0xE0202838);
                    c.frame(bx, by, 280, 120, 2, (clicks & 1) != 0 ? 0xFF6FDC5F : 0xFF8090A0);
                    c.text(bx + 16, by + 12, "hello from goome", 0xFFFFFFFF, true);
                    c.line(bx + 256, by + 10, bx + 268, by + 22, 2, 0xFFFF6B8B);
                    c.line(bx + 268, by + 10, bx + 256, by + 22, 2, 0xFFFF6B8B);
                    c.text(bx + 16, by + 46, "clicks: " + clicks, 0xFFC0C8D0);
                    c.roundRect(bx + 12, by + 80, 256, 28, 8, typing ? 0xFF2E4A30 : 0xFF303848);
                    c.text(bx + 22, by + 86, typing ? (typed.isEmpty() ? "type something..." : typed) : "click here to type", 0xFFFFFFFF);
                    c.hit(bx, by, 280, 120);
                }
                g.end(c);
            }
        }
    }
}
