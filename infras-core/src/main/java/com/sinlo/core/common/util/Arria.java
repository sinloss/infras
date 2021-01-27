package com.sinlo.core.common.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Arria the array util
 * <br/><strike>Ar<u>ria</u>(ya) Stark is the one who killed the Night King, and saved us all</strike>
 *
 * @author sinlo
 */
public class Arria {

    private Arria() {
    }

    /**
     * Check if the given {@link Collection} is empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Check if the given {@code array} is empty
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Check if the given {@link Collection} is not empty
     */
    public static boolean nonEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Check if the given {@code array} is not empty
     */
    public static <T> boolean nonEmpty(T[] array) {
        return !isEmpty(array);
    }

    /**
     * Concat several arrays together
     */
    @SafeVarargs
    public static <T> T[] concat(T[] first, T[]... rest) {
        if (rest != null && rest.length >= 1) {
            T[] ret = Arrays.copyOf(first, first.length + len(rest));
            int offset = first.length;
            for (T[] array : rest) {
                System.arraycopy(array, 0, ret, offset, array.length);
                offset += array.length;
            }
            return ret;
        }
        return first;
    }

    /**
     * Append all given elements to a given array by creating a new array
     * containing all elements
     *
     * @return the newly created array containing all elements
     */
    @SafeVarargs
    public static <T> T[] append(T[] origin, T... elements) {
        return concat(origin, elements);
    }

    /**
     * Check if the {@code e} is contained in the {@code es}
     */
    public static <T> boolean contains(T[] es, T e) {
        return indexOf(es, e) != -1;
    }

    /**
     * Get the index of {@code e} in the {@code es}
     */
    public static <T> int indexOf(T[] es, T e) {
        int end = es.length;
        if (e == null) {
            for (int i = 0; i < end; i++) {
                if (es[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < end; i++) {
                if (e.equals(es[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Get the total length of all given arrays
     */
    @SafeVarargs
    public static <T> int len(T[]... arrays) {
        int ret = 0;
        if (arrays != null) {
            for (T[] array : arrays) ret += array.length;
        }
        return ret;
    }

    /**
     * Refactor an array
     *
     * @param original  the original array
     * @param generator the new array generator
     * @param mapper    the element mapper
     * @param <T>       the original type of elements
     * @param <R>       the new type of elements
     * @return the new array
     */
    public static <T, R> R[] refactor(T[] original, IntFunction<R[]> generator, Function<T, R> mapper) {
        int len = original.length;
        R[] rs = generator.apply(len);
        for (int i = 0; i < len; i++) {
            rs[i] = mapper.apply(original[i]);
        }
        return rs;
    }

    /**
     * An overload of {@link #join(String, Function, Object[])}
     */
    @SafeVarargs
    public static <T> String join(String delimiter, T... array) {
        return join(delimiter, Object::toString, array);
    }

    /**
     * Join elements in an array into a string with the specified delimiter using the
     * given converter
     */
    @SafeVarargs
    public static <T> String join(String delimiter, Function<T, String> converter, T... array) {
        return join(Arrays.asList(array), delimiter, converter);
    }

    /**
     * An overload of {@link #join(Iterable, String, Function)}
     */
    public static <T> String join(Iterable<T> iterable, String delimiter) {
        return join(iterable, delimiter, Objects::toString);
    }

    /**
     * Join elements in an {@link Iterable} into a string with the specified {@code delimiter}
     * using the given converter
     */
    public static <T> String join(Iterable<T> iterable, String delimiter,
                                  Function<T, String> converter) {
        if (iterable == null) return "";
        Function<T, String> conv =
                converter == null ? Objects::toString : converter;

        StringBuilder builder = new StringBuilder();
        for (Iterator<T> it = iterable.iterator(); ; ) {
            builder.append(conv.apply(it.next()));
            if (!it.hasNext()) break;
            builder.append(delimiter);
        }
        return builder.toString();
    }

}
