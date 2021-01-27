package com.sinlo.security.jwt.spec.exception;

/**
 * The general exception for jwt
 *
 * @author sinlo
 */
public class JwtException extends RuntimeException {

    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
