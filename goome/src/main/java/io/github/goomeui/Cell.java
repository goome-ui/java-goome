package io.github.goomeui;

import static io.github.goomeui.Native.N;

import java.lang.ref.Cleaner;

final class Cell {
    private static final Cleaner CLEANER = Cleaner.create();

    final long p;

    Cell(Object owner, long size) {
        long q = N.calloc(size);
        p = q;
        CLEANER.register(owner, () -> N.free(q));
    }
}
