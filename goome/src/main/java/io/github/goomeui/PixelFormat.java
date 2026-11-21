package io.github.goomeui;

/** Byte order of pixels when copying frames out. The non premultiplied ones unpremultiply on the way. */
public enum PixelFormat {
    /** Blue, green, red, alpha byte order, premultiplied. Matches the native frame. */
    BGRA_PREMUL,
    /** Red, green, blue, alpha byte order, premultiplied. */
    RGBA_PREMUL,
    /** Blue, green, red, alpha byte order, not premultiplied. */
    BGRA,
    /** Red, green, blue, alpha byte order, not premultiplied. */
    RGBA
}
