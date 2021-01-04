package com.sinlo.core.http.spec;

/**
 * The result produced by the interceptors that represents the next action
 *
 * @author sinlo
 */
public enum Next {
    RETRY, HALT, CONTINUE

}
