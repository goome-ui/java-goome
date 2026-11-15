package io.github.goomeui.mc;

import io.github.goomeui.Goome;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.util.Locale;
import java.util.function.Supplier;

public final class Menu {
    static final String[] TABS = {"Settings", "Hud", "About"};

    public boolean hud = true;
    public boolean fullbright;
    public double speed = 1.0;
    public double hudAlpha = 0.85;
    public int hudCorner;
    public int tab;
    public String hudKey = "H";
    private final Window win = new Window(20, 20, 260, 0);
    private final Window hudWin = new Window(0, 0, 150, 52);

    public void draw(Ui ui) {
        if (ui.window("goome", win, Ui.WIN_MOVE | Ui.WIN_COLLAPSE)) {
            tab = ui.tabs("tabs", tab, TABS);
            ui.pushId(TABS[tab]);
            if (tab == 0) {
                hud = ui.toggle("Show hud (" + hudKey + ")", hud);
                fullbright = ui.toggle("Fullbright", fullbright);
                speed = ui.slider("Speed", speed, 0.1, 4, "%.1fx");
            } else if (tab == 1) {
                hudAlpha = ui.slider("Opacity", hudAlpha, 0.1, 1, "%.2f");
                hudCorner = ui.combo("Corner", hudCorner, "Top left", "Top right", "Bottom left", "Bottom right");
            } else {
                ui.wrapped("this menu is goome drawn into a minecraft texture. drag it around, type in it, the game doesnt get the input while goome has it.");
                ui.label("goome " + Goome.VERSION + " through " + Goome.nativeKind());
            }
            ui.popId();
        }
        ui.windowEnd();
    }

    public static final int HUD_W = 150, HUD_H = 52;

    public void hud(Ui ui, Supplier<String> info) {
        int a = (int) Math.round(hudAlpha * 255) & 255;
        ui.theme().panel((ui.theme().panel() & 0x00FFFFFF) | a << 24);
        hudWin.x(0).y(0).w(HUD_W).h(HUD_H);
        if (ui.window("hud", hudWin, Ui.WIN_NO_TITLE)) {
            ui.label(info.get());
            ui.label(String.format(Locale.ROOT, "speed %.1fx%s", speed, fullbright ? ", fullbright" : ""));
        }
        ui.windowEnd();
    }

    public int[] hudSpot(int guiW, int guiH) {
        int x = (hudCorner & 1) != 0 ? guiW - HUD_W - 4 : 4;
        int y = (hudCorner & 2) != 0 ? guiH - HUD_H - 4 : 4;
        return new int[] {x, y};
    }
}
