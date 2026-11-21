package io.github.goomeui;

/** Thrown when goome says no. code() is one of the Goome error codes, and each code has its own subclass. */
public class GoomeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;

    /** An exception for an error code. */
    public GoomeException(int code, String message) {
        super(message != null ? message : Goome.strerror(code));
        this.code = code;
    }

    /** The Goome error code. */
    public int code() {
        return code;
    }

    /** This backend or os cant do that (ENOTSUP). */
    public static final class NotSupported extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public NotSupported(String m) {
            super(Goome.ENOTSUP, m);
        }
    }

    /** The os or the user said no (EDENIED). */
    public static final class Denied extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public Denied(String m) {
            super(Goome.EDENIED, m);
        }
    }

    /** No display to open on (ENODISPLAY). */
    public static final class NoDisplay extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public NoDisplay(String m) {
            super(Goome.ENODISPLAY, m);
        }
    }

    /** The thing is closed or gone (EGONE). */
    public static final class Gone extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public Gone(String m) {
            super(Goome.EGONE, m);
        }
    }

    /** Not ready yet, try again in a moment (EAGAIN). */
    public static final class Again extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public Again(String m) {
            super(Goome.EAGAIN, m);
        }
    }

    /** Out of memory (ENOMEM). */
    public static final class NoMemory extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public NoMemory(String m) {
            super(Goome.ENOMEM, m);
        }
    }

    /** A bad argument (EARG). */
    public static final class BadArg extends GoomeException {
        private static final long serialVersionUID = 1L;

        /** Makes the exception. */
        public BadArg(String m) {
            super(Goome.EARG, m);
        }
    }

    static GoomeException of(int code, String m) {
        switch (code) {
            case Goome.ENOTSUP: return new NotSupported(m);
            case Goome.EDENIED: return new Denied(m);
            case Goome.ENODISPLAY: return new NoDisplay(m);
            case Goome.EGONE: return new Gone(m);
            case Goome.EAGAIN: return new Again(m);
            case Goome.ENOMEM: return new NoMemory(m);
            case Goome.EARG: return new BadArg(m);
            default: return new GoomeException(code, m);
        }
    }

    static int check(int rc, String m) {
        if (rc < 0) throw of(rc, m != null ? m : Goome.strerror(rc));
        return rc;
    }
}
