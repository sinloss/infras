package com.sinlo.security.jwt.spec.exception;

import com.nimbusds.jose.Algorithm;

/**
 * Indicating the related jwt is having a bad structure
 *
 * @author sinlo
 */
public class BadJwtException extends JwtException {
    public BadJwtException(String message) {
        super(message);
    }

    public BadJwtException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The algorithm is not supported
     *
     * @param algorithm the unsupported algorithm
     */
    public static BadJwtException unsupported(Algorithm algorithm) {
        return new BadJwtException(
                String.format("Unsupported algorithm [ %s ]", algorithm));
    }

    /**
     * The Jwt is bad because an error has occurred
     *
     * @param cause the error that caused this
     */
    public static BadJwtException whileParsing(Throwable cause) {
        return new BadJwtException(String.format(
                "An error has occurred while parsing the Jwt: %s", cause.getMessage()), cause);
    }
}
