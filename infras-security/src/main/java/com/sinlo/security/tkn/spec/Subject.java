package com.sinlo.security.tkn.spec;

/**
 * The abstraction of subjects
 *
 * @author sinlo
 */
public interface Subject {

    /**
     * Check if the current subject is allowed to access the given path
     */
    default boolean allow(String path) {
        return true;
    }
}
