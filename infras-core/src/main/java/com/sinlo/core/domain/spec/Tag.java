package com.sinlo.core.domain.spec;

/**
 * Tag the tagged tag
 */
public class Tag<T extends Entity> {
    public final Channel chan;
    public final T entity;

    public Tag(Channel chan, T entity) {
        this.chan = chan;
        this.entity = entity;
    }
}
