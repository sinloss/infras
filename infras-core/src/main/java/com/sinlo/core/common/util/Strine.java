package com.sinlo.core.common.util;

/**
 * Strine the string util
 * <br/><strike>Oh, Do australians love string?</strike>
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
}
