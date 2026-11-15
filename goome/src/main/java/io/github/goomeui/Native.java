package io.github.goomeui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class Native {
    static final Native N = start();

    abstract String kind();

    abstract void open(Path lib);

    abstract long symbol(String name);

    abstract Object prepare(long address, String sig);

    abstract long invoke(Object handle, long[] args);

    abstract int longSize();

    abstract long calloc(long n);

    abstract void free(long p);

    abstract void zero(long p, long n);

    abstract byte getByte(long p);

    abstract void putByte(long p, byte v);

    abstract int getInt(long p);

    abstract void putInt(long p, int v);

    abstract long getLong(long p);

    abstract void putLong(long p, long v);

    abstract double getDouble(long p);

    abstract void putDouble(long p, double v);

    abstract void putFloat(long p, float v);

    abstract ByteBuffer wrap(long p, long n);

    abstract long address(ByteBuffer direct);

    static long call(Fn f, long... a) {
        Object h = f.handle;
        if (h == null) {
            if (f.address == 0) throw new UnsupportedOperationException(f.symbol + " isnt in this goome library, its too old for that");
            h = f.handle = N.prepare(f.address, f.sig);
        }
        return N.invoke(h, a);
    }

    static int i(Fn f, long... a) {
        return (int) call(f, a);
    }

    static double d(Fn f, long... a) {
        return Double.longBitsToDouble(call(f, a));
    }

    static long b(double v) {
        return Double.doubleToRawLongBits(v);
    }

    static long b(boolean v) {
        return v ? 1 : 0;
    }

    static ByteBuffer view(long p, long n) {
        return N.wrap(p, n).order(ByteOrder.nativeOrder());
    }

    static String str(long p) {
        if (p == 0) return null;
        int n = 0;
        while (N.getByte(p + n) != 0) n++;
        return str(p, n);
    }

    static String str(long p, int n) {
        byte[] b = new byte[n];
        if (n > 0) view(p, n).get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    static String chars(long p, int cap) {
        int n = 0;
        while (n < cap && N.getByte(p + n) != 0) n++;
        return str(p, n);
    }

    static void put(long p, byte[] b) {
        if (b.length > 0) view(p, b.length).put(b);
    }

    private static Native start() {
        String want = setting("goome.native", "GOOME_JAVA_NATIVE", "auto").toLowerCase(Locale.ROOT);
        List<String> order = new ArrayList<>();
        if (want.equals("ffm") || want.equals("lwjgl")) {
            order.add(want);
        } else if (Runtime.version().feature() >= 22) {
            order.add("ffm");
            order.add("lwjgl");
        } else {
            order.add("lwjgl");
        }
        List<String> why = new ArrayList<>();
        Native n = null;
        for (String k : order) {
            try {
                n = make(k);
                break;
            } catch (Throwable t) {
                why.add(k + ": " + t);
            }
        }
        if (n == null) {
            String java = Runtime.version().feature() >= 22 ? "" : " goome needs Java 22+ for the built in path, or LWJGL 3.3+ on the classpath on older Java.";
            throw new UnsatisfiedLinkError("cant call into goome." + java + " tried " + String.join(", ", why));
        }
        Path lib = library();
        n.open(lib);
        List<String> missing = new ArrayList<>();
        for (Fn f : Fn.values()) {
            f.address = n.symbol(f.symbol);
            if (f.address == 0 && !f.optional) missing.add(f.symbol);
        }
        if (!missing.isEmpty()) throw new UnsatisfiedLinkError(lib + " is missing " + String.join(", ", missing) + ", its probably too old");
        return n;
    }

    private static Native make(String kind) throws ReflectiveOperationException {
        String cls = kind.equals("ffm") ? "io.github.goomeui.FfmNative" : "io.github.goomeui.LwjglNative";
        return (Native) Class.forName(cls).getDeclaredConstructor().newInstance();
    }

    static String setting(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v == null || v.isEmpty()) v = System.getenv(env);
        return v == null || v.isEmpty() ? def : v;
    }

    static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String o = os.contains("win") ? "windows" : os.contains("mac") || os.contains("darwin") ? "macos" : "linux";
        String a = arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : "x64";
        return o + "-" + a;
    }

    static String libName() {
        String p = platform();
        return p.startsWith("windows") ? "goome.dll" : p.startsWith("macos") ? "libgoome.dylib" : "libgoome.so";
    }

    private static Path library() {
        String forced = setting("goome.lib", "GOOME_LIB", null);
        if (forced != null) {
            Path p = Paths.get(forced).toAbsolutePath();
            if (!Files.isRegularFile(p)) throw new UnsatisfiedLinkError("goome.lib points at " + p + " but theres nothing there");
            return p;
        }
        String res = "/natives/" + platform() + "/" + libName();
        byte[] data;
        try (InputStream in = Native.class.getResourceAsStream(res)) {
            if (in == null) {
                throw new UnsatisfiedLinkError("theres no goome library for " + platform() + " in this jar (" + res
                    + "). build one with ./build.sh or point -Dgoome.lib at a libgoome you built");
            }
            data = in.readAllBytes();
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("cant read " + res + ": " + e);
        }
        Path dir = cache().resolve(hash(data));
        Path out = dir.resolve(libName());
        try {
            if (Files.isRegularFile(out) && Files.size(out) == data.length) return out;
            Files.createDirectories(dir);
            Path part = Files.createTempFile(dir, "goome", ".part");
            Files.write(part, data);
            try {
                Files.move(part, out, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(part, out, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Files.deleteIfExists(part);
                if (!Files.isRegularFile(out)) throw e;
            }
            return out;
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("cant unpack the goome library into " + dir + ": " + e + ". set -Dgoome.cache to a folder you can write to");
        }
    }

    private static Path cache() {
        String forced = setting("goome.cache", "GOOME_CACHE", null);
        if (forced != null) return Paths.get(forced);
        String home = System.getProperty("user.home", ".");
        String p = platform();
        if (p.startsWith("windows")) {
            String local = System.getenv("LOCALAPPDATA");
            return Paths.get(local != null ? local : home, "goome", "java");
        }
        if (p.startsWith("macos")) return Paths.get(home, "Library", "Caches", "goome", "java");
        String xdg = System.getenv("XDG_CACHE_HOME");
        return Paths.get(xdg != null && !xdg.isEmpty() ? xdg : home + "/.cache", "goome", "java");
    }

    private static String hash(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder s = new StringBuilder();
            for (int k = 0; k < 8; k++) s.append(String.format("%02x", d[k] & 255));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(java.util.Arrays.hashCode(data));
        }
    }
}
