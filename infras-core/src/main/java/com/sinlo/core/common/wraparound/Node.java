package com.sinlo.core.common.wraparound;

/**
 * Node the linkable node
 *
 * @param <T> subclass type
 */
@SuppressWarnings("unchecked")
public class Node<T extends Node<T>> {

    protected T next;

    protected T prev;

    public T prev() {
        return prev;
    }

    public T next() {
        return next;
    }

    /**
     * Join a given node
     */
    public T join(T node) {
        if (next != null) next.prev = node;
        node.next = next;
        (node.prev = (T) this).next = node;
        return (T) this;
    }

    /**
     * After a given node
     */
    public T after(T node) {
        node.join((T) this);
        return (T) this;
    }

    /**
     * Eject this node off the chain
     */
    public T eject() {
        if (prev != null) prev.next = next;
        if (next != null) next.prev = prev;
        prev = next = null;
        return (T) this;
    }

    /**
     * Swap this node with the given node, meaning the given node will take the
     * place of this node, and vice versa
     */
    public T swap(T node) {
        T p = prev;
        T n = next;
        prev = node.prev;
        next = node.next;
        node.prev = p;
        node.next = n;
        return (T) this;
    }

    /**
     * Initialize a root node whose {@link #prev} will be the last node of all
     * joining nodes and {@link #next} the first
     */
    public static <T extends Node<T>> T rooted(T node) {
        return node.prev = node.next = node; // circulate
    }

    /**
     * A simplest subclass of {@link Node}
     */
    public static class Simple extends Node<Simple> {
    }

    /**
     * A subclass of {@link Node} with payload
     */
    public static class Payloaded<T> extends Node<Payloaded<T>> {

        public final T payload;

        public Payloaded(T payload) {
            this.payload = payload;
        }
    }

}
