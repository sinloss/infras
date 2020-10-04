package com.sinlo.core.domain.persistor.spec;

/**
 * The selector specification
 *
 * @author sinlo
 */
@FunctionalInterface
public interface Selector<T extends Entity> {

    @SuppressWarnings("rawtypes")
    Selector ZERO_VALUE = t -> null;

    Repo<T> select(T t);
}
