package com.sinlo.security.jwt.spec.exception;

/**
 * Indicating the decoding has failed
 *
 * @see com.sinlo.security.jwt.Jwter.Scheme#decode(String)
 */
public class DecodingFailedException extends JwtException {

    public DecodingFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DecodingFailedException malformed(String what, Throwable cause) {
        return new DecodingFailedException(String.format(
                "The decoding failed with malformed [ %s ] on the cause of %s", what, cause), cause);
    }

    public static DecodingFailedException other(Throwable cause) {
        return new DecodingFailedException(String.format(
                "The decoding failed on the cause of %s", cause), cause);
    }
}
