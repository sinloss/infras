package com.sinlo.core.common.wraparound;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A simple container of the target object itself with a sidecar
 */
public class It<T, Sidecar> {

    private T t;
    private Sidecar sidecar;

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
     * Mutate the item
     *
     * @param selfMutation the mutate function
     */
    public It<T, Sidecar> mutate(Function<T, T> selfMutation) {
        this.t = Objects.requireNonNull(selfMutation.apply(this.t));
        return this;
    }

    /**
     * Mutate the sidecar
     *
     * @param sidecarMutation the mutate function
     */
    public It<T, Sidecar> sidecarMutate(Function<Sidecar, Sidecar> sidecarMutation) {
        this.sidecar = sidecarMutation.apply(sidecar);
        return this;
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
     * Create it without any {@link #sidecar}
     */
    public static <T> It<T, Void> just(T t) {
        return self(t).without();
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
         * Build {@link It} with the given {@link Sidecar}
         */
        public <Sidecar> It<T, Sidecar> with(Sidecar sidecar) {
            return new It<>(t, sidecar);
        }

        /**
         * Build {@link It} with anything
         */
        public It<T, Void> without() {
            return new It<>(t, null);
        }
    }
}
