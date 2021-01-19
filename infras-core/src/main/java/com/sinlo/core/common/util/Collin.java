package com.sinlo.core.common.util;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Collin the collector of {@link java.util.stream.Collector}s
 * <br/><strike>Collin likes to collect collectors</strike>
 *
 * @author sinlo
 */
public class Collin {

    /**
     * The collector for {@link ConcurrentSkipListSet}
     */
    public static <T> Collector<T, ?, ConcurrentSkipListSet<T>> toSkipList() {
        return Collectors.toCollection(ConcurrentSkipListSet::new);
    }
}
