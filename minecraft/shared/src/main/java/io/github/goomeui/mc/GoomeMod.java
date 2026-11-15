package io.github.goomeui.mc;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class GoomeMod {
    public static final String ID = "goome";
    public static final Identifier CATEGORY = Identifier.fromNamespaceAndPath(ID, "goome");
    public static KeyMapping open, hud;
    public static final Menu MENU = new Menu();
    public static final GoomeHud OVERLAY = new GoomeHud(MENU);

    private GoomeMod() {
    }

    public static void setup(Path gameDir) {
        if (System.getProperty("goome.native") == null) System.setProperty("goome.native", "lwjgl");
        if (System.getProperty("goome.cache") == null) System.setProperty("goome.cache", gameDir.resolve("goome").toString());
    }

    public static KeyMapping[] keys(KeyMapping.Category category) {
        open = new KeyMapping("key.goome.open", InputConstants.KEY_G, category);
        hud = new KeyMapping("key.goome.hud", InputConstants.KEY_H, category);
        return new KeyMapping[] {open, hud};
    }

    public static void tick(Minecraft mc) {
        while (open != null && open.consumeClick()) {
            if (mc.gui.screen() == null) mc.gui.setScreen(new GoomeScreen(Component.literal("goome"), MENU::draw));
        }
        while (hud != null && hud.consumeClick()) MENU.hud = !MENU.hud;
    }
}
