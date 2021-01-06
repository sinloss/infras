package com.sinlo.core.common.spec;

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
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            e.printStackTrace();
        }
        return null;
    }
}
