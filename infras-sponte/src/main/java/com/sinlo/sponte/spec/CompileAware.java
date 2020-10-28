package com.sinlo.sponte.spec;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.core.Pri;

import java.lang.annotation.*;

/**
 * Prepare things at compile time
 *
 * @author sinlo
 */
@FunctionalInterface
public interface CompileAware {

    Pri<CompileAware> Pri = new Pri<>();

    void onCompile(Context.Subject cs);

    /**
     * Skip the {@link Sponte#compiling()} on and only on the target annotated by this
     * annotation. It is very useful when the assigned {@link CompileAware} class is in the
     * same code base with the {@link Sponte} annotated target as the {@link CompileAware} is
     * not really compiled in the annotation processing process
     */
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Neglect {
    }
}
