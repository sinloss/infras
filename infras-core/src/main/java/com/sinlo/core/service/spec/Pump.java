package com.sinlo.core.service.spec;

import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * The pond pump which pumps objects in and out of the pond
 *
 * @author sinlo
 */
@FunctionalInterface
public interface Pump {

    /**
     * The default utility method to create an instance of the given type, it is used
     * by the {@link com.sinlo.core.service.Pond.Keeper#onExplore(Profile, Object)}
     * in the case of {@link Pump}'s absence
     */
    static <T> T create(Class<T> type) {
        return Typer.create(type);
    }

    /**
     * The {@link com.sinlo.core.service.Pond.Keeper} needs an instance of the given
     * type
     *
     * @return the instance needed
     */
    <T> T sink(Class<T> type);

    /**
     * The {@link com.sinlo.core.service.Pond.Keeper} has already maintained the given
     * object, and it is ready to be placed in the {@link com.sinlo.core.service.Pond}
     *
     * @return the object to be placed in the pond, or null to stop it from being
     * placed in the pond
     */
    default <T> T sink(T t) {
        return t;
    }

    /**
     * The {@link com.sinlo.core.service.Pond.Keeper} has got an annotation, and is
     * about to use it to prepare payloads
     *
     * @return the annotation instance to be used to prepare payloads
     */
    default <A extends Annotation> A note(A annotation, AnnotatedElement element) {
        return annotation;
    }
}
