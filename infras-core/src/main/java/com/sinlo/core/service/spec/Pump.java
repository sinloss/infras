package com.sinlo.core.service.spec;

import com.sinlo.sponte.spec.Ext;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Function;

/**
 * The pond pump which pumps objects in and out of the pond
 *
 * @author sinlo
 */
@FunctionalInterface
public interface Pump {

    /**
     * The default {@link Default}
     */
    Pump DEFAULT = new Default();

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
        return DEFAULT.sink(t);
    }

    /**
     * The {@link com.sinlo.core.service.Pond.Keeper} has got an annotation, and is
     * about to use it to prepare payloads
     *
     * @return the annotation instance to be used to prepare payloads
     */
    default <A extends Annotation> A note(A annotation, AnnotatedElement element) {
        return DEFAULT.note(annotation, element);
    }

    /**
     * Should the maintenance be proceeded or not
     */
    default boolean should(Class<?> type, Object service) {
        return DEFAULT.should(type, service);
    }

    /**
     * Create a {@link Pump} instance with default behaviours and a specific {@link Filter}
     * as its {@link #should(Class, Object)}. It could be used to filter the process of the
     * {@link com.sinlo.core.service.Pond.Keeper#maintain(Class, Object, Function, Pump)}
     * without changing other default behaviours
     */
    static Default filter(Filter filter) {
        return new Default(filter);
    }

    /**
     * Create a {@link Pump} instance with default behaviours and a specific {@link Processor}
     * as its {@link #sink(Object)}. It could be used to process the final created instance of
     * {@link com.sinlo.core.service.Pond.Keeper#maintain(Class, Object, Function, Pump)}
     * without changing other default behaviours
     */
    static Default processor(Processor filter) {
        return new Default(filter);
    }

    /**
     * @see #filter(Filter)
     */
    @FunctionalInterface
    interface Filter {

        /**
         * @see Default#should(Class, Object)
         */
        boolean should(Class<?> type, Object service);
    }

    /**
     * @see #processor(Processor)
     */
    @FunctionalInterface
    interface Processor {

        /**
         * @see Default#sink(Object)
         */
        <T> T sink(T t);
    }

    /**
     * The default behaviour for the {@link com.sinlo.core.service.Pond.Keeper#maintain(Class, Object, Function, Pump)}
     * to create an instance of the given type in case of {@link Pump}'s absence
     */
    class Default implements Pump {

        private final Filter filter;
        private final Processor processor;

        private Default() {
            this.filter = null;
            this.processor = null;
        }

        private Default(Filter filter) {
            this.filter = filter;
            this.processor = null;
        }

        private Default(Processor processor) {
            this.filter = null;
            this.processor = processor;
        }

        /**
         * @inheritDoc
         */
        @Override
        public <T> T sink(Class<T> type) {
            return Pump.create(type);
        }

        /**
         * @inheritDoc
         */
        @Override
        public <T> T sink(T t) {
            return processor == null ? t : processor.sink(t);
        }

        /**
         * @inheritDoc
         */
        @Override
        public <A extends Annotation> A note(A annotation, AnnotatedElement element) {
            return annotation;
        }

        /**
         * @inheritDoc
         */
        @Override
        public boolean should(Class<?> type, Object service) {
            if (filter == null) return !(service instanceof Ext.I);
            return filter.should(type, service);
        }
    }
}
