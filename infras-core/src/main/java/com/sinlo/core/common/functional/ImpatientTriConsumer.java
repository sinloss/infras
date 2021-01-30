package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

/**
 * The impatient tri-consumer who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientTriConsumer<T, U, V, E extends Throwable> extends TriConsumer<T, U, V> {
    void consume(T t, U u, V v) throws E;

    default void accept(T t, U u, V v) {
        try {
            consume(t, u, v);
        } catch (Throwable e) {
            Try.toss(e);
        }
    }
}
