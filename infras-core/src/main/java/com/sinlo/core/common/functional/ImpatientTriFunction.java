package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

/**
 * Similar to the {@link java.util.function.BiFunction} but accepts three arguments
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientTriFunction<T, U, V, R, E extends Throwable> extends TriFunction<T, U, V, R> {

    R employ(T t, U u, V v) throws E;

    default R apply(T t, U u, V v) {
        try {
            return employ(t, u, v);
        } catch (Throwable e) {
            return Try.toss(e);
        }
    }

}
