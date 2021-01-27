package com.sinlo.core.common.util;

import com.sinlo.core.common.functional.ImpatientRunnable;
import com.sinlo.core.common.functional.ImpatientSupplier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Create a {@link Try} of the given {@link ImpatientSupplier} closure
     */
    public static <R, E extends Throwable> Try<R, E> of(ImpatientSupplier<R, E> closure) {
        return new Try<>(closure);
    }

    /**
     * Create a {@link Try} of the given {@link ImpatientRunnable} closure
     */
    public static <E extends Throwable> Try<Void, E> of(ImpatientRunnable<E> closure) {
        return new Try<>(() -> {
            closure.runs();
            return null;
        });
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
     * Easily set the direct value
     */
    public Try<R, E> otherwise(R value) {
        return this.otherwise(e -> value);
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
     * Otherwise map the {@link Throwable} to a new one, and then throw it
     *
     * @see #toss(Throwable)
     */
    public Try<R, E> otherwiseThrow(Function<Throwable, Throwable> mapper) {
        return otherwise(e -> Try.toss(mapper.apply(e)));
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
            return this.closure.supply();
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

        public Try<R, E> then(R value) {
            return then(e -> value);
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
         * Map the caught {@link Throwable} to a new one, and then throw it
         *
         * @see #toss(Throwable)
         */
        public Try<R, E> thenThrow(Function<Throwable, Throwable> mapper) {
            return then(e -> Try.toss(mapper.apply(e)));
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
        try {
            return supplier.supply();
        } catch (Throwable e) {
            return tolerate(e);
        }
    }

    /**
     * Leniently run the runnable that throws exceptions, meaning suppress the underlying
     * exceptions
     *
     * @see ImpatientSupplier#get()
     */
    public static <E extends Throwable> void tolerate(ImpatientRunnable<E> runnable) {
        try {
            runnable.runs();
        } catch (Throwable e) {
            tolerate(e);
        }
    }

    /**
     * Similar to {@link #tolerate(ImpatientSupplier)} but returns a {@link Supplier}
     * instead of getting a supply from it
     */
    public static <T, E extends Throwable> Supplier<T> tolerated(ImpatientSupplier<T, E> supplier) {
        return supplier;
    }

    /**
     * Similar to {@link #tolerate(ImpatientRunnable)} but returns a {@link Runnable}
     * instead of running it
     */
    public static <E extends Throwable> Runnable tolerated(ImpatientRunnable<E> runnable) {
        return runnable;
    }

    /**
     * Similar to {@link #tolerate(ImpatientSupplier)} but throws the caught exceptions
     */
    public static <T, E extends Throwable> T panic(ImpatientSupplier<T, E> supplier) {
        try {
            return supplier.supply();
        } catch (Throwable e) {
            return toss(e);
        }
    }

    /**
     * Similar to {@link #tolerate(ImpatientRunnable)} but throws the caught exceptions
     *
     * @see ImpatientSupplier#get()
     */
    public static <E extends Throwable> void panic(ImpatientRunnable<E> runnable) {
        try {
            runnable.runs();
        } catch (Throwable e) {
            toss(e);
        }
    }

    /**
     * Similar to {@link #tolerated(ImpatientSupplier)} but throws the caught exceptions
     */
    public static <T, E extends Throwable> Supplier<T> panicked(ImpatientSupplier<T, E> supplier) {
        return () -> panic(supplier);
    }

    /**
     * Similar to {@link #tolerated(ImpatientRunnable)} but throws the caught exceptions
     */
    public static <E extends Throwable> Runnable panicked(ImpatientRunnable<E> runnable) {
        return () -> panic(runnable);
    }

    /**
     * Capture any of the {@link Throwable} thrown and return it
     */
    public static <T, E extends Throwable> Throwable capture(ImpatientSupplier<T, E> supplier) {
        try {
            supplier.supply();
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    /**
     * @see #capture(ImpatientSupplier)
     */
    public static <E extends Throwable> Throwable capture(ImpatientRunnable<E> runnable) {
        return capture(() -> {
            runnable.runs();
            return null;
        });
    }

    /**
     * Similar to {@link #capture(ImpatientSupplier)} but returns a {@link Supplier}
     * instead of getting a supply from it
     */
    public static <T, E extends Throwable> Supplier<Throwable> captured(ImpatientSupplier<T, E> supplier) {
        return () -> capture(supplier);
    }

    /**
     * Similar to {@link #capture(ImpatientRunnable)} but returns a {@link Runnable}
     * instead of running it
     */
    public static <E extends Throwable> Supplier<Throwable> captured(ImpatientRunnable<E> runnable) {
        return () -> capture(runnable);
    }

}
