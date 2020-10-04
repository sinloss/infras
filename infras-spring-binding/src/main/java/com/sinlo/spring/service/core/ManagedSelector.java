package com.sinlo.spring.service.core;

import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Repo;
import com.sinlo.core.domain.persistor.spec.Selector;

import java.util.HashMap;
import java.util.Map;

/**
 * The repo selector containing managed repo resources
 *
 * @author sinlo
 */
@SuppressWarnings("rawtypes")
public class ManagedSelector implements Selector {

    private static final Map<String, Repo> repos = new HashMap<>();

    protected static void add(Repo repo) {
        repos.put(repo.aw(), repo);
    }

    @Override
    public Repo select(Entity entity) {
        return repos.get(entity.getClass().getName());
    }
}
