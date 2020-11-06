package com.sinlo.core.service;

import com.sinlo.core.service.spec.Pump;
import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Perch;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Typer;

import javax.lang.model.element.ElementKind;
import java.lang.annotation.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To keep a manifest of all {@link Ponded} annotations, so that the {@link Pond} could get
 * an array of annotations as subjects
 *
 * @author sinlo
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Sponte(inheritable = false)
@Must(value = ElementKind.ANNOTATION_TYPE, with = Sponte.class,
        spec = {@Must.Spec(value = "value", clz = Pond.Keeper.class),
                @Must.Spec(value = "agent.value", clz = Pond.Delegate.class)})
public @interface Ponded {

    class Manifest {

        /**
         * Get all subjects
         */
        @SuppressWarnings("unchecked")
        public static Class<? extends Annotation>[] get() {
            return Sponte.Fo.lines(Ponded.class, Typer::forSure).toArray(new Class[0]);
        }

        /**
         * Get {@link Sponte spontes} of all subjects
         */
        public static Stream<Map.Entry<Class<? extends Annotation>, Sponte>> spontes() {
            return Arrays.stream(get())
                    .map(c -> new SimpleImmutableEntry<>(
                            c, c.getAnnotation(Sponte.class)));
        }

        /**
         * Get all {@link com.sinlo.core.service.Pond.Keeper keepers} of all subjects
         */
        public static Stream<Map.Entry<Class<? extends Annotation>, Pond.Keeper<?, ?>>> keepers() {
            return spontes().map(entry -> new SimpleImmutableEntry<>(entry.getKey(),
                    Optional.of(entry.getValue()).map(Sponte::value)
                            .filter(Pond.Keeper.class::isAssignableFrom)
                            .map(c -> SponteAware.pri.get(entry.getValue(), c))
                            .map(keeper -> (Pond.Keeper<?, ?>) keeper)
                            .orElseThrow(IllegalStateException::new)));
        }

        /**
         * Get a map of all subjects annotated {@link Class classes} and their corresponding
         * {@link com.sinlo.core.service.Pond.Keeper keepers}
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static Map<String, KeeperValue> keeperMap() {
            return keepers().flatMap(entry ->
                    Sponte.Fo.lines(entry.getKey(), Perch::typeName).stream()
                            .map(Typer::forSure)
                            .map(c -> new SimpleImmutableEntry<>(
                                    c.getName(), new KeeperValue(entry.getKey(), entry.getValue()))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    /**
     * The value of the map returned by {@link Manifest#keeperMap()}
     *
     * @param <T> the type of annotation {@link #pivot}
     */
    class KeeperValue<T extends Annotation> {
        public final Class<T> pivot;
        public final Pond.Keeper<T, ?> keeper;
        public final Profile.Subjectifier<T> subjectifier;

        public KeeperValue(Class<T> pivot, Pond.Keeper<T, ?> keeper) {
            this.pivot = pivot;
            this.keeper = keeper;
            this.subjectifier = new Profile.Subjectifier<>(
                    e -> e.getAnnotation(pivot),
                    Sponte.Fo.inheritors(this.pivot));
        }

        /**
         * @see Pond.Keeper#maintain(Class, Object, Function, Pump)
         */
        public Object maintain(final Class<?> type, final Object target, final Pump pump) {
            return keeper.maintain(type, target, subjectifier, pump);
        }
    }
}
