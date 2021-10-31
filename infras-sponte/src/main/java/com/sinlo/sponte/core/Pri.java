package com.sinlo.sponte.core;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.util.function.Supplier;

/**
 * Private pool for {@link CompileAware} and {@link SponteAware} with its method
 * {@link #get(Class, String, Supplier)} access limited inside the same package
 *
 * @author sinlo
 */
public class Pri<T> {

    private final Pool.Simple<T> pool = new Pool.Simple<>();

    /**
     * Get an instance of {@link T} using the {@link com.sinlo.sponte.Sponte.Keys#DEFAULT default key}
     */
    public T get(Class<? extends T> c) {
        return get(c, Sponte.Keys.DEFAULT);
    }

    /**
     * Get an instance of {@link T} using the given key
     */
    public T get(Class<? extends T> c, String key) {
        return pool.get(key(c, key));
    }

    /**
     * Get an instance of {@link T} using the given key, and supply a new instance if none present
     */
    public T get(Class<? extends T> c, String key, Supplier<T> ifNone) {
        return pool.get(key(c, key), ifNone);
    }

    /**
     * Get an instance of {@link T} base on the given {@link com.sinlo.sponte.Sponte sponte}
     */
    public T get(Sponte sponte, Class<? extends T> c) {
        return get(c, Sponte.Keys.get(sponte, c), () -> Typer.create(c));
    }

    /**
     * Get the {@link #pool} key of the given class
     */
    public static String key(Class<?> c, String key) {
        return c.getName().concat("@").concat(key);
    }
}
