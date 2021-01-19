package com.sinlo.core.common.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * A function that returns null
     */
    public static <T, R> R nil(T t) {
        return null;
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
     * An equivalent of the nvl function in Oracle/PLSQL
     */
    public static <T> T nvl(T nullable, Supplier<T> ifNull) {
        return nullable == null ? ifNull.get() : nullable;
    }

}
