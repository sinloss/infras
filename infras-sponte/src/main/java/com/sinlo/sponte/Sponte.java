package com.sinlo.sponte;

import com.sinlo.sponte.core.Spontaneously;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.SponteAware;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.*;

/**
 * The main annotation who introduces the processing from {@link Spontaneously}
 *
 * @author sinlo
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sponte {

    String NAME = "com.sinlo.sponte.Sponte";

    String SPONTE_ROOT = "META-INF/spontaneously";

    String SPONTED = ".sponted";

    String LOCK = ".lock";

    /**
     * The {@link SponteAware}
     */
    Class<? extends SponteAware> value() default SponteAware.class;

    /**
     * The key of instances of the {@link #value()} and the {@link #compiling()}. When only
     * one key is supplied, they would share the key, otherwise the {@link #value()} would
     * use the first key and the {@link #compiling()} the second.
     * <p/>
     * Different element annotated with the same {@code key} will share the same instance
     * of the {@link #value()} and the {@link #compiling()} respectively
     */
    String[] key() default Keys.DEFAULT;

    /**
     * Call the {@link CompileAware#onCompile(ProcessingEnvironment, Class, Element)} method on
     * compile. Note that the given {@link CompileAware compiling} itself should be already
     * compiled before the compiling of which on it the {@link Sponte} is annotated
     */
    Class<? extends CompileAware> compiling() default CompileAware.class;

    boolean spread() default false;

    class Keys {

        public static final String DEFAULT = "";

        public static String get(Sponte sponte, Class<?> which) {
            String[] keys = sponte.key();
            switch (keys.length) {
                case 0:
                    return DEFAULT;
                case 1:
                    return keys[0];
                default:
                    return keys[SponteAware.class.isAssignableFrom(which) ? 0 : 1];
            }
        }
    }
}
