package com.sinlo.core.common.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Collin the collector of {@link java.util.stream.Collector}s
 * <br/><strike>Collin likes to collect collectors</strike>
 *
 * @author sinlo
 */
public class Collin {

    /**
     * The collector for {@link ConcurrentSkipListSet}
     */
    public static <T> Collector<T, ?, ConcurrentSkipListSet<T>> toSkipList() {
        return Collectors.toCollection(ConcurrentSkipListSet::new);
    }

    /**
     * The collector for joining with a tally counting the joined elements
     */
    public static class CountingJoiner implements Collector<CharSequence, StringBuilder, String> {
        private int count = 0;
        /**
         * The delimiter used in the joining process
         */
        public final CharSequence delimiter;

        /**
         * Factory method
         *
         * @param delimiter the delimiter used by the joining
         */
        public static CountingJoiner joining(CharSequence delimiter) {
            return new CountingJoiner(delimiter);
        }

        /**
         * @see #joining(CharSequence)
         */
        public static CountingJoiner joining() {
            return new CountingJoiner("");
        }

        private CountingJoiner(CharSequence delimiter) {
            this.delimiter = delimiter;
        }

        private StringBuilder add(StringBuilder joiner, CharSequence cs) {
            if (count > 0) joiner.append(delimiter);
            count++;
            return joiner.append(cs);
        }

        /**
         * Get the final count
         */
        public int count() {
            return count;
        }

        @Override
        public Supplier<StringBuilder> supplier() {
            return StringBuilder::new;
        }

        @Override
        public BiConsumer<StringBuilder, CharSequence> accumulator() {
            return this::add;
        }

        @Override
        public BinaryOperator<StringBuilder> combiner() {
            return StringBuilder::append;
        }

        @Override
        public Function<StringBuilder, String> finisher() {
            return StringBuilder::toString;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }

    /**
     * Simple implementation of {@code Collector}.
     *
     * @param <T> the type of elements to be collected
     * @param <R> the type of the result
     */
    public static class Impl<T, A, R> implements Collector<T, A, R> {
        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Characteristics> characteristics;

        Impl(Supplier<A> supplier,
             BiConsumer<A, T> accumulator,
             BinaryOperator<A> combiner,
             Function<A, R> finisher,
             Characteristics... characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = Arria.collect(HashSet::new, characteristics);
        }

        Impl(Supplier<A> supplier,
             BiConsumer<A, T> accumulator,
             BinaryOperator<A> combiner,
             Characteristics... characteristics) {
            this(supplier, accumulator, combiner, Funny::cast, characteristics);
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }
}
