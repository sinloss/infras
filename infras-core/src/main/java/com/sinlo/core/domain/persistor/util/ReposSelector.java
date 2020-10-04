package com.sinlo.core.domain.persistor.util;

import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Repo;
import com.sinlo.core.domain.persistor.spec.Selector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The general repo selector
 *
 * @author sinlo
 */
public class ReposSelector<T extends Entity> implements Selector<T> {

    private final Map<String, Repo<? extends T>> mapping;

    public ReposSelector(List<Repo<? extends T>> repos) {
        this.mapping = repos.stream()
                .collect(Collectors.toMap(Repo::aw, r -> r));
    }

    @SafeVarargs
    public ReposSelector(Repo<? extends T>... repos) {
        this.mapping = Arrays.stream(repos)
                .collect(Collectors.toMap(Repo::aw, r -> r));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Repo<T> select(T t) {
        return (Repo<T>) mapping.get(t.getClass().getName());
    }
}
