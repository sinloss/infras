package com.sinlo.core.timewheel.spec;

import com.sinlo.core.common.wraparound.Node;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Cylinder the cylinder
 *
 * @author sinlo
 */
@SuppressWarnings("UnusedReturnValue")
public class Cylinder<T extends Cylinder.EntryAware<T>> implements Delayed {

    private final AtomicInteger tally;

    private final Entry<T> root = Node.rooted(new Entry<>(null, -1));

    private final AtomicLong expiration = new AtomicLong(-1L);

    public Cylinder(AtomicInteger tally) {
        this.tally = tally;
    }

    /**
     * traverse all deferred objects and consume them
     *
     * @param f consumer
     */
    public Cylinder<T> foreach(Consumer<T> f) {
        if (f != null) {
            synchronized (this) {
                for (Entry<T> entry = root.next(); entry != root && entry != null; entry = entry.next()) {
                    if (!entry.cancelled()) f.accept(entry.deferred);
                }
            }
        }
        return this;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public Cylinder<T> add(Entry<T> entry) {
        while (true) {
            entry.remove(); // 尝试从另一个Circle中移除该entry
            synchronized (this) {
                synchronized (entry) {
                    if (entry.cylinder == null) {
                        root.after(entry); // entry作为root的前驱节点，也即链表的末端
                        entry.cylinder = this;
                        tally.incrementAndGet();
                        break;
                    }
                }
            }
        }
        return this;
    }

    /**
     * 移除一个延迟任务条目
     *
     * @param entry 延迟任务条目
     * @return Circle自身，用于链式调用
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public Cylinder<T> remove(Entry<T> entry) {
        if (entry != null) {
            synchronized (this) {
                synchronized (entry) {
                    if (entry.cylinder == this) {
                        entry.eject();
                        tally.decrementAndGet();
                    }
                }
            }
        }
        return this;
    }

    /**
     * 清除所有延迟任务条目，并对每个被删除的条目应用消费函数
     *
     * @param f 消费函数
     * @return Cylinder自身，用于链式调用
     */
    public Cylinder<T> purge(Consumer<Entry<T>> f) {
        if (f != null) {
            synchronized (this) {
                for (Entry<T> entry = root.next(); entry != root && entry != null; entry = root.next()) {
                    f.accept(entry);
                    this.remove(entry);
                }
                expiration.set(-1L);
            }
        }
        return this;
    }

    public long getExpiration() {
        return expiration.get();
    }

    public boolean setExpiration(long expiration) {
        return this.expiration.getAndSet(expiration) != expiration;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(
                Math.max(this.getExpiration() - Ticker.nanoedMillis(), 0L),
                TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(Delayed o) {
        if (o instanceof Cylinder) {
            return this.getExpiration() > ((Cylinder<T>) o).getExpiration() ? 1 : -1;
        }
        return 0;
    }

    @SuppressWarnings("UnusedReturnValue")
    static class Entry<T extends EntryAware<T>> extends Node<Entry<T>> implements Comparable<Entry<T>> {

        public final T deferred;

        public final long expir;

        private Cylinder<T> cylinder;

        public Entry(T deferred, long expir) {
            this.deferred = deferred;
            this.expir = expir;
            if (this.deferred != null) this.deferred.setEntry(this);
        }

        public boolean cancelled() {
            return !this.equals(deferred.getEntry());
        }

        public void remove() {
            Cylinder<T> c = cylinder;
            while (c != null) {
                c.remove(this);
                c = cylinder;
            }
        }


        @Override
        public int compareTo(Entry o) {
            return (int) (this.expir - (o == null ? -1 : o.expir));
        }
    }

    public interface EntryAware<T extends EntryAware<T>> {

        void setEntry(Entry<T> entry);

        Entry<T> getEntry();
    }
}