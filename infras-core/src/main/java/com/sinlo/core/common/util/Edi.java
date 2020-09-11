package com.sinlo.core.common.util;

import java.util.UUID;

/**
 * Edi the id util
 *
 * @author sinlo
 */
public class Edi {

    private Edi() {
    }

    /**
     * generate 32 digits uuid string without '-'
     */
    public static String uuid() {
        UUID uuid = UUID.randomUUID();
        return digits(uuid.getMostSignificantBits() >> 32, 8) +
                digits(uuid.getMostSignificantBits() >> 16, 4) +
                digits(uuid.getMostSignificantBits(), 4) +
                digits(uuid.getLeastSignificantBits() >> 48, 4) +
                digits(uuid.getLeastSignificantBits(), 12);
    }

    /**
     * returns val represented by the specified number of hex digits
     */
    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    /**
     * generate random string
     *
     * @param len   the length of the random string
     * @param radix the radix
     */
    public static String rand(int len, final int radix) {
        long multiplier = (long) Math.pow(radix, len);
        if ((multiplier + 1L) < 0) {// overflow
            return rand((len /= 2), radix) + rand(len, radix);
        }
        return Long.toString((long) (Math.random() * (multiplier + 1L)), radix);
    }

    /**
     * generate random string with the radix of 36
     *
     * @param len the length of the random string
     */
    public static String rand(int len) {
        return rand(len, 36);
    }
}
