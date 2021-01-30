package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

import java.util.function.Supplier;

/**
 * The impatient supplier who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientSupplier<T, E extends Throwable> extends Supplier<T> {

    T supply() throws E;

    default T get() {
        try {
            return supply();
        } catch (Throwable e) {
            return Try.toss(e);
        }
    }
}
