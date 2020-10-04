package com.sinlo.core.common.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Arria the array util
 *
 * @author sinlo
 */
public class Arria {

    private Arria() {
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
     * Restore an array object to a typed array
     *
     * @param arr arr object
     * @param clz restored typed array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] restore(Object arr, Class<T> clz) {
        if (arr.getClass().isArray()) {
            int length = Array.getLength(arr);
            T[] restore = (T[]) Array.newInstance(clz, length);
            for (int i = 0; i < length; i++) {//String类特殊处理
                Object e = Array.get(arr, i);
                if (e != null && clz.isAssignableFrom(e.getClass())) {
                    restore[i] = (T) ((clz == String.class) ? String.valueOf(e) : e);
                }
            }
            return restore;
        }
        return null;
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
            builder.append(delimiter)
                    .append(conv.apply(it.next()));

            if (!it.hasNext()) break;
            builder.append(delimiter);
        }
        return builder.toString();
    }
}
