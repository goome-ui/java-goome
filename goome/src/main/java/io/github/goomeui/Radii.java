package io.github.goomeui;

/** A corner radius for each corner. */
public final class Radii {
    private final double tl, tr, br, bl;

    /** Top left, top right, bottom right, bottom left. */
    public Radii(double tl, double tr, double br, double bl) {
        this.tl = tl;
        this.tr = tr;
        this.br = br;
        this.bl = bl;
    }

    /** The same radius on all four corners. */
    public static Radii of(double r) {
        return new Radii(r, r, r, r);
    }

    /** Top left radius. */
    public double tl() {
        return tl;
    }

    /** Top right radius. */
    public double tr() {
        return tr;
    }

    /** Bottom right radius. */
    public double br() {
        return br;
    }

    /** Bottom left radius. */
    public double bl() {
        return bl;
    }

    @Override
    public String toString() {
        return "Radii(" + tl + ", " + tr + ", " + br + ", " + bl + ")";
    }
}
