package com.sinlo.security.jwt.spec;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specification of the private key and the public key
 *
 * @author sinlo
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Keys {

    String pri();

    String pub();
}
