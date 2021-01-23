package com.sinlo.core.common.util;

/**
 * Strine the string util
 * <br/><strike>Oh, Do australians love strings?</strike>
 *
 * @author sinlo
 */
public class Strine {

    /**
     * Check if the string is empty or not
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Check if the string is not empty
     */
    public static boolean nonEmpty(String s) {
        return !isEmpty(s);
    }

    /**
     * Turn the possible null value to an empty string
     */
    public static String blankIfNull(String s) {
        return s == null ? "" : s;
    }

    /**
     * Ensure the {@code original} string ends with the {@code end}
     */
    public static String endItWith(String original, String end) {
        if (original.endsWith(end))
            return original;
        return original.concat(end);
    }

    /**
     * Ensure the {@code original} string starts with the {@code start}
     */
    public static String startItWith(String original, String start) {
        if (original.startsWith(start))
            return original;
        return start.concat(original);
    }
}
