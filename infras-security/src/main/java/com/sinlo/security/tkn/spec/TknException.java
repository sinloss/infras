package com.sinlo.security.tkn.spec;

/**
 * The tkn exceptions
 *
 * @author sinlo
 */
public class TknException extends RuntimeException {

    public TknException(String message, Throwable cause) {
        super(message, cause);
    }

    public TknException(String message) {
        super(message);
    }

    public static class NoState extends TknException {
        public NoState() {
            super("Could not restore a valid state for the given tkn");
        }
    }

    public static class Null extends TknException {
        public Null() {
            super("The given tkn is null");
        }
    }

    public static class Expired extends TknException {
        public Expired() {
            super("The given tkn has expired");
        }
    }
}
