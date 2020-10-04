package com.sinlo.core.timewheel.core;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DialOfTime a dial of time
 *
 * @author sinlo
 */
@SuppressWarnings("UnusedReturnValue")
public class DialOfTime {

    public final long interval;

    public final int scale;

    public final long tick;

    private final Cylinder<Deferred>[] buckets;

    private final DelayQueue<Cylinder<Deferred>> queue;

    private final AtomicInteger tally;

    @SuppressWarnings("unused")
    private volatile DialOfTime overflowee;

    private long current;

    public DialOfTime(long tick, int scale, long start, AtomicInteger tally, DelayQueue<Cylinder<Deferred>> queue) {
        this.interval = tick * scale;
        this.buckets = newBuckets(this.scale = scale, tally);
        this.queue = queue;
        this.tally = tally;
        this.tick = tick;
        this.current = start - (start % tick); // round down
    }

    public boolean overflow() {
        assert unsafe != null;
        return unsafe.compareAndSwapObject(this, overfloweeOffset, null,
                new DialOfTime(interval, scale, current, tally, queue));
    }

    /**
     * 添加任务
     *
     * @param entry 任务entry
     * @return 成功与否
     */
    public boolean add(Cylinder.Entry<Deferred> entry) {
        final long expir = entry.expir;
        if (entry.cancelled()
                || expir < current + tick) { // 已取消或已过期
            return false;
        } else if (expir < current + interval) {// 在区间内
            final long vid = expir / tick;
            final Cylinder<Deferred> bucket = buckets[(int) (vid % scale)].add(entry);
            if (bucket.setExpiration(vid * tick)) { // expiration的时间有所不同，才重新加入队列
                queue.offer(bucket);
            }
            return true;
        } else { // 超出区间(expir >= current + interval)
            if (this.overflowee == null) this.overflow(); // 尝试溢出时间盘
            return this.getOverflowee().add(entry); // 尝试给溢出时间盘添加entry
        }
    }

    /**
     * 时间推进
     *
     * @param t 推进的目标时间毫秒数
     */
    public void promote(long t) {
        if (t >= current + tick) {
            current = t - (t % tick); // 舍取为tick的倍数
            // 尝试推进溢出时间盘
            DialOfTime of = getOverflowee();
            if (of != null) of.promote(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static Cylinder<Deferred>[] newBuckets(int scale, AtomicInteger tally) {
        Cylinder<Deferred>[] cylinders = new Cylinder[scale];
        for (int i = 0; i < scale; i++) {
            cylinders[i] = new Cylinder<>(tally);
        }
        return cylinders;
    }

    public DialOfTime getOverflowee() {
        assert unsafe != null;
        return (DialOfTime) unsafe.getObject(this, overfloweeOffset);
    }

    /*==================*
     *  U N S A F E      *
     *==================*/
    private static final Unsafe unsafe = theUnsafe();
    private static final long overfloweeOffset;

    static {
        try {
            assert unsafe != null;
            overfloweeOffset = unsafe.objectFieldOffset(
                    DialOfTime.class.getDeclaredField("overflowee"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static Unsafe theUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
