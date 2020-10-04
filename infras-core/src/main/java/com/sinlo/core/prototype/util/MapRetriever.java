package com.sinlo.core.prototype.util;

import com.sinlo.core.prototype.spec.Retriever;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * The {@link java.util.Map} retriever with a little filter
 *
 * @author sinlo
 */
public class MapRetriever implements Retriever {

    private final Map<String, ?> map;
    private final BiFunction<String, Class<?>, Boolean> filter;

    public MapRetriever(Map<String, ?> map) {
        this(map, null);
    }

    public MapRetriever(Map<String, ?> map, BiFunction<String, Class<?>, Boolean> filter) {
        if (map == null) {
            throw new IllegalArgumentException(
                    "The given map could not be null");
        }
        this.map = map;
        this.filter = filter;
    }

    @Override
    public Object retrieve(String name, Class<?> type, Object value) {
        Object val = map.get(name);
        if (val == null
                || !type.isAssignableFrom(val.getClass())
                || (filter != null && !filter.apply(name, type))) {
            return SKIP;
        }
        return val;
    }
}
