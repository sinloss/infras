package com.sinlo.core.common.wraparound;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Chan the abstract channel
 *
 * @param <T> the type of the item being hold in the {@link #q}
 */
public abstract class Chan<T, R> {

    public static final ScheduledExecutorService EX =
            Executors.newScheduledThreadPool(
                    Runtime.getRuntime().availableProcessors());

    /**
     * The underlying {@link Queue}
     */
    protected final Queue<T> q;
    /**
     * Handler that handles items in the {@link #q}
     */
    private final Function<T, Boolean> handler;

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

    public Chan(Function<T, Boolean> handler) {
        this(handler, 1);
    }

    public Chan(Function<T, Boolean> handler, long interval) {
        this.q = create();
        this.handler = Objects.requireNonNull(
                handler, "The consumer must not be null");
        this.interval = interval <= 0 ? 1 : interval;
    }

    /**
     * @see Queue#offer(Object)
     */
    public R offer(T t) {
        return ret(q.offer(t), t);
    }

    /**
     * Start polling
     */
    public void polling() {
        if (polling.compareAndSet(false, true)) {
            this.future = execute(this::consume);
        }
    }

    public void halt(boolean interrupt) {
        if (polling.compareAndSet(true, false)) {
            this.future.cancel(interrupt);
            this.future = null;
            this.q.clear();
        }
    }

    protected void consume() {
        T item = this.q.peek();
        if (item == null) {
            ifNone();
            return;
        }
        try {
            if (handler.apply(item)) {
                // successfully handled
                this.q.poll();
            }
        } catch (Interrupt e) {
            this.halt(true);
        }
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
     * This produces the needed return type {@link R} for {@link #offer(Object)}
     */
    protected abstract R ret(boolean succeeded, T t);

    /**
     * This specifies how the {@link #handler} is being executed
     */
    protected Future<?> execute(Runnable command) {
        return EX.scheduleAtFixedRate(command, 0, interval, TimeUnit.MILLISECONDS);
    }

    /**
     * Called when polled nothing
     */
    @SuppressWarnings("EmptyMethod")
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
    public static class Interval<T> extends Chan<T, Boolean> {

        public Interval(Function<T, Boolean> handler, long interval) {
            super(handler, interval);
        }

        @Override
        protected Queue<T> create() {
            return new ConcurrentLinkedQueue<>();
        }

        @Override
        protected Boolean ret(boolean succeeded, T t) {
            return succeeded;
        }
    }

    /**
     * A {@link Chan} implementation that uses {@link DelayQueue} as the underlying
     * {@link #q}
     */
    public static class Defer<T> extends Chan<Deferred<T>, Deferred<T>> {

        /**
         * @see #Defer(Function, long)
         */
        public Defer(Function<T, Boolean> handler) {
            this(handler, 1);
        }

        /**
         * Constructor
         *
         * @param handler the global fallback consumer for all {@link Deferred} items
         * @param tick    the polling ratio
         */
        public Defer(Function<T, Boolean> handler, long tick) {
            super(d -> d.accomplish(Objects.requireNonNull(
                    handler, "The handler must not be null")), tick);
        }

        /**
         * Offers a {@link Deferred} item with the given {@code delay}
         *
         * @see #offer(Object)
         */
        public Deferred<T> deferred(T payload, long delay) {
            return this.offer(Deferred.just(payload, delay));
        }

        @Override
        protected Queue<Deferred<T>> create() {
            return new DelayQueue<>();
        }

        @Override
        protected Deferred<T> ret(boolean succeeded, Deferred<T> deferred) {
            return succeeded ? deferred : null;
        }

    }

    /**
     * The {@link Delayed} item to be hold in the {@link Defer}
     */
    public static class Deferred<T> implements Delayed {

        /**
         * The underlying payload
         */
        public final T payload;
        /**
         * The defined delay
         */
        public final long delay;
        /**
         * The specific task
         */
        public final Function<T, Boolean> task;
        /**
         * The calculated time at which the delay will be over in milliseconds
         */
        private Long at;
        /**
         * The state indicating if this {@link Deferred} is finished. There should not
         * be any more task calling after this has been set to true
         */
        private final AtomicBoolean finished;

        private Deferred(T payload, long delay, Function<T, Boolean> task) {
            this.payload = payload;
            this.task = task;
            this.at = System.currentTimeMillis() + (this.delay = delay);
            this.finished = new AtomicBoolean(false);
        }

        /**
         * Create an instance of {@link Deferred}
         *
         * @param payload the payload
         * @param delay   the delay in milliseconds
         * @param task    the task to be accomplished
         * @param <T>     the type of payload
         * @return a {@link Deferred} instance
         */
        public static <T> Deferred<T> of(T payload, long delay, Function<T, Boolean> task) {
            return new Deferred<>(payload, delay, task);
        }

        /**
         * Create an of {@link Deferred} without a task
         *
         * @see #of(Object, long, Function)
         */
        public static <T> Deferred<T> just(T payload, long delay) {
            return of(payload, delay, null);
        }

        /**
         * Update the delay
         */
        public Deferred<T> update(BiFunction<Long, Long, Long> calc) {
            this.at = calc.apply(at, delay);
            return this;
        }

        /**
         * Cancel this deferred
         */
        public Deferred<T> cancel() {
            this.finished.compareAndSet(false, true);
            return this;
        }

        /**
         * Accomplish the underlying task
         *
         * @param fallback if no task is specified, call the fallback
         */
        public boolean accomplish(Function<T, Boolean> fallback) {
            if (!this.finished.compareAndSet(false, true))
                return true;
            if (this.task != null) {
                return task.apply(payload);
            } else if (fallback != null) {
                return fallback.apply(payload);
            }
            return true;
        }

        /**
         * @InheritDoc
         */
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(
                    this.at - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * @InheritDoc
         */
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
