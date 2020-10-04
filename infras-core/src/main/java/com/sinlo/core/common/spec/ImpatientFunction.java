package com.sinlo.core.common.spec;

import java.util.function.Function;

/**
 * The impatient function who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientFunction<T, R, E extends Exception> extends Function<T, R> {

    R employ(T t) throws E;

    default R apply(T t) {
        try {
            return employ(t);
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            e.printStackTrace();
        }
        return null;
    }
}
