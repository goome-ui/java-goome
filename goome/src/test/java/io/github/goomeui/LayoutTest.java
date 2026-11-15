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
    }

    @Test
    void everyFunctionIsThere() {
        for (Fn f : Fn.values()) if (!f.optional) assertTrue(f.present(), f.symbol);
    }

    @Test
    void pickedTheAskedPath() {
        String want = System.getProperty("goome.native");
        if (want != null && !want.isEmpty() && !want.equals("auto")) assertEquals(want, Goome.nativeKind());
    }
}
