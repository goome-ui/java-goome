package io.github.goomeui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LayoutTest {
    @Test
    void matchesC() throws Exception {
        String out = T.runC("tests/layout.c");
        if (out == null) return;
        Map<String, String> c = new HashMap<>();
        for (String line : out.split("\\R")) {
            if (line.isBlank()) continue;
            int sp = line.indexOf(' ');
            c.put(line.substring(0, sp), line.substring(sp + 1).trim());
        }
        int checked = 0;
        for (Layout l : Layout.ALL) {
            assertEquals(c.get(l.name), String.valueOf(l.size), "sizeof " + l.name);
            for (Map.Entry<String, int[]> f : l.fields().entrySet()) {
                String key = l.name + "." + f.getKey();
                assertEquals(c.get(key), f.getValue()[0] + " " + f.getValue()[1], key);
                checked++;
            }
        }
        long cFields = c.keySet().stream().filter(k -> k.contains(".")).count();
        assertEquals(cFields, checked, "layout.c has fields Layout.java doesnt");
        Map<String, Integer> java = constants();
        int consts = 0;
        for (Map.Entry<String, String> e : c.entrySet()) {
            if (!e.getKey().startsWith("GOOME_")) continue;
            assertEquals(e.getValue(), String.valueOf(java.get(e.getKey())), e.getKey());
            consts++;
        }
        assertEquals(java.size(), consts, "Java has constants layout.c doesnt print");
    }

    static Map<String, Integer> constants() {
        Map<String, Integer> m = new HashMap<>();
        m.put("GOOME_TEXT", Goome.TEXT);
        m.put("GOOME_PREEDIT", Goome.PREEDIT);
        m.put("GOOME_DIR_AUTO", Font.DIR_AUTO);
        m.put("GOOME_DIR_LTR", Font.DIR_LTR);
        m.put("GOOME_DIR_RTL", Font.DIR_RTL);
        String[] ease = {"SMOOTH", "LINEAR", "OUT", "IN_OUT", "BACK", "CUSTOM", "IN", "SPRING", "BOUNCE"};
        int[] ev = {Theme.EASE_SMOOTH, Theme.EASE_LINEAR, Theme.EASE_OUT, Theme.EASE_IN_OUT, Theme.EASE_BACK, Theme.EASE_CUSTOM, Theme.EASE_IN,
            Theme.EASE_SPRING, Theme.EASE_BOUNCE};
        for (int k = 0; k < ease.length; k++) m.put("GOOME_EASE_" + ease[k], ev[k]);
        int[] roles = {A11yNode.ROLE_NONE, A11yNode.ROLE_WINDOW, A11yNode.ROLE_BUTTON, A11yNode.ROLE_CHECKBOX, A11yNode.ROLE_RADIO,
            A11yNode.ROLE_SLIDER, A11yNode.ROLE_TEXT, A11yNode.ROLE_INPUT, A11yNode.ROLE_COMBO, A11yNode.ROLE_LIST, A11yNode.ROLE_ITEM,
            A11yNode.ROLE_TAB, A11yNode.ROLE_MENU, A11yNode.ROLE_MENUITEM, A11yNode.ROLE_TABLE, A11yNode.ROLE_TREE, A11yNode.ROLE_PROGRESS,
            A11yNode.ROLE_IMAGE, A11yNode.ROLE_GROUP, A11yNode.ROLE_DIALOG};
        for (int k = 0; k < roles.length; k++) m.put("GOOME_ROLE_" + A11yNode.ROLE_NAMES.get(k).toUpperCase(java.util.Locale.ROOT), roles[k]);
        m.put("GOOME_A11Y_FOCUSED", A11yNode.FOCUSED);
        m.put("GOOME_A11Y_CHECKED", A11yNode.CHECKED);
        m.put("GOOME_A11Y_DISABLED", A11yNode.DISABLED);
        m.put("GOOME_A11Y_EXPANDED", A11yNode.EXPANDED);
        m.put("GOOME_A11Y_SELECTED", A11yNode.SELECTED);
        return m;
    }

    @Test
    void everyFunctionIsThere() {
        for (Fn f : Fn.values()) assertTrue(f.present(), f.symbol);
    }

    @Test
    void pickedTheAskedPath() {
        String want = System.getProperty("goome.native");
        if (want != null && !want.isEmpty() && !want.equals("auto")) assertEquals(want, Goome.nativeKind());
    }
}
