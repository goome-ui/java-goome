package io.github.goomeui.examples;

import io.github.goomeui.App;
import io.github.goomeui.Flag;
import io.github.goomeui.Goome;
import io.github.goomeui.Split;
import io.github.goomeui.TextBuffer;
import io.github.goomeui.Ui;
import java.util.ArrayList;
import java.util.List;

public final class Notes {
    static final class Note {
        String title;
        final TextBuffer body;

        Note(String title, String body) {
            this.title = title;
            this.body = new TextBuffer(8192, body);
        }
    }

    public static void main(String[] args) {
        List<Note> notes = new ArrayList<>();
        notes.add(new Note("Groceries", "milk\neggs\ncoffee\nrice\ncat food"));
        notes.add(new Note("Release checklist", "bump the version\nrun the tests\nwrite the changelog\ntag it"));
        notes.add(new Note("Trip", "train at 8:10, book the hotel"));
        int[] sel = {0};
        Flag wrap = new Flag(true), about = new Flag(false), light = new Flag(false);
        Split side = new Split(0.27, false);
        App app = new App("Notes", 980, 640).flags(Ui.APP_MENUBAR | Ui.APP_STATUSBAR | Ui.APP_NO_PAD);
        app.onEvent(e -> {
            if (e.type() == Goome.KEY && (e.mods() & Goome.CTRL) != 0) {
                if (e.key() == 'n') notes.add(new Note("Untitled", ""));
                if (e.key() == 'q') app.quit();
                app.redraw();
            }
            return false;
        });
        app.onFrame(ui -> {
            if (ui.menubarBegin()) {
                if (ui.menuBegin("File")) {
                    if (ui.menuItem("New note", "Ctrl+N")) {
                        notes.add(new Note("Untitled", ""));
                        sel[0] = notes.size() - 1;
                    }
                    ui.menuSeparator();
                    if (ui.menuItem("Quit", "Ctrl+Q")) app.quit();
                    ui.menuEnd();
                }
                if (ui.menuBegin("View")) {
                    ui.menuItem("Wrap lines", null, wrap);
                    if (ui.menuItem("Light theme", null, light)) {
                        if (light.get()) ui.theme().light();
                        else ui.theme().dark();
                    }
                    ui.menuEnd();
                }
                if (ui.menuBegin("Help")) {
                    if (ui.menuItem("About")) about.set(true);
                    ui.menuEnd();
                }
                ui.menubarEnd();
            }
            if (ui.splitBegin("main", side)) {
                for (int i = 0; i < notes.size(); i++) {
                    ui.pushId(i);
                    if (ui.selectable(notes.get(i).title, i == sel[0])) sel[0] = i;
                    ui.popId();
                }
                ui.splitNext();
                if (sel[0] < notes.size()) {
                    Note n = notes.get(sel[0]);
                    n.title = ui.input("Title", n.title, 128);
                    ui.textEdit("body", n.body, 0, wrap.get() ? Ui.EDIT_WRAP : 0);
                }
                ui.splitEnd();
            }
            if (about.get() && ui.modalBegin("About", about)) {
                ui.label("a tiny notes app made with goome " + Goome.VERSION);
                if (ui.button("Close")) ui.popupClose();
                ui.modalEnd();
            }
            ui.statusbar(notes.size() + " notes");
        });
        app.run();
        for (Note n : notes) n.body.close();
    }
}
