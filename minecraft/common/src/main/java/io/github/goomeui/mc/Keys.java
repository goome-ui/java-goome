package io.github.goomeui.mc;

import io.github.goomeui.Goome;

public final class Keys {
    private Keys() {
    }

    public static boolean printable(int goomeKey, int goomeMods) {
        boolean chord = (goomeMods & (Goome.CTRL | Goome.ALT | Goome.SUPER)) != 0;
        return !chord && goomeKey >= 32 && goomeKey < 127;
    }

    public static int sdlScancode(int sc) {
        if (sc >= 4 && sc <= 29) return 'a' + (sc - 4);
        if (sc >= 30 && sc <= 38) return '1' + (sc - 30);
        if (sc == 39) return '0';
        if (sc >= 58 && sc <= 69) return Goome.K_F1 + (sc - 58);
        switch (sc) {
            case 40:
            case 88: return Goome.K_ENTER;
            case 41: return Goome.K_ESC;
            case 42: return Goome.K_BACKSPACE;
            case 43: return Goome.K_TAB;
            case 44: return ' ';
            case 45: return '-';
            case 46: return '=';
            case 47: return '[';
            case 48: return ']';
            case 49: return '\\';
            case 51: return ';';
            case 52: return '\'';
            case 53: return '`';
            case 54: return ',';
            case 55: return '.';
            case 56: return '/';
            case 73: return Goome.K_INSERT;
            case 74: return Goome.K_HOME;
            case 75: return Goome.K_PGUP;
            case 76: return Goome.K_DELETE;
            case 77: return Goome.K_END;
            case 78: return Goome.K_PGDN;
            case 79: return Goome.K_RIGHT;
            case 80: return Goome.K_LEFT;
            case 81: return Goome.K_DOWN;
            case 82: return Goome.K_UP;
            default: return 0;
        }
    }

    public static int sdlMods(int m) {
        int out = 0;
        if ((m & 0x0003) != 0) out |= Goome.SHIFT;
        if ((m & 0x00C0) != 0) out |= Goome.CTRL;
        if ((m & 0x0300) != 0) out |= Goome.ALT;
        if ((m & 0x0C00) != 0) out |= Goome.SUPER;
        return out;
    }

    public static int glfwKey(int k) {
        if (k >= 65 && k <= 90) return 'a' + (k - 65);
        if (k >= 32 && k < 127) return k;
        if (k >= 290 && k <= 301) return Goome.K_F1 + (k - 290);
        switch (k) {
            case 256: return Goome.K_ESC;
            case 257:
            case 335: return Goome.K_ENTER;
            case 258: return Goome.K_TAB;
            case 259: return Goome.K_BACKSPACE;
            case 260: return Goome.K_INSERT;
            case 261: return Goome.K_DELETE;
            case 262: return Goome.K_RIGHT;
            case 263: return Goome.K_LEFT;
            case 264: return Goome.K_DOWN;
            case 265: return Goome.K_UP;
            case 266: return Goome.K_PGUP;
            case 267: return Goome.K_PGDN;
            case 268: return Goome.K_HOME;
            case 269: return Goome.K_END;
            default: return 0;
        }
    }

    public static int glfwMods(int m) {
        int out = 0;
        if ((m & 1) != 0) out |= Goome.SHIFT;
        if ((m & 2) != 0) out |= Goome.CTRL;
        if ((m & 4) != 0) out |= Goome.ALT;
        if ((m & 8) != 0) out |= Goome.SUPER;
        return out;
    }
}
