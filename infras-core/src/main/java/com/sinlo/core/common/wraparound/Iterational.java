package com.sinlo.core.common.wraparound;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Common iteration wraparound
 *
 * @author sinlo
 */
public class Iterational<T> implements Iterable<T> {

    private final T initial;
    private final BiPredicate<T, Integer> condition;
    private final BiFunction<T, Integer, T> next;

    /**
     * Instantiate an {@link Iterational}
     *
     * @param root    the initial element. If there's no intention of using the first parameter in
     *                neither the {@code hasNext} nor {@code next}, feel free to make it null
     * @param hasNext predication of next element's existence
     * @param next    provide the next element
     */
    public static <T> Iterational<T> of(T root,
                                        BiPredicate<T, Integer> hasNext,
                                        BiFunction<T, Integer, T> next) {
        return new Iterational<>(root, hasNext, next);
    }

    private Iterational(T initial, BiPredicate<T, Integer> condition, BiFunction<T, Integer, T> next) {
        this.initial = initial;
        this.condition = condition;
        this.next = next;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private T t = initial;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return condition.test(t, cursor);
            }

            @Override
            public T next() {
                T n = t;
                t = next.apply(t, ++cursor);
                return n;
            }
        };
    }

    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
