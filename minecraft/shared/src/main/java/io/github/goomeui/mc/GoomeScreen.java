package io.github.goomeui.mc;

import io.github.goomeui.Goome;
import io.github.goomeui.Ui;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class GoomeScreen extends Screen {
    private final Consumer<Ui> body;
    private final GoomeTexture tex = new GoomeTexture();
    private GoomeView view;
    private boolean dirty = true;
    private long drawn;

    public GoomeScreen(Component title, Consumer<Ui> body) {
        super(title);
        this.body = body;
    }

    public GoomeView view() {
        return view;
    }

    @Override
    protected void init() {
        double s = minecraft.getWindow().getGuiScale();
        if (view == null) view = new GoomeView(width, height, s);
        else view.resize(width, height, s);
        dirty = true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        long now = Goome.nowMs();
        if (dirty || view.ui().animating() || now - drawn > 250) {
            dirty = false;
            drawn = now;
            view.frame(body);
            String copied = view.copied();
            if (copied != null) minecraft.keyboardHandler.setClipboard(copied);
            view.upload(tex);
        }
        tex.draw(g, 0, 0, width, height);
    }

    private boolean mark(boolean eaten) {
        dirty = true;
        return eaten;
    }

    @Override
    public void mouseMoved(double x, double y) {
        mark(view.mouseMoved(x, y));
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        return mark(view.mouseDown(e.x(), e.y(), e.button())) || super.mouseClicked(e, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        return mark(view.mouseUp(e.x(), e.y(), e.button())) || super.mouseReleased(e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return mark(view.mouseMoved(e.x(), e.y())) || super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        return mark(view.scroll(x, y, v)) || super.mouseScrolled(x, y, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int k = Keys.sdlScancode(e.key()), m = Keys.sdlMods(e.modifiers());
        if (e.isPaste()) view.pasteFrom(minecraft.keyboardHandler.getClipboard());
        boolean eaten = Keys.printable(k, m) ? view.wantsKeyboard() : view.key(k, m);
        return mark(eaten) || super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        return mark(view.text(e.codepointAsString(), 0)) || super.charTyped(e);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        tex.close();
        if (view != null) view.close();
        view = null;
        super.removed();
    }
}
