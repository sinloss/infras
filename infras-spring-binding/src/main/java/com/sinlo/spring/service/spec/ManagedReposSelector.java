package com.sinlo.spring.service.spec;

import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.domain.spec.Selector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The repo selector containing managed repo resources
 *
 * @author sinlo
 */
@SuppressWarnings("rawtypes")
public class ManagedReposSelector implements Selector {

    public static final ManagedReposSelector SPRING_MANAGED = new ManagedReposSelector();

    private final Map<String, Repo> repos = new HashMap<>();

    public void add(Repo repo) {
        repos.put(repo.support(), repo);
    }

    public ManagedReposSelector modify(Consumer<Map<String, Repo>> consumer) {
        if (consumer != null) {
            consumer.accept(repos);
        }
        return this;
    }

    @Override
    public Repo select(Entity entity) {
        return repos.get(entity.getClass().getName());
    }
}
