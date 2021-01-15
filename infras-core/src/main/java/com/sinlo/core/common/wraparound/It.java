package com.sinlo.core.common.wraparound;

import java.util.Objects;
import java.util.Optional;

/**
 * A simple container of the target object itself with a sidecar
 */
public class It<T, Sidecar> {

    public final T t;
    public final Sidecar sidecar;

    private It(T t, Sidecar sidecar) {
        this.t = t;
        this.sidecar = sidecar;
    }

    /**
     * Get the item
     */
    public T get() {
        return t;
    }

    /**
     * Try to get the sidecar
     */
    public Optional<Sidecar> sidecar() {
        return Optional.ofNullable(sidecar);
    }

    /**
     * New self ready for a new sidecar
     */
    public Self<T> self() {
        return self(t);
    }

    /**
     * Get a fluent builder
     */
    public static <T> Self<T> self(T t) {
        return new Self<>(Objects.requireNonNull(t));
    }

    /**
     * The fluent builder
     */
    public static class Self<T> {

        private final T t;

        private Self(T t) {
            this.t = t;
        }

        /**
         * Build a {@link It} with the given {@link Sidecar}
         */
        public <Sidecar> It<T, Sidecar> with(Sidecar sidecar) {
            return new It<>(t, sidecar);
        }

        public <Sidecar> It<T, Sidecar> withNothing() {
            return new It<>(t, null);
        }
    }
}
