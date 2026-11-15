package io.github.goomeui.mc.neoforge;

import io.github.goomeui.mc.GoomeMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GoomeMod.ID, dist = Dist.CLIENT)
public final class GoomeNeoForge {
    public GoomeNeoForge(IEventBus bus) {
        GoomeMod.setup(FMLPaths.GAMEDIR.get());
        bus.addListener(GoomeNeoForge::keys);
        bus.addListener(GoomeNeoForge::layers);
        NeoForge.EVENT_BUS.addListener(GoomeNeoForge::tick);
    }

    private static void keys(RegisterKeyMappingsEvent e) {
        KeyMapping.Category category = new KeyMapping.Category(GoomeMod.CATEGORY);
        e.registerCategory(category);
        for (KeyMapping k : GoomeMod.keys(category)) e.register(k);
    }

    private static void layers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(Identifier.fromNamespaceAndPath(GoomeMod.ID, "hud"), (g, delta) -> GoomeMod.OVERLAY.render(g));
    }

    private static void tick(ClientTickEvent.Post e) {
        GoomeMod.tick(Minecraft.getInstance());
    }
}
