package com.sinlo.security.jwt.spec.exception;

import java.security.PrivateKey;

/**
 * Indicating the signing has failed
 *
 * @see com.sinlo.security.jwt.Jwter.Scheme#signer(PrivateKey)
 * @see com.sinlo.security.jwt.Jwter.Issuer#issue(String, Object, long, java.util.List)
 */
public class SigningFailedException extends JwtException {

    public SigningFailedException(Throwable cause) {
        super(String.format(
                "The signing failed on the cause of %s", cause), cause);
    }
}
