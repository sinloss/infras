package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

import java.util.function.BiConsumer;

/**
 * The impatient bi-consumer who throws the specific exception ASAP
 *
 * @author sinlo
 */
public interface ImpatientBiConsumer<T, U, E extends Throwable> extends BiConsumer<T, U> {

    void consume(T t, U u) throws E;

    default void accept(T t, U u) {
        try {
            consume(t, u);
        } catch (Throwable e) {
            Try.tolerate(e);
        }
    }
}
