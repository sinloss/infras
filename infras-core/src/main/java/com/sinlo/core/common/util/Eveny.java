package com.sinlo.core.common.util;

import com.sinlo.sponte.util.Pool;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Eveny the event model
 * <br/><strike>Eveny is truly curious, sweet, caring...especially curious, curious about EVENTS</strike>
 *
 * @param <K> the event key type
 * @param <V> the event payload type
 * @author sinlo
 */
public class Eveny<K, V> {

    private final Pool<K, Map<Integer, Consumer<V>>> pool = new Pool<>();

    private Map<Integer, Consumer<V>> get(K k) {
        return pool.get(k, HashMap::new);
    }

    /**
     * register an event handler on event key [ k ]
     *
     * @param k       event key
     * @param handler event handler
     * @return a {@link Canceler} that holds the hash key relating to the given handler
     */
    public Canceler on(K k, Consumer<V> handler) {
        return put(k, handler.hashCode(), handler);
    }

    private Canceler put(K k, int hash, Consumer<V> handler) {
        pool.on(Pool.Key.catstate(k), (key, t) -> {
            (t == null ? (t = new HashMap<>()) : t).putIfAbsent(hash, handler);
            return t;
        });
        return this.new Canceler(hash, k);
    }

    /**
     * register an event handler on the event key [ k ], the handler
     * would be forgotten after the specific event with key [ k ] is
     * triggered, which means the given handler will be triggered
     * only once
     *
     * @see Eveny#on(Object, Consumer)
     */
    public Canceler once(K k, Consumer<V> handler) {
        // use the hash code of the given handler
        int hash = handler.hashCode();
        // wrap the given handler around
        Consumer<V> wrapped = v -> Eveny.this.forget(k, hash, h -> handler.accept(v));
        return put(k, hash, wrapped);
    }

    /**
     * Fire the event without any {@code executor}
     */
    public Eveny<K, V> fire(K k, V v) {
        fire(k, v, null);
        return this;
    }

    /**
     * Fire the event using the given {@code executor}, it is very useful when asynchronous
     * event handling is demanded
     */
    public Eveny<K, V> fire(K k, V v, Consumer<Runnable> executor) {
        pool.on(Pool.Key.present(k), (key, value) -> {
            value.values().forEach(executor == null
                    ? c -> c.accept(v)
                    : c -> executor.accept(() -> c.accept(v)));
        });
        return this;
    }

    /**
     * remove all handlers on the event key [ k ]
     *
     * @param k    the event key
     * @param then handle the removed handlers
     * @see Pool#on(Pool.Key, BiFunction)
     */
    public Eveny<K, V> forget(K k, Consumer<Collection<Consumer<V>>> then) {
        pool.on(Pool.Key.present(k), (key, t) -> {
            if (then != null) {
                then.accept(t.values());
            }
            // return null to remove the value associated with the given key
            return null;
        });
        return this;
    }

    /**
     * remove the specific handler on the event key [ k ] with hash key [ hash ]
     *
     * @param k    the event key
     * @param hash the hash key
     * @param then handle the remove handler
     * @see Pool#on(Pool.Key, BiConsumer)
     */
    public Eveny<K, V> forget(K k, int hash, Consumer<Consumer<V>> then) {
        pool.on(Pool.Key.present(k), (key, value) -> {
            if (then != null) {
                then.accept(value.remove(hash));
            } else {
                value.remove(hash);
            }
        });
        return this;
    }

    /**
     * Instances of this class would hold the event {@link #k key} and the {@link #hash} key
     * relating to a specific handler. So that it can easily cancel the related handler
     */
    public class Canceler {

        public final int hash;
        public final K k;

        private Canceler(int hash, K k) {
            this.hash = hash;
            this.k = k;
        }

        /**
         * Cancel the handler related to this canceler
         */
        public void cancel(Consumer<Consumer<V>> then) {
            Eveny.this.forget(k, hash, then);
        }

        /**
         * Cancel without a further handler
         *
         * @see #cancel(Consumer)
         */
        public void cancel() {
            cancel(null);
        }

        /**
         * Get a {@link Siblings} that represents all siblings of this canceler
         */
        public Siblings siblings() {
            return this.new Siblings();
        }


        /**
         * Representing all siblings of a specific canceller
         */
        public class Siblings {

            private Siblings() {
            }

            /**
             * Cancel all the handlers registered under the same key with the {@link Canceler}
             */
            public void cancel(Consumer<Collection<Consumer<V>>> then) {
                Eveny.this.forget(Canceler.this.k, then);
            }

            /**
             * Cancel without a further handler
             */
            public void cancel() {
                cancel(null);
            }
        }
    }

}
