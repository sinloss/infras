package com.sinlo.core.common.functional;

import com.sinlo.core.common.util.Try;

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
            return Try.toss(e);
        }
    }

}
