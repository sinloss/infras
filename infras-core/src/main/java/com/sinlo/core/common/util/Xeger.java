package com.sinlo.core.common.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Xeger is the shadow brother of regex
 *
 * @author sinlo
 */
public class Xeger {

    /**
     * Zip all given regex expressions together as an one big regex pattern
     *
     * @param delim split every regex expression into several parts by this delimiter
     *              to merge the same parts of all expressions
     * @see #zip(Map)
     * @see #zip(String, Collection)
     * @see #zip(String, Stream)
     */
    public static Pattern zip(String delim, String... exprs) {
        return zip(Strine.tree(delim, exprs));
    }

    /**
     * Zip all given regex expressions together as an one big regex pattern
     *
     * @param delim split every regex expression into several parts by this delimiter
     *              to merge the same parts of all expressions
     * @see #zip(Map)
     * @see #zip(String, String...)
     * @see #zip(String, Stream)
     */
    public static Pattern zip(String delim, Collection<String> exprs) {
        return zip(Strine.tree(delim, exprs));
    }

    /**
     * Zip all given regex expressions together as an one big regex pattern
     *
     * @param delim split every regex expression into several parts by this delimiter
     *              to merge the same parts of all expressions
     * @see #zip(Map)
     * @see #zip(String, String...)
     * @see #zip(String, Collection)
     */
    public static Pattern zip(String delim, Stream<String> exprs) {
        return zip(Strine.tree(delim, exprs));
    }

    /**
     * Zip all given regex expressions together as an one big regex pattern
     *
     * @param tree the {@link Jason.Thingama.Bob} produced by the {@link Strine#tree(String, Stream)},
     *             {@link Strine#tree(String, String...)} and {@link Strine#tree(String, Collection)}.
     *             Or some other kind of {@link Map} having the same structure produced by the said
     *             methods
     */
    public static Pattern zip(Map<?, ?> tree) {
        return Pattern.compile(build(tree).toString());
    }

    // zip: build the map
    private static CharSequence build(Map<?, ?> map) {
        return or(map.entrySet().stream().map(Xeger::build));
    }

    // zip: build the entry
    private static CharSequence build(Map.Entry<?, ?> entry) {
        return new StringBuilder().append(entry.getKey()).append(build(entry.getValue()));
    }

    // zip: build the value
    private static CharSequence build(Object value) {
        if (value instanceof Map)
            return build((Map<?, ?>) value);
        if (value instanceof List) {
            return or(((List<?>) value).stream().flatMap(v -> v instanceof Map
                    ? ((Map<?, ?>) v).entrySet().stream().map(Xeger::build)
                    : Stream.of(v.toString())));
        }
        return new StringBuilder().append(value);
    }

    /**
     * Create a regex 'or' expression of the given patterns {@link Stream}
     */
    public static CharSequence or(Stream<CharSequence> patterns) {
        Collin.CountingJoiner cj = Collin.CountingJoiner.joining("|");
        // join
        String inner = patterns.collect(cj);
        if (cj.count() <= 1) {
            return inner;
        }
        return new StringBuilder("(?:").append(inner).append(")");
    }

    /**
     * Check if the given {@link Matcher} matches partially
     */
    public static boolean partiallyMatches(Matcher matcher) {
        return matcher.matches() || matcher.hitEnd();
    }
}
