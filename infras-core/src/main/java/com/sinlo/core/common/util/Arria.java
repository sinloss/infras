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
     * concat several arrays together
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
     * get the total length of all given arrays
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
     * restore an array object to a typed array
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
     * join elements in an array into a string with the specified delimitor
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
