package io.github.goomeui;

import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.file.Path;

final class FfmNative extends Native {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final MemorySegment ALL = MemorySegment.NULL.reinterpret(Long.MAX_VALUE);
    private static final MemoryLayout RADII = MemoryLayout.structLayout(JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE);
    private static final MemoryLayout CLONG = LINKER.canonicalLayouts().get("long");
    private static final MethodHandle TO_INT, TO_DOUBLE, TO_SEG, TO_RADII, FROM_DOUBLE, FROM_SEG, ZERO;
    private final MethodHandle calloc, free;
    private SymbolLookup lib;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            TO_INT = l.findStatic(FfmNative.class, "toInt", MethodType.methodType(int.class, long.class));
            TO_DOUBLE = l.findStatic(Double.class, "longBitsToDouble", MethodType.methodType(double.class, long.class));
            TO_SEG = l.findStatic(MemorySegment.class, "ofAddress", MethodType.methodType(MemorySegment.class, long.class));
            TO_RADII = l.findStatic(FfmNative.class, "radii", MethodType.methodType(MemorySegment.class, long.class));
            FROM_DOUBLE = l.findStatic(Double.class, "doubleToRawLongBits", MethodType.methodType(long.class, double.class));
            FROM_SEG = l.findVirtual(MemorySegment.class, "address", MethodType.methodType(long.class));
            ZERO = MethodHandles.constant(long.class, 0L);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    FfmNative() {
        SymbolLookup c = LINKER.defaultLookup();
        calloc = LINKER.downcallHandle(c.find("calloc").orElseThrow(), FunctionDescriptor.of(ADDRESS, JAVA_LONG, JAVA_LONG));
        free = LINKER.downcallHandle(c.find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
    }

    private static int toInt(long v) {
        return (int) v;
    }

    private static MemorySegment radii(long p) {
        return MemorySegment.ofAddress(p).reinterpret(RADII.byteSize());
    }

    @Override
    String kind() {
        return "ffm";
    }

    @Override
    void open(Path path) {
        lib = SymbolLookup.libraryLookup(path, Arena.global());
    }

    @Override
    long symbol(String name) {
        return lib.find(name).map(MemorySegment::address).orElse(0L);
    }

    private static MemoryLayout layout(char c) {
        switch (c) {
            case 'i': return JAVA_INT;
            case 'd': return JAVA_DOUBLE;
            case 'l': return CLONG;
            case 'z': return JAVA_LONG;
            case 'r': return RADII;
            default: return ADDRESS;
        }
    }

    private static MethodHandle filter(char c) {
        switch (c) {
            case 'i': return TO_INT;
            case 'd': return TO_DOUBLE;
            case 'l': return CLONG.byteSize() == 4 ? TO_INT : null;
            case 'z': return null;
            case 'r': return TO_RADII;
            default: return TO_SEG;
        }
    }

    @Override
    Object prepare(long address, String sig) {
        char ret = sig.charAt(0);
        String rest = sig.substring(1);
        int fixed = rest.indexOf('.');
        char[] args = rest.replace(".", "").toCharArray();
        MemoryLayout[] lay = new MemoryLayout[args.length];
        MethodHandle[] filters = new MethodHandle[args.length];
        for (int k = 0; k < args.length; k++) {
            lay[k] = layout(args[k]);
            filters[k] = filter(args[k]);
        }
        FunctionDescriptor fd = ret == 'v' ? FunctionDescriptor.ofVoid(lay) : FunctionDescriptor.of(layout(ret), lay);
        MethodHandle mh = fixed < 0
            ? LINKER.downcallHandle(MemorySegment.ofAddress(address), fd)
            : LINKER.downcallHandle(MemorySegment.ofAddress(address), fd, Linker.Option.firstVariadicArg(fixed));
        mh = MethodHandles.filterArguments(mh, 0, filters);
        switch (ret) {
            case 'v':
                mh = MethodHandles.collectArguments(ZERO, 0, mh);
                break;
            case 'd':
                mh = MethodHandles.filterReturnValue(mh, FROM_DOUBLE);
                break;
            case 'i':
            case 'l':
            case 'z':
                break;
            default:
                mh = MethodHandles.filterReturnValue(mh, FROM_SEG);
        }
        MethodType all = MethodType.methodType(long.class, java.util.Collections.nCopies(args.length, long.class));
        return mh.asType(all).asSpreader(long[].class, args.length);
    }

    @Override
    long invoke(Object handle, long[] a) {
        try {
            return (long) ((MethodHandle) handle).invokeExact(a);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    int longSize() {
        return (int) CLONG.byteSize();
    }

    @Override
    long calloc(long n) {
        long p;
        try {
            p = ((MemorySegment) calloc.invokeExact(1L, Math.max(1L, n))).address();
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (p == 0) throw new OutOfMemoryError("no native memory for " + n + " bytes");
        return p;
    }

    @Override
    void free(long p) {
        try {
            free.invokeExact(MemorySegment.ofAddress(p));
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    void zero(long p, long n) {
        ALL.asSlice(p, n).fill((byte) 0);
    }

    @Override
    byte getByte(long p) {
        return ALL.get(JAVA_BYTE, p);
    }

    @Override
    void putByte(long p, byte v) {
        ALL.set(JAVA_BYTE, p, v);
    }

    @Override
    int getInt(long p) {
        return ALL.get(JAVA_INT_UNALIGNED, p);
    }

    @Override
    void putInt(long p, int v) {
        ALL.set(JAVA_INT_UNALIGNED, p, v);
    }

    @Override
    long getLong(long p) {
        return ALL.get(JAVA_LONG_UNALIGNED, p);
    }

    @Override
    void putLong(long p, long v) {
        ALL.set(JAVA_LONG_UNALIGNED, p, v);
    }

    @Override
    double getDouble(long p) {
        return ALL.get(JAVA_DOUBLE_UNALIGNED, p);
    }

    @Override
    void putDouble(long p, double v) {
        ALL.set(JAVA_DOUBLE_UNALIGNED, p, v);
    }

    @Override
    void putFloat(long p, float v) {
        ALL.set(JAVA_FLOAT_UNALIGNED, p, v);
    }

    @Override
    ByteBuffer wrap(long p, long n) {
        return MemorySegment.ofAddress(p).reinterpret(n).asByteBuffer();
    }

    @Override
    long address(ByteBuffer direct) {
        return MemorySegment.ofBuffer(direct).address();
    }
}
