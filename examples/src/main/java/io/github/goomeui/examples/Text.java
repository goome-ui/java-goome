package io.github.goomeui.examples;

import io.github.goomeui.Event;
import io.github.goomeui.Font;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Image;
import io.github.goomeui.Options;
import io.github.goomeui.TextBuffer;
import io.github.goomeui.Ui;

public final class Text {
    static final String[] LINES = {
        "goome 😀 🎉 👍🏽 👨‍👩‍👧 🇩🇪 #️⃣ ❤️ 🐱",
        "السلام عليكم، كيف حالك؟",
        "שלום עולם, מה שלומך?",
        "goome says مرحبا (123) to שלום 👋",
        "لا لأن محمد كتب 2024 [ok]",
    };

    static void save(Goome g, String path) {
        Image f = g.lastFrame();
        int[] px = f.copy();
        for (int i = 0; i < px.length; i++) {
            int s = px[i], a = s >>> 24, v = 0xFF000000;
            for (int k = 0; k < 24; k += 8) v |= (((s >>> k) & 255) + (0x20 * (255 - a)) / 255) << k;
            px[i] = v;
        }
        Goome.savePpm(path, px, f.w, f.h, f.w);
    }

    public static void main(String[] args) {
        String shot = args.length > 0 ? args[0] : null;
        Goome g;
        try {
            g = Goome.open(new Options().title("goome text").mode(shot != null ? Goome.MODE_OFFSCREEN : Goome.MODE_WINDOW).size(560, 600)
                .scale(shot != null ? 2 : 0).windowFlags(Goome.WINDOW_RESIZABLE | Goome.WINDOW_CENTER));
        } catch (GoomeException e) {
            System.err.println("cant open: " + e.getMessage());
            System.exit(1);
            return;
        }
        try (g; Ui u = g.ui(); TextBuffer notes = new TextBuffer(1024,
            "שלום! this line starts in hebrew\nmixed: abc ערבי 42 👍🏽\nمرحبا بكم")) {
            if (shot != null) {
                u.theme().animMs(0);
                u.config().blinkMs(0);
            }
            int dir = Font.DIR_AUTO, frame = 0;
            boolean run = true;
            String name = "العربية abc 😀";
            while (run) {
                if (shot != null && frame == 3) {
                    g.push(Event.key(Goome.K_END, 0, ""));
                    for (int i = 0; i < 6; i++) g.push(Event.key(Goome.K_RIGHT, Goome.SHIFT, ""));
                }
                for (Event e = g.poll(shot != null ? 0 : 16); e != null; e = g.poll(0)) {
                    if (e.type() == Goome.CLOSE || e.type() == Goome.QUIT) run = false;
                    u.feed(e);
                }
                if (u.begin(0) == null) continue;
                Font.textDir(dir);
                try {
                    if (u.app("goome text", 0)) {
                        u.heading("Emoji, Arabic, Hebrew");
                        for (String l : LINES) u.label(l);
                        u.separator();
                        u.row(3);
                        dir = u.radio("Auto", dir, Font.DIR_AUTO);
                        dir = u.radio("Left to right", dir, Font.DIR_LTR);
                        dir = u.radio("Right to left", dir, Font.DIR_RTL);
                        u.row(1);
                        if (shot != null && frame == 2) u.setFocus();
                        name = u.input("Name", name, 128);
                        u.textEdit("notes", notes, 120, Ui.EDIT_WRAP);
                        u.appEnd();
                    }
                } finally {
                    u.end();
                    Font.textDir(Font.DIR_AUTO);
                }
                if (shot != null && ++frame == 6) run = false;
            }
            if (shot != null) save(g, shot);
        }
    }
}
