package com.sinlo.core.common.wraparound;

import com.sinlo.core.common.util.Jason;

import java.util.Map;
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
     * Get an immutable view of it
     */
    public View<T, Sidecar> view() {
        return new View<>(t, sidecar);
    }

    /**
     * Get a map representation of it with the given aliases of itself and
     * its sidecar
     *
     * @param nameIt      to name {@link T itself}
     * @param nameSidecar to name the {@link Sidecar sidecar}
     * @return a map with aliases
     */
    public Map<?, ?> alias(String nameIt, String nameSidecar) {
        return Jason.map()
                .val(nameIt, t)
                .val(nameSidecar, sidecar);
    }

    /**
     * Mutate the item
     *
     * @param selfMutation the mutate function
     */
    public It<T, Sidecar> mutate(Function<T, T> selfMutation) {
        return mutate(selfMutation.apply(this.t));
    }

    /**
     * Mutate the item with a direct value
     */
    public It<T, Sidecar> mutate(T direct) {
        this.t = Objects.requireNonNull(direct);
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
     * Mutate the sidecar with a direct value
     */
    public It<T, Sidecar> sidecarMutate(Sidecar direct) {
        this.sidecar = direct;
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

    /**
     * An immutable view of {@link It}
     */
    public static class View<T, S> {

        public final T it;

        public final S sidecar;

        public View(T it, S sidecar) {
            this.it = it;
            this.sidecar = sidecar;
        }

        public T getIt() {
            return it;
        }

        public S getSidecar() {
            return sidecar;
        }
    }
}
