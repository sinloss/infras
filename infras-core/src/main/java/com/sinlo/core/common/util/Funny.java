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
     * {@link BiConsumer} to a valid {@link BiFunction} that returns the first
     * parameter. In some scenarios the given {@code voided} is a setter with
     * no return value, then the first parameter will be the instance to which
     * the setter belongs
     */
    public static <T, A> BiFunction<T, A, T> cascade(BiConsumer<T, A> voided) {
        return (t, a) -> {
            voided.accept(t, a);
            return t;
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
