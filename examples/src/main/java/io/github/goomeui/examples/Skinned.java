package io.github.goomeui.examples;

import io.github.goomeui.Bitmap;
import io.github.goomeui.Event;
import io.github.goomeui.Flag;
import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Icon;
import io.github.goomeui.Options;
import io.github.goomeui.Pacer;
import io.github.goomeui.Skin;
import io.github.goomeui.Sort;
import io.github.goomeui.Split;
import io.github.goomeui.Style;
import io.github.goomeui.Theme;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Skinned {
    static final String[] TABS = {"General", "Sound", "About"};
    static final String[] BRUSHES = {"Round", "Pencil", "Ink pen", "Marker", "Charcoal"};
    static final Object[][] SKINS = {
        {Ui.SKIN_BUTTON, "button", 8, 8, 8, 8}, {Ui.SKIN_INPUT, "input", 8, 8, 8, 8}, {Ui.SKIN_CHECKBOX, "checkbox", 18, 18, 18, 18},
        {Ui.SKIN_RADIO, "radio", 18, 18, 18, 18}, {Ui.SKIN_TOGGLE, "toggle", 38, 22, 38, 22}, {Ui.SKIN_SLIDER_TRACK, "slider", 3, 3, 3, 3},
        {Ui.SKIN_SLIDER_FILL, "slider_fill", 3, 3, 3, 3}, {Ui.SKIN_SCROLLBAR, "scrollbar", 3, 3, 3, 3}, {Ui.SKIN_TAB, "tab", 8, 8, 8, 8},
        {Ui.SKIN_TITLEBAR, "titlebar", 12, 12, 12, 3}, {Ui.SKIN_TOOLTIP, "tooltip", 8, 8, 8, 8}, {Ui.SKIN_MENU, "menu", 10, 10, 10, 10},
        {Ui.SKIN_POPUP, "menu", 10, 10, 10, 10},
    };
    static final String[][] FILES = {
        {"sketch.png", "image", "482"}, {"notes.txt", "text", "12"}, {"palette.gpl", "colors", "3"}, {"cover.jpg", "image", "1210"},
        {"draft.psd", "layers", "8840"}, {"todo.md", "text", "4"}, {"brushes.zip", "archive", "2304"},
    };

    final List<Bitmap> loaded = new ArrayList<>();
    Bitmap heart, appBar;
    Skin appSkin;
    int tab, size = 1, brush, sortCol = 2;
    boolean snap = true, hints, fav = true, corners = true, autosave, sortDesc = true, running = true;
    final Flag grid = new Flag(true), outline = new Flag(), inspector = new Flag(true), layers = new Flag(true), bgOpen = new Flag(true), sketchOpen = new Flag();
    double volume = 64, copies = 3, opacity = 0.8, zoom = 1;
    int tint = 0xFFE0567A;
    String name = "cozy cat sketch";
    final Split split = new Split(0.5, false);
    final Window win = new Window(610, 300, 330, 268);

    static String findAssets() {
        for (String d : new String[] {"examples/assets", "assets", "../examples/assets", "../../examples/assets"}) {
            if (new File(d, "arrow_down.png").isFile()) return d;
        }
        return null;
    }

    Bitmap load(String dir, String file) {
        try {
            Bitmap b = Bitmap.load(dir + "/" + file + ".png");
            loaded.add(b);
            return b;
        } catch (GoomeException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    static void paper(Theme t) {
        t.bg(0xFFEFE8DD).panel(0xFFFAF6F0).border(0x2E2B1E35).text(0xFF2B2233).textDim(0xFF85798C);
        t.accent(0xFF7B4FD0).accentText(0xFFFFFFFF).hover(0xFFE7DEF0).active(0xFFDACDEA).inputBg(0xFFEDE6DE).shadow(0x3A281838);
        t.radius(9).pad(10).gap(9).borderWidth(1).shadowBlur(24).animMs(120);
    }

    boolean dress(Ui u) {
        String dir = findAssets();
        if (dir == null) {
            System.err.println("cant find examples/assets, run it from the repo folder");
            return false;
        }
        Theme t = u.theme();
        paper(t);
        for (Icon i : Icon.values()) {
            Bitmap b = load(dir, i.name().toLowerCase(Locale.ROOT));
            if (b != null) t.icon(i, b);
        }
        for (Object[] s : SKINS) {
            Bitmap b = load(dir, (String) s[1]);
            if (b != null) t.skin((int) s[0], new Skin(b, (int) s[2], (int) s[3], (int) s[4], (int) s[5]));
        }
        heart = load(dir, "heart");
        appBar = load(dir, "titlebar_app");
        if (appBar != null) appSkin = new Skin(appBar, 4, 4, 4, 3);
        return true;
    }

    void menus(Ui u) {
        if (!u.menubarBegin()) return;
        if (u.menuBegin("File")) {
            u.menuItem("New", "Ctrl+N");
            u.menuItem("Open", "Ctrl+O");
            u.menuSeparator();
            if (u.menuItem("Quit", "Ctrl+Q")) running = false;
            u.menuEnd();
        }
        if (u.menuBegin("View")) {
            u.menuItem("Grid", null, grid);
            u.menuItem("Outline", null, outline);
            if (u.menuBegin("Zoom")) {
                if (u.menuItem("50%")) zoom = 0.5;
                if (u.menuItem("100%")) zoom = 1;
                if (u.menuItem("200%")) zoom = 2;
                u.menuEnd();
            }
            if (u.menuItem("Inspector", null, inspector) && win.h() <= 0) win.h(300);
            u.menuEnd();
        }
        u.menubarEnd();
    }

    void leftPane(Ui u) {
        u.heading("Look");
        tab = u.tabs("tabs", tab, TABS);
        u.space(2);
        u.row(2);
        snap = u.checkbox("Snap to grid", snap);
        hints = u.checkbox("Show hints", hints);
        if (heart != null) u.pushIcon(Icon.CHECK, heart);
        u.pushColor(Style.ACCENT, 0xFFE0567A);
        fav = u.checkbox("Favorite", fav);
        u.pop(heart != null ? 2 : 1);
        u.tooltip("this one gets a heart, pushed with pushIcon");
        size = u.radio("Small", size, 0);
        u.sameLine();
        size = u.radio("Medium", size, 1);
        u.sameLine();
        size = u.radio("Large", size, 2);
        u.row(2);
        corners = u.toggle("Soft corners", corners);
        autosave = u.toggle("Autosave", autosave);
        volume = u.slider("Volume", volume, 0, 100, "%.0f%%");
        u.row(2);
        brush = u.combo("Brush", brush, BRUSHES);
        tint = u.color("Tint", tint);
        u.row(2);
        copies = u.number("Copies", copies, 1, "%.0f");
        opacity = u.dragFloat("Opacity", opacity, 0.005, 0, 1, "%.2f");
        if (u.collapsing("Layers", layers)) {
            if (u.tree("Background", bgOpen)) {
                u.label("paper texture");
                u.label("warm fill");
                u.treeEnd();
            }
            if (u.tree("Sketch", sketchOpen)) {
                u.label("lines");
                u.treeEnd();
            }
        }
    }

    static int byCol(String[] x, String[] y, int col) {
        if (col == 2) return Integer.parseInt(x[2]) - Integer.parseInt(y[2]);
        return x[col].compareTo(y[col]);
    }

    void rightPane(Ui u) {
        u.heading("Files");
        int[] order = new int[FILES.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        if (u.tableBegin("files", 3, 0, Ui.TABLE_SORTABLE | Ui.TABLE_STRIPES)) {
            u.tableColumn("Name", 0);
            u.tableColumn("Kind", 90);
            u.tableColumn("KB", 70);
            Sort s = u.tableSort(sortCol, sortDesc);
            sortCol = s.col;
            sortDesc = s.desc;
            for (int i = 1; i < order.length; i++) {
                for (int j = i; j > 0; j--) {
                    int c = sortCol >= 0 ? byCol(FILES[order[j - 1]], FILES[order[j]], sortCol) : 0;
                    if (sortDesc ? c >= 0 : c <= 0) break;
                    int t = order[j];
                    order[j] = order[j - 1];
                    order[j - 1] = t;
                }
            }
            for (int i : order) {
                u.tableRow();
                u.label(FILES[i][0]);
                u.tableNext();
                u.label(FILES[i][1]);
                u.tableNext();
                u.label(FILES[i][2]);
            }
        }
        u.tableEnd();
        u.space(4);
        name = u.input("Name", name, 64);
        u.progress(volume / 100, "exporting");
        u.row(3);
        if (u.button("Export")) u.toast("exported, check your folder", 2500);
        u.tooltip("saves every layer as its own png");
        u.button("Share");
        if (u.button("Inspector")) inspector.set(true);
    }

    void inspector(Ui u) {
        win.open(inspector.get());
        if (u.window("Inspector", win, Ui.WIN_MOVE | Ui.WIN_RESIZE | Ui.WIN_COLLAPSE | Ui.WIN_CLOSE)) {
            u.label("Brush settings");
            opacity = u.slider("Flow", opacity, 0, 1, "%.2f");
            snap = u.checkbox("Pressure", snap);
            u.separator();
            for (int i = 0; i < 14; i++) u.label("stroke %d, %d px", i + 1, 2 + i * 3);
        }
        u.windowEnd();
        inspector.set(win.open());
    }

    void frame(Ui u, boolean first) {
        if (first) u.toast("skin loaded, every icon is a png now", 4000);
        if (appSkin != null) u.pushSkin(Ui.SKIN_TITLEBAR, appSkin);
        u.app("Skinned", Ui.APP_TITLEBAR | Ui.APP_MENUBAR | Ui.APP_STATUSBAR | Ui.APP_NO_PAD);
        if (appSkin != null) u.pop();
        menus(u);
        u.splitBegin("main", split);
        leftPane(u);
        u.splitNext();
        rightPane(u);
        u.splitEnd();
        u.statusbar(String.format(Locale.ROOT, "%d files, zoom %.0f%%, every glyph and part here is a png from examples/assets", FILES.length, zoom * 100));
        u.appEnd();
        if (inspector.get()) inspector(u);
    }

    public static void main(String[] args) {
        Goome g;
        try {
            g = Goome.open(new Options().title("Skinned").mode(Goome.MODE_WINDOW).size(980, 680).minSize(760, 520)
                .windowFlags(Goome.WINDOW_RESIZABLE | Goome.WINDOW_CENTER));
        } catch (GoomeException e) {
            System.err.println("cant open a window: " + e.getMessage());
            System.exit(1);
            return;
        }
        Skinned s = new Skinned();
        try (g; Ui u = g.ui(); Pacer pace = new Pacer(60)) {
            if (!s.dress(u)) System.exit(1);
            boolean test = g.backendName().startsWith("test"), first = true;
            while (s.running) {
                int wait = test || u.animating() ? pace.next() : 500;
                for (Event e = g.poll(wait); e != null; e = g.poll(0)) {
                    if (e.type() == Goome.QUIT || e.type() == Goome.CLOSE) s.running = false;
                    u.feed(e);
                }
                if (!s.running) break;
                if (u.begin(0) == null) continue;
                try {
                    s.frame(u, first);
                    first = false;
                } finally {
                    u.end();
                }
            }
        } finally {
            for (Bitmap b : s.loaded) b.close();
        }
    }
}
