package com.sinlo.core.domain.spec;

@FunctionalInterface
public interface Selector<T extends Entity> {

    @SuppressWarnings("rawtypes")
    Selector ZERO_VALUE = t -> null;

    Repo<T> select(T t);
}
