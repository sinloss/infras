package com.sinlo.sponte.core;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;

import java.util.function.Supplier;

/**
 * Private pool for {@link CompileAware} and {@link SponteAware} with its method
 * {@link #get(Class, String, Supplier)} access limited inside of the same package
 *
 * @author sinlo
 */
public class Pri<T> {

    private final Pool.Simple<T> pool = new Pool.Simple<>();

    public T get(Class<? extends T> c) {
        return get(c, Sponte.Keys.DEFAULT);
    }

    public T get(Class<? extends T> c, String key) {
        return pool.get(key(c, key));
    }

    T get(Class<? extends T> c, String key, Supplier<T> ifNone) {
        return pool.get(key(c, key), ifNone);
    }

    public static String key(Class<?> c, String key) {
        return c.getName().concat("@").concat(key);
    }
}
