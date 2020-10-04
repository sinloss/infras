package com.sinlo.core.common.spec;

/**
 * The impatient bi-function who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public interface ImpatientBiFunction<T, U, R, E extends Exception> {

    R apply(T t, U u) throws E;
}
