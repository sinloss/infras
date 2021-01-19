package com.sinlo.core.common.util;

import com.sinlo.core.common.functional.ImpatientSupplier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The fluent style try
 *
 * @param <R> the return type
 * @param <E> the exception type
 * @author sinlo
 */
public class Try<R, E extends Throwable> {

    private final ImpatientSupplier<R, E> closure;

    private final Map<Class<? extends Throwable>, Function<Throwable, R>> fallbacks;

    private Function<Throwable, R> otherwise = Try::toss;

    private Try(ImpatientSupplier<R, E> closure) {
        this.closure = closure;
        this.fallbacks = new HashMap<>();
    }

    /**
     * Create a {@link Try} of the given closure
     */
    public static <R, E extends Throwable> Try<R, E> of(ImpatientSupplier<R, E> closure) {
        return new Try<>(closure);
    }

    /**
     * If any throwable of the given {@code types} has been caught
     */
    @SafeVarargs
    public final Caught caught(Class<? extends Throwable>... types) {
        return new Caught(types);
    }

    /**
     * Apply the given {@code otherwise} if none of the assigned type of throwable was caught.
     * It will sneaky throw the throwable by default
     */
    public Try<R, E> otherwise(Function<Throwable, R> fallback) {
        this.otherwise = Objects.requireNonNull(fallback);
        return this;
    }

    /**
     * Equivalent to <pre>{@code
     *      this.otherwise(Funny::toss);
     * }</pre>
     *
     * @see #otherwise(Function)
     * @see #toss(Throwable)
     */
    public Try<R, E> otherwiseThrow() {
        return otherwise(Try::toss);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.otherwise(Funny::nil);
     * }</pre>
     *
     * @see #otherwise(Function)
     * @see #toss(Throwable)
     */
    public Try<R, E> otherwiseNull() {
        return otherwise(Funny::nil);
    }

    /**
     * Give it a short
     */
    public R exert() {
        try {
            return this.closure.get();
        } catch (Throwable e) {
            // try to get a fallback matching the type of the caught throwable
            return match(e.getClass()).apply(e);
        }
    }

    private Function<Throwable, R> match(Class<? extends Throwable> type) {
        return fallbacks.entrySet().stream()
                .filter(entry -> entry.getKey().isAssignableFrom(type))
                .findFirst().map(Map.Entry::getValue).orElse(otherwise);
    }

    /**
     * Sneaky throw
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable, R> R toss(Throwable e) throws E {
        throw (E) e;
    }

    /**
     * Tolerate the given {@link Throwable} if it is not of {@link RuntimeException}
     */
    public static <E extends Throwable, R> R tolerate(Throwable e) {
        if (e instanceof RuntimeException)
            throw (RuntimeException) e;
        e.printStackTrace();
        return null;
    }

    /**
     * Leniently get supplies from the supplier that throws exceptions, meaning suppress the
     * underlying exceptions
     *
     * @see ImpatientSupplier#get()
     */
    public static <T, E extends Throwable> T tolerate(ImpatientSupplier<T, E> supplier) {
        return supplier.get();
    }

    /**
     * Similar to {@link #tolerate(ImpatientSupplier)} but throws the caught exceptions
     */
    public static <T, E extends Throwable> T panic(ImpatientSupplier<T, E> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            return toss(e);
        }
    }

    /**
     * A caught fallback builder
     */
    public class Caught {

        private final Class<? extends Throwable>[] types;

        private Caught(Class<? extends Throwable>[] types) {
            this.types = types;
        }

        /**
         * Associate the given {@code fallback} with the {@link #types} by putting them
         * into the {@link #fallbacks}
         */
        public Try<R, E> then(Function<Throwable, R> fallback) {
            Arrays.stream(types).forEach(t -> Try.this.fallbacks.put(t, fallback));
            return Try.this;
        }

        /**
         * Equivalent to <pre>{@code
         *      this.then(Funny::toss);
         * }</pre>
         *
         * @see #then(Function)
         * @see #toss(Throwable)
         */
        public Try<R, E> thenThrow() {
            return then(Try::toss);
        }

        /**
         * Equivalent to <pre>{@code
         *      this.then(Funny::nil);
         * }</pre>
         *
         * @see #then(Function)
         * @see #toss(Throwable)
         */
        public Try<R, E> thenNull() {
            return then(Funny::nil);
        }
    }
}