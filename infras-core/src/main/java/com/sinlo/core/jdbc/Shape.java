package com.sinlo.core.jdbc;

import com.sinlo.core.jdbc.util.Shapeherder;
import com.sinlo.core.jdbc.spec.Shaper;
import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;

import javax.lang.model.element.ElementKind;
import java.lang.annotation.*;

/**
 * Shape could annotate {@link Shaper shapers} to register them to the
 * {@link Shapeherder}
 *
 * @author sinlo
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Sponte(value = Shapeherder.class)
@Must(value = ElementKind.CLASS, extend = Shaper.class)
public @interface Shape {

    /**
     * The priority of the annotated {@link Shaper}. It helps to determine the proper
     * shaper when multiple shapers who shape to the same type are provided
     */
    int priority() default Integer.MIN_VALUE;
}
