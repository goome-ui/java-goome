package io.github.goomeui.examples;

import io.github.goomeui.Goome;
import io.github.goomeui.GoomeException;
import io.github.goomeui.Image;
import io.github.goomeui.Rect;
import java.util.ArrayList;
import java.util.List;

public final class Grab {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: Grab [monitor] [x y w h] out.ppm");
            System.exit(2);
        }
        List<Integer> nums = new ArrayList<>();
        for (int k = 0; k < args.length - 1 && nums.size() < 5; k++) {
            try {
                nums.add(Integer.parseInt(args[k]));
            } catch (NumberFormatException e) {
                nums.add(0);
            }
        }
        Goome g;
        try {
            g = Goome.open("goome grab");
        } catch (GoomeException e) {
            System.err.println("cant open: " + e.getMessage());
            System.exit(1);
            return;
        }
        try (g) {
            int mon = nums.size() == 1 || nums.size() == 5 ? nums.get(0) : 0;
            int n = nums.size();
            Rect r = n >= 4 ? new Rect(nums.get(n - 4), nums.get(n - 3), nums.get(n - 2), nums.get(n - 1)) : null;
            Image img;
            try {
                img = g.captureWait(mon, r, 50, 100);
                img.savePpm(args[args.length - 1]);
            } catch (GoomeException e) {
                System.err.println("grab failed: " + Goome.strerror(e.code()) + " (" + g.error() + ")");
                System.exit(1);
                return;
            }
            Rect a = img.area;
            System.out.printf("%dx%d px of %d,%d %dx%d on monitor %d%s%s%n", img.w, img.h, a.x, a.y, a.w, a.h, mon,
                img.hasSelf ? ", has our overlay" : "", img.hasCursor ? ", has the cursor" : "");
        }
    }
}
