package com.sinlo.core.common.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Loki the lock util
 *
 * @author sinlo
 */
public class Loki {

    /**
     * Create a {@link Group} of locks and then call the {@link #sequentially(Callable, Group)}
     */
    public static <T> T sequentially(Callable<T> action, Object... locks) {
        return sequentially(action, Group.of(locks));
    }

    /**
     * Sequentially lock all locks in the given {@link Group locks}, and then do the
     * given {@link Callable action}. This is mainly used to avoid dead lock
     */
    public static <T> T sequentially(Callable<T> action, Group locks) {
        final Object lock = locks.take();
        if (lock == null) {
            // all locks are successfully locked
            try {
                return action.call();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            return sequentially(action, locks);
        }
    }

    /**
     * A group of locks
     */
    public static class Group {

        public static final Object[] ZERO_VALUE = new Object[0];

        private final Object[] locks;
        /**
         * A cursor pointing at the current lock object
         */
        private int cursor;

        /**
         * Filter and sort the given locks array, and crate a group with it
         */
        public static Group of(Object... locks) {
            return new Group(Arrays.stream(locks)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(Object::hashCode))
                    .toArray(Object[]::new));
        }

        private Group(Object... locks) {
            this.locks = locks == null ? ZERO_VALUE : locks;
            this.cursor = 0;
        }

        /**
         * Peek the current pointed lock object
         */
        public Object peek() {
            if (cursor >= locks.length)
                return null;
            return locks[cursor];
        }

        /**
         * Take a lock object and move cursor to the next one
         */
        public Object take() {
            if (cursor >= locks.length)
                return null;
            return locks[cursor++];
        }

        /**
         * Reset the cursor to the start of the locks array
         */
        public Group reset() {
            this.cursor = 0;
            return this;
        }
    }
}
