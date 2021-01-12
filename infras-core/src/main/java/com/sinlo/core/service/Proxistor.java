package com.sinlo.core.service;

import com.sinlo.core.domain.persistor.Persistor;
import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Repo;
import com.sinlo.core.domain.persistor.spec.Selector;
import com.sinlo.core.domain.persistor.util.ReposSelector;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.SponteInitializer;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * The classes that are annotated by this annotation will be processed and wrapped into a proxy
 * which delegates methods with {@link Persistor#enclose(Consumer, Selector)}, and registered
 * the delegated object into the {@link Pond}
 *
 * @author sinlo
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Sponte(value = Proxistor.Keeper.class,
        agent = @Agent(Proxistor.Delegate.class))
@Ponded
public @interface Proxistor {

    /**
     * The entity class used to get a corresponding {@link com.sinlo.core.domain.persistor.Persistor}
     */
    Class<? extends Entity> value();

    /**
     * The custom selector
     */
    Class<? extends Selector> selector() default Selector.class;

    /**
     * Tell {@link Proxistor} wrapped proxies to ignore the annotated method
     *
     * @author sinlo
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Ignore {
    }

    @SuppressWarnings("rawtypes")
    class Delegate implements Pond.Delegate<Map.Entry<Selector, Persistor>> {
        @SuppressWarnings("unchecked")
        @Override
        public <R> R handle(Agent.Context context, Agent.Mission<R> mission,
                            Map.Entry<Selector, Persistor> payload) {
            try (Persistor.Stub stub = payload.getValue()
                    .enclose(null, payload.getKey())) {
                try {
                    return mission.call(context.args);
                } catch (Exception e) {
                    stub.cancel();
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    class Keeper implements Pond.Keeper<Proxistor, Map.Entry<Selector, Persistor>> {

        private static final Pool.Simple<Selector> selectors = new Pool.Simple<>();

        @Override
        public Map.Entry<Selector, Persistor> payload(Proxistor proxistor, Object service, Method method) {
            return new AbstractMap.SimpleImmutableEntry<>(
                    sel(proxistor.selector(), service), Persistor.of(proxistor.value()));
        }

        @Override
        public boolean should(Proxistor proxistor, Method method, boolean lazy) {
            return !method.isAnnotationPresent(Ignore.class);
        }

        @SuppressWarnings("unchecked")
        private static Selector sel(Class<? extends Selector> assigned, Object service) {
            if (assigned.isInterface()) {
                final List<Repo> repos = new LinkedList<>();
                // derive from fields of the given service
                Prototype.of((Class<Object>) service.getClass()).every(property -> {
                    if (property.is(Repo.class)) {
                        Repo repo = (Repo) property.get(service);
                        if (repo != null) repos.add(repo);
                    }
                });

                return repos.isEmpty()
                        ? Selector.ZERO_VALUE : new ReposSelector(repos);
            }
            return selectors.get(assigned.getName(), () -> Typer.create(assigned));
        }

        @Override
        public void finale(int fin) {
            if (SponteInitializer.DECLINING == fin) {
                selectors.purge();
            }
        }
    }

    /**
     * A default {@link Proxistor} which uses the {@link Entity} as the {@link Proxistor#value()}
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    @Proxistor(value = Entity.class)
    @interface Default {

        Class<? extends Selector> selector() default Selector.class;

        /**
         * The default {@link Persistor}
         */
        Persistor<Entity> persistor = Persistor.of(Entity.class);
    }

}
