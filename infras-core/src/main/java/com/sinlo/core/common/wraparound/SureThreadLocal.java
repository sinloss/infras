package com.sinlo.core.common.wraparound;

import java.util.function.Supplier;

/**
 * The wraparound of the ThreadLocal, whose get method is never null
 *
 * @author sinlo
 */
public class SureThreadLocal<T> {

    private final ThreadLocal<T> local = new ThreadLocal<>();
    private final Supplier<T> supplier;

    private SureThreadLocal(Supplier<T> supplier) {
        local.set((this.supplier = supplier).get());
    }

    public static <T> SureThreadLocal<T> of(Supplier<T> supplier) {
        if (supplier == null)
            throw new IllegalArgumentException("The [ supplier ] is mandatory");
        return new SureThreadLocal<>(supplier);
    }

    public T get() {
        T t = local.get();
        if (t == null) local.set(t = this.supplier.get());
        return t;
    }

    public SureThreadLocal<T> set(T t) {
        local.set(t);
        return this;
    }

    public SureThreadLocal<T> clear() {
        local.set(null);
        return this;
    }

}
