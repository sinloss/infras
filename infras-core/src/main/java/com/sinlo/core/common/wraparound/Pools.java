package com.sinlo.core.common.wraparound;

import com.sinlo.core.common.util.Funny;
import com.sinlo.sponte.util.Pool;

import java.util.Objects;
import java.util.function.*;

/**
 * Collections of {@link Pool} variants
 *
 * @author sinlo
 */
public class Pools {

    /**
     * An expiring pool implementation of {@link Pool} based on the {@link Chan.Defer} to maintain
     * the expiring
     *
     * @see Pool
     */
    public static class Expiring<K, V> extends Pool<K, V> {

        private final Pool<K, It<V, Chan.Deferred<K>>> underlying;
        private final Chan.Defer<K> chan;

        private final Consumer<V> onExpired;
        private final long delay;
        private final boolean fixed;
        private static final ThreadLocal<Long> withed = new ThreadLocal<>();

        private Expiring(Consumer<V> onExpired, long delay, boolean fixed, long tick) {
            super(null);
            this.onExpired = onExpired;
            this.delay = delay;
            this.fixed = fixed;
            this.underlying = new Pool<>();
            this.chan = new Chan.Defer<>(this::expiring, tick);
        }

        public Expiring<K, V> start() {
            this.chan.polling();
            return this;
        }

        private long delay() {
            return Funny.nvl(withed.get(), () -> delay);
        }

        private V derive(It<V, Chan.Deferred<K>> it) {
            return it == null ? null : it.get();
        }

        private void refresh(Chan.Deferred<K> d) {
            d.update((at, delay) -> System.currentTimeMillis() + delay);
        }

        /**
         * <pre>{@code quiet(delay, fixed, 1L)}</pre>
         *
         * @see #quiet(long, boolean, long)
         */
        public static <K, V> Expiring<K, V> quiet(long delay, boolean fixed) {
            return quiet(delay, fixed, 1L);
        }

        /**
         * Create a quiet {@link Expiring} that expiring the items in the pool without
         * notifying others
         *
         * @param delay the default delay
         * @param fixed if the delay is fixed which means the item has no chance of postpone
         *              its expiring. If it is not fixed, on the other hand, it will refresh
         *              the delay every time it is being replaced
         * @param tick  how long does it take to tick once
         */
        public static <K, V> Expiring<K, V> quiet(long delay, boolean fixed, long tick) {
            return new Expiring<>(null, delay, fixed, tick);
        }

        /**
         * <pre>{@code perceptible(delay, fixed, onExpired, 1L)}</pre>
         *
         * @see #perceptible(long, boolean, Consumer, long)
         */
        public static <K, V> Expiring<K, V> perceptible(long delay, boolean fixed,
                                                        Consumer<V> onExpired) {
            return new Expiring<>(Objects.requireNonNull(onExpired), delay, fixed, 1L);
        }

        /**
         * Create a perceptible {@link Expiring} that expiring the items in the pool
         * and notify the provided {@link #onExpired}
         *
         * @see #quiet(long, boolean, long)
         */
        public static <K, V> Expiring<K, V> perceptible(long delay, boolean fixed,
                                                        Consumer<V> onExpired, long tick) {
            return new Expiring<>(Objects.requireNonNull(onExpired), delay, fixed, tick);
        }

        @Override
        public V get(K key) {
            return derive(underlying.get(key));
        }

        @Override
        public V take(K key) {
            return derive(underlying.take(key));
        }

        @Override
        public V on(Key<K> key, BiFunction<K, V, V> compute) {
            return derive(underlying.on(key, (k, it) -> {
                try {
                    V item = compute.apply(k, derive(it));
                    if (it == null) {
                        if (item != null) {
                            return It.self(item).with(chan.deferred(k, delay()));
                        }
                        return null;
                    }
                    if (item == null) {
                        // cancel the expiring if the value is about to be removed
                        // from the pool
                        it.sidecar().ifPresent(Chan.Deferred::cancel);
                        return null;
                    }
                    if (!fixed) {
                        // if not flagged as fixed and the returned item is not null,
                        // meaning that the value is about to be replaced in the pool
                        // then, fresh the delay
                        it.sidecar().ifPresent(this::refresh);
                    }
                } catch (AsIs ignored) {
                    // skip to here to keep "it" as is
                }
                return it;
            }));
        }

        /**
         * @InheritDoc
         */
        @Override
        public V get(K key, Supplier<V> ifNone) {
            return derive(underlying.get(key,
                    () -> It.self(ifNone.get()).with(chan.deferred(key, delay()))));
        }

        @Override
        public void purge() {
            underlying.purge();
        }

        /**
         * Apply the given {@code then} on this pool with a special {@code delay} that will
         * be chosen over the default {@link #delay}. The given {@code delay} will be discarded
         * once the {@code then} function is done
         */
        public <T> T with(long delay, Function<Expiring<K, V>, T> then) {
            withed.set(delay);
            try {
                return then.apply(this);
            } finally {
                withed.set(null);
            }
        }

        private boolean expiring(K k) {
            V expired = this.take(k);
            if (onExpired != null) onExpired.accept(expired);
            return true;
        }

        /**
         * Get the default {@link #delay}
         */
        public long defaultDelay() {
            return this.delay;
        }

        /**
         * Indicating that the item should be kept as is
         *
         * @see #on(Key, BiFunction)
         */
        public static class AsIs extends RuntimeException {
        }
    }
}
