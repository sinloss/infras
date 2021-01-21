package com.sinlo.core.common.wraparound;

import com.sinlo.core.common.functional.ImpatientSupplier;
import com.sinlo.core.common.util.Try;

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

    public <E extends Throwable> Lazy(ImpatientSupplier<T, E> initializer) {
        this(Try.panicked(initializer));
    }

    public static <T> Lazy<T> of(Supplier<T> initializer) {
        return new Lazy<>(initializer);
    }

    /**
     * Accept an initializer that may throws exceptions
     */
    public static <T, E extends Throwable> Lazy<T> of(ImpatientSupplier<T, E> initializer) {
        return new Lazy<>(initializer);
    }

    /**
     * Get the item
     */
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

    /**
     * Make the current {@link Lazy} as a {@link Default} which could mutate the
     * underlying item
     */
    public Default asDefault() {
        return new Default();
    }

    /**
     * Create a new {@link Default} out of the current {@link Lazy} using the same
     * {@link #initializer}
     */
    public Default newDefault() {
        return of(initializer).new Default();
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
