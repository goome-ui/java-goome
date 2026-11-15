package io.github.goomeui.examples;

import io.github.goomeui.Event;
import io.github.goomeui.Goome;
import io.github.goomeui.Image;
import io.github.goomeui.Ui;
import io.github.goomeui.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class Offscreen {
    static void script(Goome g, int frame) {
        switch (frame) {
            case 2: g.push(Event.down(40, 109, 1)); g.push(Event.up(40, 109, 1)); break;
            case 3: g.push(Event.down(60, 163, 1)); break;
            case 4: g.push(Event.motion(250, 163)); break;
            case 5: g.push(Event.up(250, 163, 1)); break;
            case 6: g.push(Event.down(150, 223, 1)); g.push(Event.up(150, 223, 1)); break;
            case 7: g.pushText(" from an engine"); break;
            case 8: g.push(Event.motion(80, 279)); break;
            default:
        }
    }

    static void writePng(String path, Image f) throws IOException {
        BufferedImage out = new BufferedImage(f.w, f.h, BufferedImage.TYPE_INT_RGB);
        int[] bgs = {0xFF2E3C4A, 0xFF3A4A5A};
        int[] px = f.copy();
        for (int y = 0; y < f.h; y++) {
            for (int x = 0; x < f.w; x++) {
                int s = px[y * f.w + x], a = s >>> 24, bg = bgs[(x / 16 + y / 16) & 1];
                int r = (s >> 16 & 255) + (bg >> 16 & 255) * (255 - a) / 255;
                int gr = (s >> 8 & 255) + (bg >> 8 & 255) * (255 - a) / 255;
                int b = (s & 255) + (bg & 255) * (255 - a) / 255;
                out.setRGB(x, y, r << 16 | gr << 8 | b);
            }
        }
        ImageIO.write(out, "png", new File(path));
    }

    public static void main(String[] args) throws IOException {
        String path = args.length > 0 ? args[0] : "offscreen.png";
        try (Goome g = Goome.openOffscreen(360, 330, 2); Ui ui = g.ui()) {
            ui.theme().animMs(0);
            Window win = new Window(20, 20, 320, 0);
            boolean check = false;
            double volume = 0.25;
            String name = "goome";
            for (int frame = 0; frame < 10; frame++) {
                script(g, frame);
                for (Event e : g.events(0)) ui.feed(e);
                if (ui.begin(0) == null) continue;
                if (ui.window("offscreen", win, Ui.WIN_MOVE)) {
                    ui.label("no window, the engine owns the screen");
                    check = ui.checkbox("Pushed a click", check);
                    volume = ui.slider("Volume", volume, 0, 1, "%.2f");
                    name = ui.input("Name", name, 64);
                    ui.progress(volume);
                    ui.button("Hover me");
                }
                ui.windowEnd();
                ui.end();
            }
            writePng(path, g.lastFrame());
            System.out.printf("check %d volume %.2f name '%s' cursor %s -> %s%n", check ? 1 : 0, volume, name, Goome.CURSOR_NAMES.get(g.cursor()), path);
        }
    }
}
