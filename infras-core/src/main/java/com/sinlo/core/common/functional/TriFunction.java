package com.sinlo.core.common.functional;

import java.util.Objects;
import java.util.function.Function;

/**
 * Similar to the {@link java.util.function.BiFunction} but accepts three arguments
 *
 * @author sinlo
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {

    R apply(T t, U u, V v);

    /**
     * @see java.util.function.BiFunction#andThen(Function)
     */
    default <W> TriFunction<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
        Objects.requireNonNull(after);
        return (T t, U u, V v) -> after.apply(apply(t, u, v));
    }
}
