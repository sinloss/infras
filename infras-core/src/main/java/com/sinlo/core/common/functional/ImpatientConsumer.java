package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

import java.util.function.Consumer;

/**
 * The impatient consumer who throws the specific exception ASAP
 *
 * @author sinlo
 */
public interface ImpatientConsumer<T, E extends Throwable> extends Consumer<T> {

    void consume(T t) throws E;

    default void accept(T t) {
        try {
            consume(t);
        } catch (Throwable e) {
            Try.toss(e);
        }
    }
}
