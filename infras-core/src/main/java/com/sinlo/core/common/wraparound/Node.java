package com.sinlo.core.common.wraparound;

import com.sinlo.core.common.util.Loki;

/**
 * Node the linkable node, it is not thread safe by default. If you want a thread safe
 * version, please consider {@link Atomic}
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

    private static <T extends Node<T>> Node<T> attach(Node<T> one, Node<T> another) {
        if (one != null) one.next = (T) another;
        if (another != null) another.prev = (T) one;
        return one;
    }

    /**
     * Join a given node
     *
     * @throws NotDetachedException when the given node still has links to other nodes
     */
    public T join(T node) throws NotDetachedException {
        if (node.prev != null || node.next != null) {
            throw new NotDetachedException();
        }
        attach(this, attach(node, next));
        return (T) this;
    }

    /**
     * After a given node
     *
     * @see #join(Node)
     */
    public T after(T node) throws NotDetachedException {
        node.join((T) this);
        return (T) this;
    }

    /**
     * Eject this node off the chain
     */
    public T eject() {
        attach(prev, next);
        prev = next = null;
        return (T) this;
    }

    /**
     * Swap this node with the given node, meaning the given node will take the
     * place of this node, and vice versa
     */
    public T swap(T node) {
        Node<T> p = node.prev;
        Node<T> n = node.next;
        attach(prev, attach(node, next));
        attach(p, attach(this, n));
        return (T) this;
    }

    /**
     * Get a downward {@link Iterational}. The {@link Iterational#iterator()} of this is not
     * thread safe, so please use this single threaded
     */
    public Iterational<T> downward() {
        return Iterational.of((T) this,
                (t, i) -> t != null && t != this,
                (t, i) -> t.next);
    }

    /**
     * Get an upward {@link Iterational}. The {@link Iterational#iterator()} of this is not
     * thread safe, so please use this single threaded
     */
    public Iterational<T> upward() {
        return Iterational.of((T) this,
                (t, i) -> t != null && t != this,
                (t, i) -> t.prev);
    }

    /**
     * Initialize a root node whose {@link #prev} will be the last node of all
     * joining nodes and {@link #next} the first
     */
    public static <T extends Node<T>> T rooted(T node) {
        return node.prev = node.next = node; // circulate
    }

    /**
     * The simplest subclass of {@link Node}
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

    /**
     * A thread safe subclass of {@link Node}. This approach tries to reduce the possibility
     * of lock contention throughout the entire node chain, and utilize the optimization of
     * {@code synchronized} keyword
     */
    public static class Atomic extends Node<Atomic> {

        /**
         * Left side direction lock, the {@link Atomic} object itself is used as
         * the right side direction lock
         */
        private final Object l = new Object();

        @Override
        public Atomic prev() {
            synchronized (l) {
                return super.prev();
            }
        }

        @Override
        public synchronized Atomic next() {
            return super.next();
        }

        /**
         * {@inheritDoc} atomically by locking the following node directions
         * <ul>
         *     <li>
         *         {@code this -->}
         *     </li>
         *     <li>
         *         {@code <-- node -->}
         *     </li>
         *     <li>
         *         {@code <-- this.next}
         *     </li>
         * </ul>
         */
        @Override
        public Atomic join(Atomic node) {
            return Loki.sequentially(() -> super.join(node),
                    this, next == null ? null : next.l, node, node.l);
        }

        /**
         * {@inheritDoc} atomically by locking the following node directions
         * <ul>
         *     <li>
         *         {@code this.prev -->}
         *     </li>
         *     <li>
         *         {@code <-- this -->}
         *     </li>
         *     <li>
         *         {@code <-- this.next}
         *     </li>
         * </ul>
         */
        @Override
        public Atomic eject() {
            return Loki.sequentially(super::eject,
                    prev, this, l, next == null ? null : next.l);
        }

        /**
         * Eject the given node then join it atomically by locking the following node directions
         * <ul>
         *     <li>
         *         {@code this -->}
         *     </li>
         *     <li>
         *         {@code <-- this.next}
         *     </li>
         *     <li>
         *         {@code node.prev -->}
         *     </li>
         *     <li>
         *         {@code <-- node -->}
         *     </li>
         *     <li>
         *         {@code <-- node.next}
         *     </li>
         * </ul>
         */
        public Atomic snatch(Atomic node) {
            return Loki.sequentially(() -> super.join(node.rawEject()),
                    this, next == null ? null : next.l,
                    node.prev, node, node.l, node.next == null ? null : node.next.l);
        }

        private Atomic rawEject() {
            return super.eject();
        }

        /**
         * {@inheritDoc} atomically by locking the following node directions
         * <ul>
         *     <li>
         *         {@code this.prev -->}
         *     </li>
         *     <li>
         *         {@code <-- node -->}
         *     </li>
         *     <li>
         *         {@code <-- this.next}
         *     </li>
         *     <li>
         *         {@code node.prev -->}
         *     </li>
         *     <li>
         *         {@code <-- this -->}
         *     </li>
         *     <li>
         *         {@code <-- node.next}
         *     </li>
         * </ul>
         */
        @Override
        public Atomic swap(Atomic node) {
            return Loki.sequentially(() -> super.swap(node),
                    prev, this, next == null ? null : next.l,
                    node.prev, node, node.l, node.next == null ? null : node.next.l);
        }

        /**
         * A subclass of {@link Atomic} with payload
         */
        public static class Payloaded<T> extends Atomic {

            public final T payload;

            public Payloaded(T payload) {
                this.payload = payload;
            }
        }
    }

    /**
     * @see Node#join(Node)
     */
    public static class NotDetachedException extends Exception {

        public NotDetachedException() {
            super("The given node is not a detached node, consider ejecting it first");
        }
    }
}
