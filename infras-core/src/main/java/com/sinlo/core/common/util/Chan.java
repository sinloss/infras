package com.sinlo.core.common.util;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Chan the abstract channel
 *
 * @param <T> the type of the item being hold in the {@link #q}
 */
public abstract class Chan<T> {

    public static final ScheduledExecutorService EX =
            Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors());

    /**
     * The underlying {@link Queue}
     */
    protected final Queue<T> q;
    /**
     * Consumer that consumes items in the {@link #q} by returning true
     */
    private final Function<T, Boolean> consumer;

    /**
     * The polling interval
     */
    private final long interval;

    /**
     * The {@link Future} yielded by the {@link #execute(Runnable)}
     */
    private Future<?> future;

    /**
     * Indicates that if the channel is currently polling
     */
    private final AtomicBoolean polling = new AtomicBoolean(false);

    public Chan(Function<T, Boolean> consumer) {
        this(consumer, 1);
    }

    public Chan(Function<T, Boolean> consumer, long interval) {
        this.q = create();
        this.consumer = consumer;
        this.interval = interval <= 0 ? 1 : interval;
    }

    /**
     * @see Queue#offer(Object)
     */
    public boolean offer(T t) {
        return q.offer(t);
    }

    /**
     * Start polling
     */
    public Chan<T> polling() {
        if (polling.compareAndSet(false, true)) {
            this.future = execute(this::consume);
        }
        return this;
    }

    public Chan<T> halt(boolean interrupt) {
        if (polling.compareAndSet(true, false)) {
            this.future.cancel(interrupt);
            this.future = null;
            this.q.clear();
        }
        return this;
    }

    protected boolean consume() {
        T item = this.q.peek();
        if (item == null) {
            ifNone();
            return false;
        }
        try {
            if (consumer.apply(item)) {
                this.q.poll();
            }
        } catch (Interrupt e) {
            this.halt(true);
            return false;
        }
        return true;
    }

    public int size() {
        return this.q.size();
    }

    /**
     * The supplier of the {@link #q}, which specifies the type of the {@link Queue}
     * being used by this channel
     */
    protected abstract Queue<T> create();

    /**
     * This specifies how the {@link #consumer} is being executed
     */
    protected Future<?> execute(Runnable command) {
        return EX.scheduleAtFixedRate(command, 0, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * Called when polled nothing
     */
    protected void ifNone() {

    }

    /**
     * The exception that can interrupt the {@link #polling()}
     */
    public static final class Interrupt extends RuntimeException {

    }

    /**
     * A {@link Chan} implementation that executes the consuming process at a regular
     * interval
     */
    public static class Interval<T> extends Chan<T> {

        public Interval(Function<T, Boolean> consumer, long interval) {
            super(consumer, interval);
        }

        @Override
        protected Queue<T> create() {
            return new ConcurrentLinkedQueue<>();
        }
    }

    /**
     * A {@link Chan} implementation that uses {@link DelayQueue} as the underlying
     * {@link #q}
     */
    public static class Defer<T> extends Chan<Deferred<T>> {

        public Defer(Function<T, Boolean> consumer) {
            super(d -> consumer.apply(d.t));
        }

        /**
         * Offers a {@link Deferred} item with the given {@code delay}
         *
         * @see #offer(Object)
         */
        public boolean deferred(T t, long delay) {
            return this.offer(new Deferred<>(t, delay));
        }

        /**
         * Same as {@link #deferred(Object, long)} but calculate the {@code delay} using the
         * given {@link Function} which takes the current queue size as the parameter and
         * returns the calculated delay
         */
        public boolean deferred(T t, Function<Integer, Long> delay) {
            return this.offer(new Deferred<>(t,
                    delay.apply(q.size())));
        }

        @Override
        protected Queue<Deferred<T>> create() {
            return new DelayQueue<>();
        }

    }

    /**
     * The {@link Delayed} item to be hold in the {@link Defer}
     */
    public static class Deferred<T> implements Delayed {

        /**
         * The underlying object
         */
        public final T t;
        private final Long at;

        public Deferred(T t, long delay) {
            this.t = t;
            this.at = System.currentTimeMillis() + delay;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(
                    this.at - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (o instanceof Deferred) {
                return this.at.compareTo(((Deferred<?>) o).at);
            }
            return Long.compare(this.getDelay(TimeUnit.MILLISECONDS),
                    o.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
