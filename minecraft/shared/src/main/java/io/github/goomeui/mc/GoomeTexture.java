package io.github.goomeui.mc;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.goomeui.Rect;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class GoomeTexture implements GoomeView.Sink, AutoCloseable {
    private static int next;
    private final Identifier id = Identifier.fromNamespaceAndPath("goome", "view/" + next++);
    private NativeImage image;
    private DynamicTexture tex;
    private int w, h;

    @Override
    public ByteBuffer pixels(int w, int h) {
        if (image == null || w != this.w || h != this.h) {
            close();
            image = new NativeImage(w, h, false);
            tex = new DynamicTexture(id::toString, image);
            Minecraft.getInstance().getTextureManager().register(id, tex);
            this.w = w;
            this.h = h;
        }
        return MemoryUtil.memByteBuffer(image.getPointer(), w * h * 4);
    }

    @Override
    public void uploaded(Rect dirty) {
        tex.upload();
    }

    public void draw(GuiGraphicsExtractor g, int x, int y, int guiW, int guiH) {
        if (tex == null) return;
        g.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, id, x, y, 0f, 0f, guiW, guiH, w, h, w, h);
    }

    @Override
    public void close() {
        if (tex != null) Minecraft.getInstance().getTextureManager().release(id);
        tex = null;
        image = null;
    }
}
