package com.sinlo.sponte.core;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The sponte annotated elements explorer
 *
 * @author sinlo
 */
public class SponteExplorer {

    private SponteExplorer() {
    }

    /**
     * Same as {@link #explore(Predicate, Object, Class[])} but with no {@code filter}
     */
    @SafeVarargs
    public static <T> Stream<? extends SponteAware> explore(T payload, Class<? extends Annotation>... subjects) {
        return explore(null, payload, subjects);
    }

    @SafeVarargs
    public static <T> Stream<? extends SponteAware> explore(Predicate<Profile> filter,
                                                            T payload,
                                                            Class<? extends Annotation>... subjects) {
        return explore(null, filter, payload, subjects);
    }

    /**
     * Explore all given {@code subjects} by calling all the {@link SponteAware} defined
     * in their scopes, and call a global {@code handler} on every subject explored
     */
    @SafeVarargs
    public static <T> Stream<? extends SponteAware> explore(Class<? extends Annotation> inherited,
                                                            Predicate<Profile> filter,
                                                            T payload,
                                                            Class<? extends Annotation>... subjects) {
        if (subjects == null) return Stream.empty();
        return Stream.of(subjects)
                .flatMap(subject -> Sponte.Fo.lines(subject,
                        line -> Profile.of(line, subject, Sponte.Fo.inheritors(subject)))
                        .stream()
                        .filter(filter == null ? profile -> true : filter)
                        .map(profile -> {
                            Sponte sponte = profile.sponte;
                            Class<? extends SponteAware> type = sponte.value();
                            // ignore uninstantiable value
                            if (type.isInterface()) return null;
                            SponteAware aware = SponteAware.pri.get(type,
                                    Sponte.Keys.get(sponte, type),
                                    () -> Typer.create(type));
                            if (aware != null && aware.filter(profile)) {
                                aware.onExplore(profile, payload);
                            }
                            return aware;
                        }))
                .filter(Objects::nonNull);

    }
}
