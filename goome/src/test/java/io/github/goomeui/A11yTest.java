package io.github.goomeui;

import static io.github.goomeui.A11yNode.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class A11yTest {
    static final class Run {
        Goome g;
        Ui u;
        boolean dark;
        int ok, mode, fruit, tab, customHits;
        double vol = 2;
        String name = "Bo";
        final Flag open = new Flag();
        TextBuffer note;
        List<A11yNode> n = List.of();

        void frame() {
            for (Event e : g.events(0)) u.feed(e);
            if (u.begin(0) == null) return;
            u.app("Demo", Ui.APP_TITLEBAR | Ui.APP_STATUSBAR);
            u.label("Hello there");
            if (u.button("OK")) ok++;
            dark = u.checkbox("Dark", dark);
            mode = u.radio("Big", mode, 1);
            vol = u.slider("Volume", vol, 0, 10, "%.1f");
            name = u.input("Name", name, 64);
            fruit = u.combo("Fruit", fruit, "Mango", "Kiwi", "Fig");
            tab = u.tabs("tabs", tab, "One", "Two");
            if (u.tree("Folder", open)) {
                u.label("inside");
                u.treeEnd();
            }
            u.progress(0.25);
            u.textEdit("Notes", note, 60, 0);
            u.a11yRole(ROLE_BUTTON, "Knob");
            u.a11yValue("11");
            if (u.custom("knob", 40, 40).clicked()) customHits++;
            u.statusbar("ready");
            u.appEnd();
            u.end();
            n = u.a11yTree();
        }

        A11yNode find(int role, String label) {
            for (A11yNode x : n) if (x.role() == role && x.label().equals(label)) return x;
            return null;
        }

        int index(A11yNode x) {
            return n.indexOf(x);
        }
    }

    @Test
    void treeAndActions() {
        Run k = new Run();
        try (Goome g = Goome.open(new Options().backend(Goome.TEST).mode(Goome.MODE_WINDOW).size(420, 700).testScript("end 100000")); Ui u = g.ui();
            TextBuffer note = new TextBuffer(256)) {
            k.g = g;
            k.u = u;
            k.note = note;
            u.theme().font(null);
            k.frame();
            assertTrue(k.n.size() > 15);
            A11yNode app = k.find(ROLE_WINDOW, "Demo"), ok = k.find(ROLE_BUTTON, "OK"), dark = k.find(ROLE_CHECKBOX, "Dark");
            A11yNode vol = k.find(ROLE_SLIDER, "Volume"), name = k.find(ROLE_INPUT, "Name"), fruit = k.find(ROLE_COMBO, "Fruit");
            A11yNode tree = k.find(ROLE_TREE, "Folder"), knob = k.find(ROLE_BUTTON, "Knob"), tab2 = k.find(ROLE_TAB, "Two");
            A11yNode big = k.find(ROLE_RADIO, "Big"), notes = k.find(ROLE_INPUT, "Notes");
            assertEquals(0, k.index(app));
            assertEquals(-1, app.parent());
            assertEquals(0, ok.parent());
            assertTrue(ok.w() > 0 && ok.h() > 0);
            assertNotNull(k.find(ROLE_TEXT, "Hello there"));
            assertNotNull(k.find(ROLE_TEXT, "ready"));
            assertNotNull(k.find(ROLE_BUTTON, "Close"));
            assertNotNull(k.find(ROLE_BUTTON, "Minimize"));
            assertFalse(dark.checked());
            assertFalse(big.checked());
            assertEquals(2, vol.num());
            assertEquals(0, vol.min());
            assertEquals(10, vol.max());
            assertEquals("2.0", vol.value());
            assertEquals("Bo", name.value());
            assertEquals("Mango", fruit.value());
            assertFalse(fruit.expanded());
            assertEquals(ROLE_GROUP, k.n.get(tab2.parent()).role());
            assertFalse(tab2.selected());
            assertTrue(k.find(ROLE_TAB, "One").selected());
            assertFalse(tree.expanded());
            assertNull(k.find(ROLE_TEXT, "inside"));
            assertEquals("11", knob.value());
            assertEquals("button", knob.roleName());
            assertNotNull(notes);
            A11yNode pr = k.n.stream().filter(x -> x.role() == ROLE_PROGRESS).findFirst().orElseThrow();
            assertEquals(0.25, pr.num());
            assertEquals("25%", pr.value());
            List<A11yNode> before = k.n;
            k.frame();
            assertEquals(before.size(), k.n.size());
            for (int i = 0; i < before.size(); i++) assertEquals(before.get(i).id(), k.n.get(i).id());
            assertEquals(k.n.size(), k.n.stream().mapToLong(A11yNode::id).distinct().count());

            u.a11yPress(ok);
            u.a11yPress(dark);
            u.a11yPress(big);
            u.a11ySet(vol, "7.5");
            u.a11ySet(name, "hello there");
            u.a11yPress(knob);
            u.a11yExpand(tree);
            u.a11yPress(tab2);
            u.a11ySet(notes, "line one");
            k.frame();
            assertEquals(1, k.ok);
            assertTrue(k.dark);
            assertEquals(1, k.mode);
            assertEquals(7.5, k.vol);
            assertEquals("hello there", k.name);
            assertEquals(1, k.customHits);
            assertTrue(k.open.get());
            assertEquals(1, k.tab);
            assertEquals("line one", note.text());
            k.frame();
            tree = k.find(ROLE_TREE, "Folder");
            assertTrue(k.find(ROLE_CHECKBOX, "Dark").checked());
            assertEquals("7.5", k.find(ROLE_SLIDER, "Volume").value());
            assertEquals("hello there", k.find(ROLE_INPUT, "Name").value());
            assertTrue(tree.expanded());
            assertEquals(k.index(tree), k.find(ROLE_TEXT, "inside").parent());
            u.a11yExpand(tree);
            u.a11ySet(k.find(ROLE_SLIDER, "Volume"), "99");
            k.frame();
            assertTrue(k.open.get());
            assertEquals(10, k.vol);

            u.a11yFocus(k.find(ROLE_INPUT, "Name"));
            k.frame();
            k.frame();
            name = k.find(ROLE_INPUT, "Name");
            assertTrue(name.focused());
            assertEquals(11, name.caret());
            assertEquals(1, k.n.stream().filter(A11yNode::focused).count());

            u.a11yExpand(k.find(ROLE_COMBO, "Fruit"));
            k.frame();
            k.frame();
            fruit = k.find(ROLE_COMBO, "Fruit");
            A11yNode fig = k.find(ROLE_ITEM, "Fig");
            assertTrue(fruit.expanded());
            assertNotNull(fig);
            A11yNode list = k.n.get(fig.parent());
            assertEquals(ROLE_LIST, list.role());
            assertEquals(k.index(fruit), list.parent());
            u.a11yPress(fig);
            k.frame();
            k.frame();
            assertEquals(2, k.fruit);
            assertNull(k.find(ROLE_ITEM, "Fig"));
            assertEquals("Fig", k.find(ROLE_COMBO, "Fruit").value());
            u.a11ySet(k.find(ROLE_COMBO, "Fruit"), "Kiwi");
            k.frame();
            assertEquals(1, k.fruit);

            u.a11y(false);
            k.frame();
            assertTrue(k.n.isEmpty());
        }
    }

    @Test
    void offscreenIsOffUntilAsked() {
        try (Goome g = Goome.openOffscreen(200, 100, 1); Ui u = g.ui()) {
            u.theme().font(null);
            for (int pass = 0; pass < 2; pass++) {
                u.frame(0, c -> u.button("Hi"));
                assertEquals(pass, u.a11yTree().size());
                u.a11y(true);
            }
            u.frame(0, c -> u.button("Hi"));
            List<A11yNode> n = u.a11yTree();
            assertEquals(1, n.size());
            assertEquals(ROLE_BUTTON, n.get(0).role());
            assertEquals("Hi", n.get(0).label());
        }
    }
}
