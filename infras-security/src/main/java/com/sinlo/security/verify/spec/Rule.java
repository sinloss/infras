package com.sinlo.security.verify.spec;

/**
 * The specification of the implementation of rules
 *
 * @author sinlo
 */
public interface Rule {

    /**
     * Should the given path be verified for tokens
     */
    boolean should(String path);
}
