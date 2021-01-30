package com.sinlo.core.common.functional;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Similar to the {@link java.util.function.BiConsumer} but accepts three arguments
 *
 * @author sinlo
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);

    /**
     * @see java.util.function.BiConsumer#andThen(BiConsumer)
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (t, u, v) -> {
            accept(t, u, v);
            after.accept(t, u, v);
        };
    }
}
