package io.github.goomeui.mc.fabric;

import io.github.goomeui.mc.GoomeMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class GoomeFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GoomeMod.setup(FabricLoader.getInstance().getGameDir());
        KeyMapping.Category category = KeyMapping.Category.register(GoomeMod.CATEGORY);
        for (KeyMapping k : GoomeMod.keys(category)) KeyMappingHelper.registerKeyMapping(k);
        ClientTickEvents.END_CLIENT_TICK.register(GoomeMod::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GoomeMod.ID, "hud"), (g, delta) -> GoomeMod.OVERLAY.render(g));
    }
}
