package com.sinlo.core.common.util;

import com.sinlo.core.common.functional.*;

import java.util.function.*;

/**
 * Funny the function util
 * <br/><strike>Funny is the most funny and functional name, ever!</strike>
 *
 * @author sinlo
 */
public class Funny {

    /**
     * An identity function {@code t -> t}
     */
    public static <T> T identity(T t) {
        return t;
    }

    /**
     * A function that returns null
     */
    public static <T, R> R nil(T t) {
        return null;
    }

    /**
     * A function that returns true
     */
    public static <T> boolean aye(T t) {
        return true;
    }

    /**
     * A function that returns false
     */
    public static <T> boolean nay(T t) {
        return false;
    }

    /**
     * A maybe that treat null like an identity function does and treat presented
     * value with the given custom function
     */
    public static <T, R> R maybe(T t, Function<T, R> ifNotNull) {
        if (t == null) return null;
        return ifNotNull.apply(t);
    }

    /**
     * Reference to the function that casts to a specific type
     *
     * @param as   the {@link Class} of the specific type
     * @param <T>  the original type
     * @param <AS> the specific type
     */
    public static <T, AS> Function<T, AS> cast(Class<AS> as) {
        return Funny::cast;
    }

    /**
     * Cast to a specific type
     *
     * @param t    the original object
     * @param <T>  the original type
     * @param <AS> the specific type
     */
    public static <T, AS> AS cast(T t, Class<AS> as) {
        return cast(t);
    }

    /**
     * Cast to a left side implied type
     *
     * @param t    the original object
     * @param <T>  the original type
     * @param <AS> the left side implied type
     */
    @SuppressWarnings("unchecked")
    public static <T, AS> AS cast(T t) {
        return (AS) t;
    }

    /**
     * Convert the given {@link Consumer} to a valid {@link Function} having no
     * return value. It is mainly used to meet the requirements of some methods
     * which only accepts {@link Function}s
     */
    public static <T> Function<T, Void> voided(Consumer<T> proc) {
        return (t) -> {
            proc.accept(t);
            return null;
        };
    }

    /**
     * Opposite to the {@link #voided(Consumer)}, this will convert the given
     * {@link Consumer} to a valid {@link Function} that returns the given
     * {@code next} which might be the caller in case of method referencing
     */
    public static <T, A> Function<A, T> cascade(Consumer<A> voided, T next) {
        return a -> {
            voided.accept(a);
            return next;
        };
    }

    /**
     * Similar to {@link #cascade(Consumer, Object)}, but binds an argument, and return
     * a identity function that can return what it applied on which might be the caller
     * in case of method referencing
     */
    public static <T, A> Function<T, T> cascade(BiConsumer<T, A> voided, A arg) {
        return t -> {
            voided.accept(t, arg);
            return t;
        };
    }

    /**
     * Binds an argument to the given {@link BiConsumer} and produce a simple {@link Consumer}
     * that only accepts the first argument which might be the caller in case of method referencing
     */
    public static <T, A> Consumer<T> bind(BiConsumer<T, A> bi, A a) {
        return t -> bi.accept(t, a);
    }

    /**
     * Binds an argument to the given {@link Consumer} and produce a simple {@link Runnable}
     */
    public static <T> Runnable bind(Consumer<T> cons, T t) {
        return () -> cons.accept(t);
    }

    /**
     * Similar to {@link #bind(BiConsumer, Object)} but binds all arguments
     */
    public static <T, A> Runnable bind(BiConsumer<T, A> bi, T t, A a) {
        return () -> bi.accept(t, a);
    }

    /**
     * Binds 2 arguments to the given {@link TriConsumer} and produce a simple {@link Consumer}
     * that only accepts the first argument which might be the caller in case of method referencing
     */
    public static <T, A, B> Consumer<T> bind(TriConsumer<T, A, B> tri, A a, B b) {
        return t -> tri.accept(t, a, b);
    }

    /**
     * Similar to {@link #bind(TriConsumer, Object, Object)} but binds all arguments
     */
    public static <T, A, B> Runnable bind(TriConsumer<T, A, B> tri, T t, A a, B b) {
        return () -> tri.accept(t, a, b);
    }

