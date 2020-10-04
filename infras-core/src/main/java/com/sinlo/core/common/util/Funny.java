package com.sinlo.core.common.util;

import java.util.function.Function;

/**
 * Funny the function util
 *
 * @author sinlo
 */
public class Funny {

    /**
     * An identity function {@code t -> t}
     */
    public static <T> T identity(T t) {
        return t;
    }

    /**
     * A maybe that treat null like an identity function does and treat presented
     * value with the given custom function
     */
    public static <T, R> R maybe(T t, Function<T, R> ifNotNull) {
        if (t == null) return null;
        return ifNotNull.apply(t);
    }
}
