package com.sinlo.core.common.util;

import java.util.function.*;

/**
 * Funny the function util
 * <br/><strike>Funny is the most funny and functional name, ever!</strike>
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
     * A function that returns null
     */
    public static <T, R> R nil(T t) {
        return null;
    }

    /**
     * A function that returns true
     */
    public static <T> boolean aye(T t) {
        return true;
    }

    /**
     * A function that returns false
     */
    public static <T> boolean nay(T t) {
        return false;
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
     * Convert the given {@link Consumer} to a valid {@link Function} having no
     * return value. It is mainly used to meet the requirements of some methods
     * which only accepts {@link Function}s
     */
    public static <T> Function<T, Void> voided(Consumer<T> proc) {
        return (t) -> {
            proc.accept(t);
            return null;
        };
    }

    /**
     * Opposite to the {@link #voided(Consumer)}, this will convert the given
     * {@link Consumer} to a valid {@link Function} that returns the given
     * {@code next}
     */
    public static <T, A> Function<A, T> cascade(Consumer<A> voided, T next) {
        return a -> {
            voided.accept(a);
            return next;
        };
    }

    /**
     * An equivalent of the nvl function in Oracle/PLSQL
     */
    public static <T> T nvl(T nullable, Supplier<T> ifNull) {
        return nullable == null ? ifNull.get() : nullable;
    }

    /**
     * Similar to {@link #nvl(Object, Supplier)} but directly accept the fallback
     * value
     */
    public static <T> T nvl(T nullable, T fallback) {
        return nullable == null ? fallback : nullable;
    }
}
