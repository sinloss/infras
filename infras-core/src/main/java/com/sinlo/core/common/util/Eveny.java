package com.sinlo.core.common.util;

import com.sinlo.sponte.util.Pool;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Eveny the event model
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
     * @return the hash key relating to the given handler
     */
    public int on(K k, Consumer<V> handler) {
        return put(k, handler.hashCode(), handler);
    }

    private int put(K k, int hash, Consumer<V> handler) {
        pool.on(Pool.Key.catstate(k), (key, t) -> {
            (t == null ? (t = new HashMap<>()) : t).putIfAbsent(hash, handler);
            return t;
        });
        return hash;
    }

    /**
     * register an event handler on the event key [ k ], the handler
     * would be forgotten after the specific event with key [ k ] is
     * triggered, which means the given handler will be triggered
     * only once
     *
     * @see Eveny#on(Object, Consumer)
     */
    public int once(K k, Consumer<V> handler) {
        // use the hash code of the given handler
        int hash = handler.hashCode();
        // wrap the given handler around
        Consumer<V> wrapped = v -> Eveny.this.forget(k, hash, h -> handler.accept(v));
        return put(k, hash, wrapped);
    }

    public void fire(K k, V v) {
        pool.on(Pool.Key.present(k), (key, value) -> {
            value.values().forEach(c -> c.accept(v));
        });
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
}
