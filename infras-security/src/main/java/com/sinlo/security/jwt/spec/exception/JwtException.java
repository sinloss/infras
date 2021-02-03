package com.sinlo.security.jwt.spec.exception;

import com.sinlo.security.tkn.spec.TknException;

/**
 * The general exception for jwt
 *
 * @author sinlo
 */
public class JwtException extends TknException {

    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
