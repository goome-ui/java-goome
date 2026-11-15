package io.github.goomeui;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.CLONG_SIZE;
import static org.lwjgl.system.libffi.LibFFI.*;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.FFIType;

final class LwjglNative extends Native {
    private static final FFIType RADII = radii();
    private SharedLibrary lib;

    private static final class Cif {
        final FFICIF cif;
        final long fn;
        final char ret;
        final char[] args;

        Cif(FFICIF cif, long fn, char ret, char[] args) {
            this.cif = cif;
            this.fn = fn;
            this.ret = ret;
            this.args = args;
        }
    }

    private static FFIType radii() {
        PointerBuffer el = memAllocPointer(5);
        for (int k = 0; k < 4; k++) el.put(k, ffi_type_double);
        el.put(4, NULL);
        return FFIType.calloc().type(FFI_TYPE_STRUCT).elements(el);
    }

    @Override
    String kind() {
        return "lwjgl";
    }

    @Override
    void open(Path path) {
        lib = Library.loadNative(LwjglNative.class, "io.github.goomeui", path.toString());
    }

    @Override
    long symbol(String name) {
        return lib.getFunctionAddress(name);
    }

    private FFIType type(char c) {
        switch (c) {
            case 'v': return ffi_type_void;
            case 'i': return ffi_type_sint32;
            case 'd': return ffi_type_double;
            case 'l': return ffi_type_slong;
            case 'z': return ffi_type_uint64;
            case 'r': return RADII;
            default: return ffi_type_pointer;
        }
    }

    @Override
    Object prepare(long address, String sig) {
        char ret = sig.charAt(0);
        String rest = sig.substring(1);
        int fixed = rest.indexOf('.');
        char[] args = rest.replace(".", "").toCharArray();
        PointerBuffer types = memAllocPointer(Math.max(1, args.length));
        for (int k = 0; k < args.length; k++) types.put(k, type(args[k]));
        FFICIF cif = FFICIF.calloc();
        int rc = fixed < 0
            ? ffi_prep_cif(cif, FFI_DEFAULT_ABI, type(ret), args.length == 0 ? null : types)
            : ffi_prep_cif_var(cif, FFI_DEFAULT_ABI, fixed, type(ret), types);
        if (rc != FFI_OK) throw new IllegalStateException("libffi cant set up a call for " + sig + " (" + rc + ")");
        return new Cif(cif, address, ret, args);
    }

    @Override
    long invoke(Object handle, long[] a) {
        Cif c = (Cif) handle;
        int n = c.args.length;
        if (a.length != n) throw new IllegalArgumentException("wanted " + n + " args, got " + a.length);
        try (MemoryStack s = MemoryStack.stackPush()) {
            long vals = s.nmalloc(8, Math.max(1, n) * 8);
            long ptrs = s.nmalloc(8, Math.max(1, n) * 8);
            long rv = s.ncalloc(8, 2, 8);
            for (int k = 0; k < n; k++) {
                long slot = vals + 8L * k;
                switch (c.args[k]) {
                    case 'r':
                        memPutAddress(ptrs + 8L * k, a[k]);
                        continue;
                    case 'i':
                        memPutInt(slot, (int) a[k]);
                        break;
                    case 'l':
                        if (CLONG_SIZE == 4) memPutInt(slot, (int) a[k]);
                        else memPutLong(slot, a[k]);
                        break;
                    default:
                        memPutLong(slot, a[k]);
                }
                memPutAddress(ptrs + 8L * k, slot);
            }
            nffi_call(c.cif.address(), c.fn, rv, ptrs);
            switch (c.ret) {
                case 'v': return 0;
                case 'i': return memGetInt(rv);
                case 'l': return CLONG_SIZE == 4 ? memGetInt(rv) : memGetLong(rv);
                default: return memGetLong(rv);
            }
        }
    }

    @Override
    int longSize() {
        return CLONG_SIZE;
    }

    @Override
    long calloc(long n) {
        long p = nmemCalloc(1, Math.max(1, n));
        if (p == NULL) throw new OutOfMemoryError("no native memory for " + n + " bytes");
        return p;
    }

    @Override
    void free(long p) {
        nmemFree(p);
    }

    @Override
    void zero(long p, long n) {
        memSet(p, 0, n);
    }

    @Override
    byte getByte(long p) {
        return memGetByte(p);
    }

    @Override
    void putByte(long p, byte v) {
        memPutByte(p, v);
    }

    @Override
    int getInt(long p) {
        return memGetInt(p);
    }

    @Override
    void putInt(long p, int v) {
        memPutInt(p, v);
    }

    @Override
    long getLong(long p) {
        return memGetLong(p);
    }

    @Override
    void putLong(long p, long v) {
        memPutLong(p, v);
    }

    @Override
    double getDouble(long p) {
        return memGetDouble(p);
    }

    @Override
    void putDouble(long p, double v) {
        memPutDouble(p, v);
    }

    @Override
    void putFloat(long p, float v) {
        memPutFloat(p, v);
    }

    @Override
    ByteBuffer wrap(long p, long n) {
        if (n > Integer.MAX_VALUE) throw new IllegalArgumentException("thats more than 2 GB, cant wrap it in one buffer");
        return memByteBuffer(p, (int) n);
    }

    @Override
    long address(ByteBuffer direct) {
        return memAddress(direct);
    }
}
