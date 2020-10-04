package com.sinlo.core.jdbc;

import com.sinlo.core.jdbc.util.Sculptor;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Profile;

import java.lang.annotation.*;

/**
 * Mould a specific class by caching its {@link com.sinlo.core.prototype.Prototype}
 *
 * @author sinlo
 * @see Sculptor#onExplore(Profile)
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Sponte(value = Sculptor.class)
public @interface Mould {
}
