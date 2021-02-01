package com.sinlo.core.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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

    /**
     * If the given char is lowercase
     */
    public static boolean isLower(char c) {
        return c >= 0x61 && c <= 0x7A;
    }

    /**
     * If the given char is uppercase
     */
    public static boolean isUpper(char c) {
        return c >= 0x41 && c <= 0x5A;
    }

    /**
     * Uppercase the initial letter of the given word
     */
    public static String capInitial(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    public static Splitting split(String raw) {
        return new Splitting(raw);
    }

    /**
     * The splitting step
     */
    public static class Splitting {

        private final String raw;

        private Splitting(String raw) {
            this.raw = raw;
        }

        /**
         * Split by a string delimiter
         */
        public Splits by(String delim) {
            return new Splits(delim, raw.split(delim));
        }

        /**
         * Split by a char delimiter
         */
        public Splits by(char delim) {
            return by(String.valueOf(delim));
        }

        /**
         * Split by the underscore which is mostly for the snake_case, yet can also properly
         * split MACRO_CASE_STRINGS
         */
        public Splits bySnake() {
            return by('_');
        }

        /**
         * Split by the dash which is mostly for the kebab-case, yet can also properly
         * split COBOL-CASE-STRINGS
         */
        public Splits byKebab() {
            return by('-');
        }

        /**
         * Split by the uppercase words, which can handle both camelCaseStrings and
         * PascalCaseStrings
         */
        public Splits byCamel() {
            List<String> parts = new ArrayList<>();
            int pos = 0;
            for (int i = 1; i < raw.length(); i++) {
                if (isLower(raw.charAt(i))) continue;
                parts.add(raw.substring(pos, i));
                pos = i;
            }
            return new Splits("", parts.toArray(new String[0]));
        }

    }

    /**
     * The split parts
     */
    public static class Splits {

        private final String delim;
        private final String[] splits;

        private Splits(String delim, String... splits) {
            this.delim = delim;
            this.splits = splits;
        }

        /**
         * The count of the split parts
         */
        public int count() {
            return splits.length;
        }

        /**
         * Produce a stream of all the split parts
         */
        public Stream<String> stream() {
            return Arrays.stream(splits);
        }

        /**
         * Produce a stream of a range of split parts
         *
         * @param from where the range starts, inclusive
         * @param to   where the range ends, exclusive
         * @return the proper stream
         */
        public Stream<String> stream(int from, int to) {
            return Arrays.stream(Arrays.copyOfRange(splits, from, to));
        }

        /**
         * <pre>Equivalent to {@code stream(from, this.count())}</pre>
         *
         * @see #stream(int, int)
         */
        public Stream<String> stream(int from) {
            return stream(from, splits.length);
        }

        /**
         * Produce a stream of the split parts with their original delimiter appended
         */
        public Stream<String> raw() {
            return delimit(this.delim);
        }

        /**
         * Produce a stream of split parts converted to camelCase
         */
        public Stream<String> camel() {
            return Stream.concat(stream(0, 1).map(String::toLowerCase),
                    stream(1).map(String::toLowerCase).map(Strine::capInitial));
        }

        /**
         * Produce a stream of split parts converted to PascalCase
         */
        public Stream<String> pascal() {
            return stream().map(String::toLowerCase).map(Strine::capInitial);
        }

        /**
         * Produce a stream of split parts converted to kebab-case
         */
        public Stream<String> kebab() {
            return delimit("-").map(String::toLowerCase);
        }

        /**
         * Produce a stream of split parts converted to COBOL-CASE
         */
        public Stream<String> cobol() {
            return delimit("-").map(String::toUpperCase);
        }

        /**
         * Produce a stream of split parts converted to snake_case
         */
        public Stream<String> snake() {
            return delimit("_").map(String::toLowerCase);
        }

        /**
         * Produce a stream of split parts converted to MACRO_CASE
         */
        public Stream<String> macro() {
            return delimit("_").map(String::toUpperCase);
        }

        /**
         * Produce a stream of split parts delimited by another delimiter
         */
        public Stream<String> delimit(String delim) {
            return Stream.concat(
                    stream(0, splits.length - 1).map(s -> s + delim),
                    stream(splits.length - 1));
        }
    }

}
