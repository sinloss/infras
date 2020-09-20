package com.sinlo.core.domain;

import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.domain.spec.Selector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The general repo selector
 *
 * @author sinlo
 */
public class RepositoriesSelector<T extends Entity> implements Selector<T> {

    private final Map<String, Repo<? extends T>> mapping;

    public RepositoriesSelector(List<Repo<? extends T>> repos) {
        this.mapping = repos.stream()
                .collect(Collectors.toMap(Repo::support, r -> r));
    }

    @SafeVarargs
    public RepositoriesSelector(Repo<? extends T>... repos) {
        this.mapping = Arrays.stream(repos)
                .collect(Collectors.toMap(Repo::support, r -> r));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Repo<T> select(T t) {
        return (Repo<T>) mapping.get(t.getClass().getName());
    }
}