    /**
     * Binds an argument to the given {@link BiFunction} and produce a simple {@link Function}
     * that only accepts the first argument which might be the caller in case of method referencing
     */
    public static <T, A, R> Function<T, R> bind(BiFunction<T, A, R> bi, A a) {
        return t -> bi.apply(t, a);
    }

    /**
     * Binds an  argument to the given {@link Function} and produce a simple {@link Supplier}
     */
    public static <T, R> Supplier<R> bind(Function<T, R> func, T t) {
        return () -> func.apply(t);
    }

    /**
     * Similar to {@link #bind(BiFunction, Object)} but binds all arguments
     */
    public static <T, A, R> Supplier<R> bind(BiFunction<T, A, R> bi, T t, A a) {
        return () -> bi.apply(t, a);
    }

    /**
     * Binds an argument to the given {@link ImpatientBiConsumer} and produce a simple {@link ImpatientConsumer}
     * that only accepts the first argument which might be the caller in case of method referencing
     */
    public static <T, A, E extends Throwable> ImpatientConsumer<T, E> bind(ImpatientBiConsumer<T, A, E> bi, A a) {
        return t -> bi.consume(t, a);
    }

    /**
     * Binds an argument to the given {@link ImpatientConsumer} and produce a simple {@link ImpatientRunnable}
     */
    public static <T, E extends Throwable> ImpatientRunnable<E> bind(ImpatientConsumer<T, E> cons, T t) {
        return () -> cons.consume(t);
    }

    /**
     * Similar to {@link #bind(ImpatientBiConsumer, Object)} but binds all arguments
     */
    public static <T, A, E extends Throwable> ImpatientRunnable<E> bind(ImpatientBiConsumer<T, A, E> bi, T t, A a) {
        return () -> bi.consume(t, a);
    }

    /**
     * Binds 2 arguments to the given {@link ImpatientTriConsumer} and produce a simple {@link ImpatientConsumer}
     * that only accepts the first argument which might be the caller in case of method referencing
     */
    public static <T, A, B, E extends Throwable> ImpatientConsumer<T, E> bind(ImpatientTriConsumer<T, A, B, E> tri, A a, B b) {
        return t -> tri.consume(t, a, b);
    }

    /**
     * Similar to {@link #bind(ImpatientTriConsumer, Object, Object)} but binds all arguments
     */
    public static <T, A, B, E extends Throwable> ImpatientRunnable<E> bind(ImpatientTriConsumer<T, A, B, E> tri, T t, A a, B b) {
        return () -> tri.consume(t, a, b);
    }

    /**
     * Binds an argument to the given {@link BiFunction} and produce a simple {@link Function}
     * that only accepts the first argument which might be the caller in case of method referencing
     */
    public static <T, A, R, E extends Throwable> ImpatientFunction<T, R, E> bind(ImpatientBiFunction<T, A, R, E> bi, A a) {
        return t -> bi.employ(t, a);
    }

    /**
     * Binds an  argument to the given {@link Function} and produce a simple {@link Supplier}
     */
    public static <T, R, E extends Throwable> ImpatientSupplier<R, E> bind(ImpatientFunction<T, R, E> func, T t) {
        return () -> func.employ(t);
    }

    /**
     * Similar to {@link #bind(BiFunction, Object)} but binds all arguments
     */
    public static <T, A, R, E extends Throwable> ImpatientSupplier<R, E> bind(ImpatientBiFunction<T, A, R, E> bi, T t, A a) {
        return () -> bi.employ(t, a);
    }


    /**
     * An equivalent of the nvl function in Oracle/PLSQL
     */
    public static <T> T nvl(T nullable, Supplier<T> ifNull) {
        return nullable == null ? ifNull.get() : nullable;
    }

    /**
     * Similar to {@link #nvl(Object, Supplier)} but directly accept the fallback
     * value
     */
    public static <T> T nvl(T nullable, T fallback) {
        return nullable == null ? fallback : nullable;
    }
}
