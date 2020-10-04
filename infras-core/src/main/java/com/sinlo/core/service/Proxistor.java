package com.sinlo.core.service;

import com.sinlo.core.domain.persistor.Persistor;
import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Repo;
import com.sinlo.core.domain.persistor.spec.Selector;
import com.sinlo.core.domain.persistor.util.ReposSelector;
import com.sinlo.core.service.util.Xervice;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.SponteInitializer;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * The classes that are annotated by this annotation will be processed and wrapped into a proxy
 * consisting of specific logic, and registered into the {@link Pond}
 *
 * @author sinlo
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Sponte(value = Proxistor.Keeper.class)
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
    class Keeper extends Pond.Delegation<Proxistor, Map.Entry<Selector, Persistor>> {

        private static Pool.Simple<Selector> selectors = new Pool.Simple<>();

        @SuppressWarnings("unchecked")
        @Override
        protected Object handle(Map.Entry<Selector, Persistor> payload, Callable<Object> invocation) {
            try (Persistor.Stub stub = payload.getValue().enclose(null, payload.getKey())) {
                try {
                    return invocation.call();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    stub.cancel();
                } catch (Exception ignored) {
                }
                return null;
            }
        }

        @Override
        protected Map.Entry<Selector, Persistor> payload(Proxistor proxistor, Object service) {
            return new AbstractMap.SimpleImmutableEntry<>(
                    sel(proxistor.selector(), service), Persistor.of(proxistor.value()));
        }

        @Override
        protected Class<? extends Annotation> excluded() {
            return Ignore.class;
        }

        @SuppressWarnings("unchecked")
        private static Selector sel(Class<? extends Selector> assigned, Object service) {
            if (assigned.isInterface()) {
                List<Repo> repos = Xervice.derive(service, Repo.class);
                return repos.isEmpty()
                        ? Selector.ZERO_VALUE : new ReposSelector(repos);
            }
            return selectors.get(assigned.getName(), () -> Typer.create(assigned));
        }

        @Override
        public void finale(int fin) {
            super.finale(fin);
            if (fin == SponteInitializer.DECLINING) {
                selectors.purge();
                selectors = null;
            }
        }
    }
}
