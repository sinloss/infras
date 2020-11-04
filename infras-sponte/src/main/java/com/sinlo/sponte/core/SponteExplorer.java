package com.sinlo.sponte.core;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The sponte annotated elements explorer
 *
 * @author sinlo
 */
public class SponteExplorer<T> {

    private final Stream<Profile> profiles;

    private final T payload;

    private SponteExplorer(Stream<Profile> profiles, T payload) {
        this.profiles = profiles;
        this.payload = payload;
    }

    /**
     * Same as {@link #of(Predicate, Object, Class[])} but with no {@code filter}
     */
    @SafeVarargs
    public static <T> SponteExplorer<T> of(T payload, Class<? extends Annotation>... subjects) {
        return of(null, payload, subjects);
    }

    /**
     * Same as {@link #of(Class, Predicate, Object, Class[])} but with no {@code inherited}
     */
    @SafeVarargs
    public static <T> SponteExplorer<T> of(Predicate<Profile> filter,
                                           T payload,
                                           Class<? extends Annotation>... subjects) {
        return of(null, filter, payload, subjects);
    }

    /**
     * Create an explorer that could explore all given {@code subjects} by calling all the
     * {@link SponteAware} defined in their scopes, and call a global {@code handler} on
     * every subject explored
     */
    @SafeVarargs
    public static <T> SponteExplorer<T> of(Class<? extends Annotation> inherited,
                                           Predicate<Profile> filter,
                                           T payload,
                                           Class<? extends Annotation>... subjects) {
        if (subjects == null) return new SponteExplorer<>(Stream.empty(), payload);
        return new SponteExplorer<>(Stream.of(subjects)
                .flatMap(subject -> Sponte.Fo.profiles(subject)
                        .stream().filter(filter == null ? profile -> true : filter))
                .filter(Objects::nonNull), payload);

    }

    public Stream<Profile> profiles() {
        return profiles;
    }

    /**
     * Do the explore on every profile
     *
     * @see #explore(Profile, Object)
     */
    public Stream<? extends SponteAware> explore() {
        if (profiles == null) return Stream.empty();
        return profiles.map(profile -> explore(profile, payload));
    }

    /**
     * Call the {@link SponteAware#onExplore(Profile, Object)} method in the corresponding annotation
     * regarding the given {@link Profile}
     */
    public static <T> SponteAware explore(Profile profile, T payload) {
        Sponte sponte = profile.sponte;
        Class<? extends SponteAware> type = sponte.value();
        // ignore uninstantiable value
        if (type.isInterface()) return null;
        SponteAware aware = SponteAware.pri.get(sponte, type);
        if (aware != null && aware.filter(profile)) {
            aware.onExplore(profile, payload);
        }
        return aware;
    }

}
