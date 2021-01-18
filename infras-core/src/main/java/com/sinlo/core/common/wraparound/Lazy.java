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

    public Default asDefault() {
        return new Default();
    }

    /**
     * A class that changes the concept the supplier of the {@link Lazy} to the
     * default supplier, which means if not value is provided, the {@link #get()}
     * would return the value from the supplier, otherwise the provided value
     */
    public class Default {

        public Default provide(T t) {
            atomic.set(t);
            return this;
        }

        public T get() {
            return Lazy.this.get();
        }
    }
}
