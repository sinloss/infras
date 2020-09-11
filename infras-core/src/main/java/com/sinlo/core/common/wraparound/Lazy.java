package com.sinlo.core.common.wraparound;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Lazy the thread safe equivalent of kotlin's lazy
 *
 * @author sinlo
 */
public class Lazy<T> {

    private final AtomicReference<T> atomic = new AtomicReference<>();

    private final Supplier<T> initializer;

    public Lazy(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public T get() {
        T t = atomic.get();
        if (t == null) {
            if (atomic.compareAndSet(null, t = initializer.get())) {
                return t;
            }
            return atomic.get();
        }
        return t;
    }
}
