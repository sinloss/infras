package com.sinlo.core.common.wraparound;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * The simple growable iterable wraparound of an array. Please keep in mind
 * that this is not a thread safe approach
 *
 * @param <T> the type of the objects
 */
public class Growable<T> implements Iterable<T> {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private T[] ts;
    private int size;
    private int modCount;

    /**
     * Add one or more objects
     */
    @SafeVarargs
    public final void add(T... ts) {
        for (T t : ts) {
            if (size - ts.length >= 0) this.grow(size + 1);
            this.modCount++;
            this.ts[size++] = t;
        }
    }

    /**
     * Get the object at the specific index
     */
    public T get(int index) {
        return ts[index];
    }

    /**
     * Remove the object at the specific index
     *
     * @return the removed object
     */
    public T remove(int index) {
        this.modCount++;
        T removed = ts[index];
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(ts, index + 1, ts, index, numMoved);
        ts[--size] = null; // clear to let GC do its work
        return removed;
    }

    /**
     * Grow the size
     *
     * @param min the minimum size to grow
     */
    public void grow(int min) {
        this.modCount++;
        int ocap = ts.length;
        int ncap = (ncap = (ocap + (ocap >> 1))) - min < 0 ? min : ncap;
        if (ncap - MAX_ARRAY_SIZE > 0) {
            if (ncap < 0)// overflow!
                throw new OutOfMemoryError();
            ncap = Integer.MAX_VALUE;
        }
        this.ts = Arrays.copyOf(ts, ncap);
    }

    /**
     * Get an iterator of all the objects
     */
    @Override
    public Iterator<T> iterator() {
        return new It();
    }

    private class It implements Iterator<T> {

        private final int expected;

        private int cursor;

        public It() {
            this.cursor = 0;
            this.expected = Growable.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return cursor < Growable.this.size;
        }

        @Override
        public T next() {
            if (expected != Growable.this.modCount)
                throw new ConcurrentModificationException();
            return Growable.this.ts[cursor++];
        }
    }
}
