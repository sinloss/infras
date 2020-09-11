package com.sinlo.core.common;

import java.util.*;
import java.util.function.Consumer;

/**
 * Eventer the event model
 *
 * @param <K> the event key type
 * @param <V> the event payload type
 * @author sinlo
 */
public class Eventer<K, V> {

    private final Map<K, Map<Integer, Consumer<V>>> pool = new HashMap<>();

    private Map<Integer, Consumer<V>> get(K k) {
        return pool.computeIfAbsent(k, k1 -> new HashMap<>());
    }

    /**
     * register an event handler on event key [ k ]
     *
     * @param k       event key
     * @param handler event handler
     * @return the hash key relating to the given handler
     */
    public int on(K k, Consumer<V> handler) {
        get(k).put(handler.hashCode(), handler);
        return handler.hashCode();
    }

    /**
     * register an event handler on the event key [ k ], the handler
     * would be forgotten after the specific event with key [ k ] is
     * triggered, which means the given handler will be triggered
     * only once
     *
     * @see Eventer#on(Object, Consumer)
     */
    public int once(K k, Consumer<V> handler) {
        return on(k, v -> {
            synchronized (handler) {
                handler.accept(v);
                Eventer.this.forget(k);
            }
        });
    }

    public void fire(K k, V v) {
        get(k).values().forEach(c -> c.accept(v));
    }

    /**
     * remove all handlers on the event key [ k ]
     *
     * @param k the event key
     * @return the removed handlers
     */
    public Collection<Consumer<V>> forget(K k) {
        return pool.remove(k).values();
    }

    /**
     * remove the specific handler on the event key [ k ] with hash key [ hash ]
     *
     * @param k    the event key
     * @param hash the hash key
     * @return the removed handlers
     * @see Eventer#on(Object, Consumer)
     */
    public Consumer<V> forget(K k, int hash) {
        return get(k).remove(hash);
    }
}
