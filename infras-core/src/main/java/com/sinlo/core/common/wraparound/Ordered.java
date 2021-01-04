package com.sinlo.core.common.wraparound;

import java.util.List;

/**
 * An item with order that could be easily sorted
 *
 * @param <T>
 * @author sinlo
 */
public class Ordered<T> implements Comparable<Ordered<T>> {

    public final int order;
    public final T t;

    public Ordered(int order, T t) {
        this.order = order;
        this.t = t;
    }

    @Override
    public int compareTo(Ordered<T> o) {
        if (o == null) return 1;
        return this.order - o.order;
    }

    public int getOrder() {
        return order;
    }

    public T getT() {
        return t;
    }

    /**
     * Add an item to the last of the given list
     */
    public static <A> void last(List<Ordered<A>> ol, A a) {
        ol.add(new Ordered<>(Integer.MAX_VALUE, a));
    }

    /**
     * @see #add(List, Ordered)
     */
    public static <A> void add(List<Ordered<A>> ol, A a, int order) {
        add(ol, new Ordered<>(order, a));
    }

    /**
     * Add an ordered item to the given list
     */
    public static <A> void add(List<Ordered<A>> ol, Ordered<A> item) {
        int i = 0;
        for (Ordered<?> ordered : ol) {
            if (ordered.order > item.order) break;
            i++;
        }
        ol.add(i, item);
    }
}
