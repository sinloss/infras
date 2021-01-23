package com.sinlo.core.common.wraparound;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Two with two options
 *
 * @param <O> type of one option
 * @param <A> type of another option
 * @author sinlo
 */
public class Two<O, A> {

    private final O one;

    private final A another;

    public final Case c;

    private Two(O one, A another, Case c) {
        this.one = one;
        this.another = another;
        this.c = c;
    }

    public static <O, A> Two<O, A> two(O one, A another) {
        return new Two<>(one, another, one != null
                ? another == null ? Case.ONE : Case.BOTH
                : another != null ? Case.ANOTHER : Case.NONE);
    }

    public static <O, A> Two<O, A> one(O one) {
        return new Two<>(one, null, Case.ONE);
    }

    public static <O, A> Two<O, A> another(A another) {
        return new Two<>(null, another, Case.ANOTHER);
    }

    /**
     * Map
     *
     * @param oneMap     Function to map {@link #one}
     * @param anotherMap Function to map {@link #another}
     * @param <Ro>       The type of mapped {@link #one}
     * @param <Ra>       The type of mapped {@link #another}
     * @return mapped {@link Two}
     */
    public <Ro, Ra> Two<Ro, Ra> map(Function<O, Ro> oneMap, Function<A, Ra> anotherMap) {
        if (oneMap == null || anotherMap == null)
            throw new IllegalArgumentException(
                    "Any of the given map functions should present");
        return two(oneMap.apply(one), anotherMap.apply(another));
    }

    /**
     * Peek
     *
     * @param onePeek     Consumer to map {@link #one}
     * @param anotherPeek Consumer to map {@link #another}
     */
    public Two<O, A> peek(Consumer<O> onePeek, Consumer<A> anotherPeek) {
        if (onePeek == null || anotherPeek == null)
            throw new IllegalArgumentException(
                    "Any of the given peek consumers should present");
        onePeek.accept(one);
        anotherPeek.accept(another);
        return this;
    }

    /**
     * Apply the two functions according to the condition
     *
     * @param ifOne       apply this function on the {@link #one} if the it presents
     * @param elseAnother apply this function on the {@link #another} if the {@link #one} does not
     *                    present and the {@link #another} presents
     * @param <R>         the return type of given functions
     * @return {@link R}
     * @throws NeitherException if neither of {@link #one} nor {@link #another} presents
     */
    public <R> R either(Function<O, R> ifOne, Function<A, R> elseAnother) throws NeitherException {
        if (ifOne == null || elseAnother == null)
            throw new IllegalArgumentException(
                    "Any of the given map function should present");
        switch (c) {
            case ONE:
            case BOTH:
                return ifOne.apply(one);
            case ANOTHER:
                return elseAnother.apply(another);
            case NONE:
            default:
                throw new NeitherException();
        }
    }

    /**
     * Get the {@code one} optionally
     */
    public Optional<O> one() {
        return Optional.ofNullable(one);
    }

    /**
     * Get the {@code another} optionally
     */
    public Optional<A> another() {
        return Optional.ofNullable(another);
    }

    /**
     * Get the {@code one} or a {@link NullPointerException}
     */
    public O sureOne() {
        return Objects.requireNonNull(one);
    }

    /**
     * Get the {@code another} or a {@link NullPointerException}
     */
    public A sureAnother() {
        return Objects.requireNonNull(another);
    }

    public enum Case {
        ONE, ANOTHER, BOTH, NONE
    }

    public static class NeitherException extends Exception {

        public NeitherException() {
            super("Neither of the two options is available");
        }
    }
}
