package io.github.goomeui.examples;

import io.github.goomeui.Bitmap;
import io.github.goomeui.Event;
import io.github.goomeui.Flag;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Monitor;
import io.github.goomeui.Options;
import io.github.goomeui.Pacer;
import io.github.goomeui.Rect;
import io.github.goomeui.Theme;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.util.List;
import java.util.Locale;

public final class UiDemo {
    static final String[] FRUITS = {"Mango", "Banana", "Cherry", "Kiwi", "Apple", "Lemon", "Grape", "Fig", "Dragonfruit", "Pear"};
    static final String[] TABS = {"Widgets", "Images", "Theme", "About"};

    static String f(String fmt, Object... a) {
        return String.format(Locale.ROOT, fmt, a);
    }

    static Bitmap gradient() {
        int[] px = new int[128 * 128];
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int r = 0x2a + x * (0x6f - 0x2a) / 127, gr = 0x40 + y * (0xdc - 0x40) / 127, bl = 0x9a - (x + y) * 0x50 / 254;
                px[y * 128 + x] = 0xFF000000 | r << 16 | gr << 8 | bl;
            }
        }
        return Bitmap.fromArgb(px, 128, 128);
    }

    static void setTheme(Theme t, boolean light, int accent, boolean ownAccent, double radius, double anim, boolean solid) {
        if (light) t.light();
        else t.dark();
        if (ownAccent) t.accent(accent);
        t.radius(radius).animMs(anim);
        if (solid) t.panel(t.panel() | 0xFF000000);
    }

    public static void main(String[] args) {
        Goome g;
        try {
            g = Goome.open(new Options().title("goome ui").noCapture(true));
        } catch (GoomeException e) {
            System.err.println("cant open an overlay: " + e.getMessage());
            System.exit(1);
            return;
        }
        try (g; Ui u = g.ui(); Pacer pace = new Pacer(60)) {
            List<Monitor> mons = g.monitors();
            Rect area = mons.isEmpty() ? new Rect(0, 0, 1280, 720) : mons.get(0).area;
            Bitmap logo = null;
            boolean haveLogo = false;
            for (String p : new String[] {"docs/logo.png", "../docs/logo.png"}) {
                try {
                    logo = Bitmap.load(p);
                    haveLogo = true;
                    break;
                } catch (GoomeException e) {
                    continue;
                }
            }
            if (logo == null) logo = gradient();

            Flag more = new Flag(true);
            Window main = new Window(area.x + 40, area.y + 40, 440, 620);
            Window stats = new Window(area.x + 500, area.y + 40, 280, 0);
            boolean grid = true, wifi = true, notify = false, light = false, ownAccent = false, solid = false, always = false, running = true;
            int tab = 0, clicks = 0, mode = 1, count = 42, fruit = 2, picked = -1, imgClicks = 0;
            boolean test = g.backendName().equals("test");
            double volume = 0.65, radius = 10, anim = 120;
            int highlight = 0xFF4FA3FF, accent = 0xFF6FDC5F;
            String name = "goome", search = "";
            long spent = 0, tframes = 0, fpsT = test ? 0 : Goome.nowMs();
            double fps = 0, frameMs = 0, accMs = 0;
            int accN = 0, fpsN = 0;
            boolean dirty = true;
            while (running) {
                int wait = dirty || always || test || u.animating() ? pace.next() : 500;
                for (Event ev = g.poll(wait); ev != null; ev = g.poll(0)) {
                    if (ev.type() == Goome.QUIT) running = false;
                    u.feed(ev);
                    dirty = true;
                }
                if (!running) break;
                if (!dirty && !always && !test && !u.animating()) continue;
                dirty = false;

                long t0 = test ? 0 : Goome.nowMs();
                if (u.begin(0) == null) continue;
                if (u.window("goome ui", main, Ui.WIN_MOVE | Ui.WIN_RESIZE | Ui.WIN_COLLAPSE)) {
                    tab = u.tabs("tabs", tab, TABS);
                    u.pushId(TABS[tab]);
                    u.scrollBegin("page", 0);
                    if (tab == 0) {
                        u.heading("Buttons");
                        u.row(3);
                        if (u.button("Click me")) clicks++;
                        if (u.button("Reset")) clicks = 0;
                        u.button("Hover me");
                        u.tooltip("hi :)");
                        u.label(clicks == 0 ? "not clicked yet" : clicks == 1 ? "clicked once" : "clicked " + clicks + " times");
                        u.separator();
                        u.heading("Switches");
                        grid = u.checkbox("Show grid", grid);
                        u.sameLine();
                        notify = u.checkbox("Notifications", notify);
                        wifi = u.toggle("Wi-Fi", wifi);
                        mode = u.radio("Small", mode, 0);
                        u.sameLine();
                        mode = u.radio("Medium", mode, 1);
                        u.sameLine();
                        mode = u.radio("Large", mode, 2);
                        u.separator();
                        u.heading("Values");
                        volume = u.slider("Volume", volume, 0, 1, "%.2f");
                        count = u.sliderInt("Count", count, 0, 100);
                        u.progress(volume, (int) (volume * 100 + 0.5) + "%");
                        u.progress(-1);
                        u.row(2);
                        name = u.input("Name", name, 64);
                        search = u.input("Search", search, 64);
                        fruit = u.combo("Fruit", fruit, FRUITS);
                        highlight = u.color("Highlight", highlight);
                        if (u.collapsing("More stuff", more)) {
                            u.wrapped("a long list so theres something to scroll. pick one, it shows up under it.");
                            u.scrollBegin("list", 150);
                            for (int i = 0; i < 30; i++) if (u.selectable("Item " + (i + 1), picked == i)) picked = i;
                            u.scrollEnd();
                            u.label(picked >= 0 ? "picked item " + (picked + 1) : "nothing picked");
                        }
                    } else if (tab == 1) {
                        u.heading(haveLogo ? "docs/logo.png" : "Generated gradient");
                        u.image(logo, 120, 120, 18);
                        u.sameLine();
                        u.image(logo, 120, 120, 60);
                        u.sameLine();
                        u.image(logo, 120, 120, 0);
                        u.label("image buttons");
                        for (int i = 0; i < 4; i++) {
                            if (i > 0) u.sameLine();
                            if (u.imageButton("img" + i, logo, 56, 56)) imgClicks++;
                        }
                        u.label(imgClicks == 0 ? "no image clicks yet" : imgClicks == 1 ? "one image click" : imgClicks + " image clicks");
                        u.image(logo, 0, 90, 10);
                    } else if (tab == 2) {
                        u.heading("Theme");
                        int lv = light ? 1 : 0, was = lv;
                        lv = u.radio("Dark", lv, 0);
                        u.sameLine();
                        lv = u.radio("Light", lv, 1);
                        light = lv == 1;
                        boolean ch = lv != was;
                        accent = u.color("Accent", accent);
                        if (u.changed()) ownAccent = ch = true;
                        radius = u.slider("Corner radius", radius, 0, 20, "%.0f px");
                        ch |= u.changed();
                        anim = u.slider("Animation", anim, 0, 400, "%.0f ms");
                        ch |= u.changed();
                        solid = u.toggle("Solid panels", solid);
                        ch |= u.changed();
                        if (u.button("Reset theme")) {
                            light = ownAccent = solid = false;
                            radius = 10;
                            anim = 120;
                            accent = 0xFF6FDC5F;
                            ch = true;
                        }
                        if (ch) setTheme(u.theme(), light, accent, ownAccent, radius, anim, solid);
                        if (!ownAccent) accent = u.theme().accent();
                    } else {
                        u.heading("About");
                        u.wrapped("made with goome. try clicking next to this window, your desktop still gets it.");
                        u.label("goome " + Goome.VERSION + " on " + g.backendName());
                        u.label(f("%.0f fps, %.2f ms a frame", fps, frameMs));
                        if (!stats.open() && u.button("Show stats")) stats.open(true);
                    }
                    u.scrollEnd();
                    u.popId();
                }
                u.windowEnd();

                if (u.window("Stats", stats, Ui.WIN_MOVE | Ui.WIN_COLLAPSE | Ui.WIN_CLOSE | Ui.WIN_RESIZE)) {
                    u.label(f("%.0f fps", fps));
                    u.label(f("%.2f ms a frame", frameMs));
                    u.progress(frameMs / 16.7, (int) (frameMs * 100 / 16.7 + 0.5) + "% of a 60 fps frame");
                    always = u.toggle("Redraw every frame", always);
                    if (u.button("Quit")) running = false;
                }
                u.windowEnd();
                u.end();

                long t1 = test ? 0 : Goome.nowMs();
                spent += t1 - t0;
                tframes++;
                accMs += t1 - t0;
                accN++;
                fpsN++;
                if (t1 - fpsT >= 500) {
                    fps = fpsN * 1000.0 / (t1 - fpsT);
                    frameMs = accMs / accN;
                    fpsT = t1;
                    fpsN = accN = 0;
                    accMs = 0;
                }
            }
            if (tframes > 0) System.out.println(f("%d frames, %.2f ms a frame", tframes, (double) spent / tframes));
            logo.close();
        }
    }
}
