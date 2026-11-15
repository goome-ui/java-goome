package io.github.goomeui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class T {
    static final Path ROOT = Paths.get(System.getProperty("goome.root", "..")).toAbsolutePath();

    private T() {
    }

    static byte[] data(String name) throws IOException {
        return Files.readAllBytes(ROOT.resolve("tests/data").resolve(name));
    }

    static int fnv(IntBuffer v, int w, int h, int stride) {
        int x = 0x811C9DC5;
        for (int y = 0; y < h; y++) {
            for (int k = 0; k < w; k++) {
                int p = v.get(y * stride + k);
                for (int s = 0; s < 32; s += 8) x = (x ^ ((p >>> s) & 255)) * 16777619;
            }
        }
        return x;
    }

    static int fnv(int[] px) {
        return fnv(IntBuffer.wrap(px), px.length, 1, px.length);
    }

    static String hex(int v) {
        return String.format("%08x", v);
    }

    static boolean zeroCols(IntBuffer v, int stride, int h, int x0, int x1) {
        for (int y = 0; y < h; y++) for (int x = x0; x < x1; x++) if (v.get(y * stride + x) != 0) return false;
        return true;
    }

    static Path tmp(String name) throws IOException {
        Path d = Paths.get(System.getProperty("java.io.tmpdir")).resolve("goome-tests");
        Files.createDirectories(d);
        return d.resolve(name);
    }

    static String compiler() {
        String cc = System.getenv("CC");
        List<String> tries = new ArrayList<>();
        if (cc != null && !cc.isEmpty()) tries.add(cc);
        tries.add("cc");
        tries.add("gcc");
        tries.add("clang");
        for (String c : tries) {
            try {
                Process p = new ProcessBuilder(c, "--version").redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0) return c;
            } catch (IOException | InterruptedException e) {
                continue;
            }
        }
        return null;
    }

    static String runC(String src, String... extra) throws IOException, InterruptedException {
        String cc = compiler();
        if (cc == null) {
            if (System.getenv("GOOME_STRICT") != null) throw new AssertionError("no C compiler to check against");
            return null;
        }
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        Path exe = tmp(Paths.get(src).getFileName().toString().replace(".c", win ? ".exe" : ""));
        List<String> cmd = new ArrayList<>(List.of(cc, "-std=c99", "-O1", "-I" + ROOT.resolve("native"), "-o", exe.toString(), ROOT.resolve(src).toString()));
        cmd.addAll(List.of(extra));
        if (!win && !System.getProperty("os.name").toLowerCase().contains("mac")) cmd.add("-lm");
        run(cmd);
        return run(List.of(exe.toString()));
    }

    private static String run(List<String> cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out;
        try (InputStream in = p.getInputStream()) {
            out = new String(in.readAllBytes());
        }
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) throw new AssertionError(String.join(" ", cmd) + " failed:\n" + out);
        return out;
    }
}
