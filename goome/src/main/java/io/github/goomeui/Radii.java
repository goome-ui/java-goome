package io.github.goomeui;

public final class Radii {
    private final double tl, tr, br, bl;

    public Radii(double tl, double tr, double br, double bl) {
        this.tl = tl;
        this.tr = tr;
        this.br = br;
        this.bl = bl;
    }

    public static Radii of(double r) {
        return new Radii(r, r, r, r);
    }

    public double tl() {
        return tl;
    }

    public double tr() {
        return tr;
    }

    public double br() {
        return br;
    }

    public double bl() {
        return bl;
    }

    @Override
    public String toString() {
        return "Radii(" + tl + ", " + tr + ", " + br + ", " + bl + ")";
    }
}
