package com.sinlo.core.common.spec;

import java.util.function.BiFunction;

/**
 * The impatient bi-function who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientBiFunction<T, U, R, E extends Throwable> extends BiFunction<T, U, R> {

    R employ(T t, U u) throws E;

    default R apply(T t, U u) {
        try {
            return employ(t, u);
        } catch (Throwable e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            e.printStackTrace();
        }
        return null;
    }

}
