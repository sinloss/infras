package com.sinlo.core.service;

import com.sinlo.sponte.Sponte;

import java.lang.annotation.*;

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
public @interface Ponded {
}
