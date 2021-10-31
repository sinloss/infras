package com.sinlo.sponte.util;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A thread safe instance pool
 *
 * @author sinlo
 */
public class Pool<K, T> {

    private final Map<K, T> pool;

    public Pool() {
        this(new ConcurrentHashMap<>());
    }

    public Pool(Map<K, T> pool) {
        this.pool = pool;
    }

    /**
     * Just get
     */
    public T get(K key) {
        return pool.get(key);
    }

    /**
     * Get and take away
     */
    public T take(K key) {
        return pool.remove(key);
    }

    /**
     * On the given {@link Key key}, atomically do the given {@code action}
     *
     * @see #on(Key, BiFunction)
     */
    public T on(Key<K> key, BiConsumer<K, T> action) {
        return on(key, (k, t) -> {
            action.accept(k, t);
            return t;
        });
    }

    /**
     * On the given {@link Key key} and its corresponding value, atomically compute a new value.
     * If the computed value is null, the corresponding key will be removed
     *
     * @return the new value associated with the specified key, or null if none
     * @see ConcurrentHashMap#computeIfPresent(Object, BiFunction)
     * @see ConcurrentHashMap#computeIfAbsent(Object, Function)
     * @see ConcurrentHashMap#compute(Object, BiFunction)
     */
    public T on(Key<K> key, BiFunction<K, T, T> compute) {
        switch (key.existence) {
            case 0:
                return pool.computeIfAbsent(key.k, k -> compute.apply(k, null));
            case 1:
                return pool.computeIfPresent(key.k, compute);
            case 2:
                return pool.compute(key.k, compute);
        }
        // this should never happen
        return null;
    }

    /**
     * Get a {@link Collection} of entries in this pool
     */
    public Collection<Map.Entry<K, T>> entries() {
        return pool.entrySet();
    }

    /**
     * Get a {@link Collection} of keys in this pool
     */
    public Collection<K> keys() {
        return pool.keySet();
    }

    /**
     * Get a {@link Collection} of values in this pool
     */
    public Collection<T> values() {
        return pool.values();
    }

    /**
     * Get or supply if none exists
     */
    public T get(K key, Supplier<T> ifNone) {
        T t = pool.get(key);
        if (t == null) {
            if (pool.putIfAbsent(key, t = ifNone.get()) == null) {
                return t;
            }
            return pool.get(key);
        }
        return t;
    }

    /**
     * Place the given value into the pool no matter what state the key is in
     */
    public Pool<K, T> place(K key, T val) {
        on(Pool.Key.catstate(key), (k, t) -> val);
        return this;
    }

    /**
     * Purge
     */
    public void purge() {
        this.pool.clear();
    }

    /**
     * The key for {@link #on(Key, BiConsumer)} and {@link #on(Key, BiFunction)}
     *
     * @param <K> the type of the underlying {@link Key#k key}
     */
    public static class Key<K> {

        public final K k;

        public final int existence;

        private Key(K k, int existence) {
            this.k = k;
            this.existence = existence;
        }

        public boolean present() {
            return this.existence == 1;
        }

        public boolean absent() {
            return this.existence == 0;
        }

        public static <K> Key<K> present(K k) {
            return new Key<>(k, 1);
        }

        public static <K> Key<K> absent(K k) {
            return new Key<>(k, 0);
        }

        /**
         * Schrödinger's cat state of the given key's present or absent
         */
        public static <K> Key<K> catstate(K k) {
            return new Key<>(k, 2);
        }
    }

    /**
     * Simple string keyed {@link Pool}
     */
    public static class Simple<T> extends Pool<String, T> {

    }

    /**
     * A cache typed {@link Pool} which uses the {@link WeakHashMap} as its
     * underlying {@link #pool}
     */
    public static class Cache<K, T> extends Pool<K, T> {

        public Cache() {
            super(new WeakHashMap<>());
        }

        /**
         * Simple string keyed {@link Cache}
         */
        public static class Simple<T> extends Cache<String, T> {

        }
    }
}
