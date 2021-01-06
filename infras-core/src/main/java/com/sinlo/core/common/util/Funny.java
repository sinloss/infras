package com.sinlo.core.common.util;

import com.sinlo.core.common.spec.ImpatientSupplier;

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

    /**
     * Leniently get supplies from the supplier that throws exceptions, meaning suppress the
     * underlying exceptions
     *
     * @see ImpatientSupplier#get()
     */
    public static <T, E extends Throwable> T leniently(ImpatientSupplier<T, E> supplier) {
        return supplier.get();
    }
}
