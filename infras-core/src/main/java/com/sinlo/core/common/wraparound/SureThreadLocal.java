package com.sinlo.core.common.wraparound;

import com.sinlo.core.common.functional.ImpatientSupplier;

import java.util.Objects;
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
        local.set((this.supplier =
                Objects.requireNonNull(supplier)).get());
    }

    public static <T> SureThreadLocal<T> of(Supplier<T> supplier) {
        return new SureThreadLocal<>(supplier);
    }

    /**
     * Accept an initializer that may throw exceptions
     */
    public static <T, E extends Throwable> SureThreadLocal<T> of(ImpatientSupplier<T, E> supplier) {
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
