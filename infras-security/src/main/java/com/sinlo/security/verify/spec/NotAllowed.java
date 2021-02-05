package com.sinlo.security.verify.spec;

/**
 * The requested resource is not accessible
 *
 * @author sinlo
 */
public class NotAllowed extends Exception {

    public NotAllowed(String path) {
        super(String.format(
                "The requested [ %s ] is not allowed for the current subject", path));
    }
}
