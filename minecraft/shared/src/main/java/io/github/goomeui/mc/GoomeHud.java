package io.github.goomeui.mc;

import io.github.goomeui.Goome;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

public final class GoomeHud {
    private final Menu menu;
    private final GoomeTexture tex = new GoomeTexture();
    private GoomeView view;
    private double scale;
    private long drawn;

    public GoomeHud(Menu menu) {
        this.menu = menu;
    }

    public void render(GuiGraphicsExtractor g) {
        if (!menu.hud) return;
        Minecraft mc = Minecraft.getInstance();
        double s = mc.getWindow().getGuiScale();
        if (view == null) {
            view = new GoomeView(Menu.HUD_W, Menu.HUD_H, s);
            scale = s;
        } else if (s != scale) {
            view.resize(Menu.HUD_W, Menu.HUD_H, s);
            scale = s;
        }
        long now = Goome.nowMs();
        if (now - drawn >= 100) {
            drawn = now;
            view.frame(u -> menu.hud(u, () -> where(mc)));
            view.upload(tex);
        }
        int[] at = menu.hudSpot(g.guiWidth(), g.guiHeight());
        tex.draw(g, at[0], at[1], Menu.HUD_W, Menu.HUD_H);
    }

    static String where(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return "not in a world";
        return String.format(Locale.ROOT, "x %.1f  y %.1f  z %.1f", p.getX(), p.getY(), p.getZ());
    }
}
