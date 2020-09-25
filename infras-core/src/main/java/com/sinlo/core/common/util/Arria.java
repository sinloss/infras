package com.sinlo.core.common.util;

import java.lang.reflect.Array;
import java.util.Arrays;

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
     * Join elements in an array into a string with the specified delimitor
     *
     * @param arr       array
     * @param delimiter the delimiter
     * @return joined string
     */
    public static String join(Object[] arr, String delimiter) {
        if (delimiter == null) delimiter = "";
        StringBuilder builder = new StringBuilder();
        if (arr != null) {
            for (Object o : arr) {
                builder.append(delimiter).append(o);
            }
        }
        if (builder.length() >= 1) {
            return builder.substring(delimiter.length());
        }
        return "";
    }
}
