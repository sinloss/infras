package com.sinlo.core.common.spec;

/**
 * The impatient tri-consumer who throws the specific exception ASAP
 *
 * @author sinlo
 */
@FunctionalInterface
public
interface ImpatientTriConsumer<T, U, V, E extends Exception> {
    void accept(T t, U u, V v) throws E;
}
