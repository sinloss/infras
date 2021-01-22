package com.sinlo.core.common.wraparound;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The fluent builder that can cascade setter calls
 *
 * @param <T> the item type
 * @author sinlo
 */
public class Cascader<T> {

    private final T t;

    private Cascader(T t) {
        this.t = t;
    }

    /**
     * Create a supplier for the given item {@code t}
     */
    public static <T> Cascader<T> of(T t) {
        return new Cascader<>(t);
    }

    /**
     * Create a {@link Cascader} out of a {@link Supplier}
     */
    public static <T> Cascader<T> of(Supplier<T> supplier) {
        return of(supplier.get());
    }

    /**
     * Cascade the given {@code oper} operation with a given argument {@code a}
     */
    public <A> Cascader<T> cscd(BiConsumer<T, A> oper, A a) {
        oper.accept(t, a);
        return this;
    }

    /**
     * Get the built item
     */
    public T get() {
        return t;
    }
}
