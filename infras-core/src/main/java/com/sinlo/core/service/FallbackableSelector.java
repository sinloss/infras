package com.sinlo.core.service;

import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.domain.spec.Selector;

/**
 * @author sinlo
 */
@SuppressWarnings("rawtypes")
public class FallbackableSelector implements Selector {

    private final Selector primary;

    private final Selector fallback;

    public FallbackableSelector(Selector primary, Selector fallback) {
        this.primary = primary == null ? Selector.ZERO_VALUE : primary;
        this.fallback = fallback == null ? Selector.ZERO_VALUE : fallback;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Repo select(Entity entity) {
        Repo repo = primary.select(entity);
        if (repo == null) {
            return fallback.select(entity);
        }
        return repo;
    }
}
