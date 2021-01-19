package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

import java.util.function.Function;

/**
 * The impatient function who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientFunction<T, R, E extends Throwable> extends Function<T, R> {

    R employ(T t) throws E;

    default R apply(T t) {
        try {
            return employ(t);
        } catch (Throwable e) {
            return Try.tolerate(e);
        }
    }
}
