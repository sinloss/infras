package com.sinlo.core.timewheel.spec;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ticker the ticker
 *
 * @author sinlo
 */
public abstract class Ticker {

    protected final DelayQueue<Cylinder<Deferred>> queue;

    protected final AtomicInteger tally;

    protected final DialOfTime dial;

    protected final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    protected final ReentrantReadWriteLock.ReadLock rlock = rwlock.readLock();
    protected final ReentrantReadWriteLock.WriteLock wlock = rwlock.writeLock();

    public Ticker(long tick, int scale, long start) {
        this.dial = new DialOfTime(tick, scale, start,
                this.tally = new AtomicInteger(0),
                this.queue = new DelayQueue<>());
    }

    public void add(Deferred deferred) {
        rlock.lock();
        try {
            this.addEntry(new Cylinder.Entry<>(deferred, deferred.defer + nanoedMillis()));
        } finally {
            rlock.unlock();
        }
    }

    protected void addEntry(Cylinder.Entry<Deferred> entry) {
        if (!dial.add(entry) && !entry.cancelled()) {
            executor().submit(entry.deferred);
        }
    }

    public boolean tick(long millis) {
        try {
            Cylinder<Deferred> bucket = queue.poll(millis, TimeUnit.MILLISECONDS);
            if (bucket != null) {
                wlock.lock();
                try {
                    while (bucket != null) {
                        dial.promote(bucket.getExpiration());
                        bucket.purge(this::addEntry);
                        bucket = queue.poll();
                    }
                } finally {
                    wlock.unlock();
                }
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int pending() {
        return tally.get();
    }

    public void shutdown() {
        executor().shutdown();
    }

    public abstract ExecutorService executor();

    public static long nanoedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}
