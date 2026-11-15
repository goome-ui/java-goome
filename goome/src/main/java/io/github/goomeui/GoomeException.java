package io.github.goomeui;

public class GoomeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;

    public GoomeException(int code, String message) {
        super(message != null ? message : Goome.strerror(code));
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static final class NotSupported extends GoomeException {
        private static final long serialVersionUID = 1L;

        public NotSupported(String m) {
            super(Goome.ENOTSUP, m);
        }
    }

    public static final class Denied extends GoomeException {
        private static final long serialVersionUID = 1L;

        public Denied(String m) {
            super(Goome.EDENIED, m);
        }
    }

    public static final class NoDisplay extends GoomeException {
        private static final long serialVersionUID = 1L;

        public NoDisplay(String m) {
            super(Goome.ENODISPLAY, m);
        }
    }

    public static final class Gone extends GoomeException {
        private static final long serialVersionUID = 1L;

        public Gone(String m) {
            super(Goome.EGONE, m);
        }
    }

    public static final class Again extends GoomeException {
        private static final long serialVersionUID = 1L;

        public Again(String m) {
            super(Goome.EAGAIN, m);
        }
    }

    public static final class NoMemory extends GoomeException {
        private static final long serialVersionUID = 1L;

        public NoMemory(String m) {
            super(Goome.ENOMEM, m);
        }
    }

    public static final class BadArg extends GoomeException {
        private static final long serialVersionUID = 1L;

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
