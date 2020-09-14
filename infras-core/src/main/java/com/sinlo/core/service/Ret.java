package com.sinlo.core.service;

import java.util.function.Consumer;
import java.util.function.Function;

public final class Ret<V, E> {

    private final V val;
    private final E err;

    private Ret(V val, E err) {
        this.val = val;
        this.err = err;
    }

    public static <V, E> Ret<V, E> ok(V val) {
        return new Ret<>(val, null);
    }

    public static <V, E> Ret<V, E> err(E err) {
        return new Ret<>(null, err);
    }

    public Ret<V, E> okThen(Consumer<V> consumer) {
        if (valid()) {
            consumer.accept(val);
        }
        return this;
    }

    public Ret<V, E> okThen(Function<V, Ret<V, E>> function) {
        return valid() ? function.apply(val) : this;
    }

    public Ret<V, E> okThenMap(Function<V, V> function) {
        return valid() ? Ret.ok(function.apply(val)) : Ret.err(err);
    }

    public Ret<V, E> orElse(Consumer<E> consumer) {
        if (erred()) {
            consumer.accept(err);
        }
        return this;
    }

    public Ret<V, E> orElse(Function<E, Ret<V, E>> function) {
        return valid() ? this : function.apply(err);
    }

    public Ret<V, E> orElseMap(Function<E, E> function) {
        return valid() ? Ret.ok(val) : Ret.err(function.apply(err));
    }

    public V orElseGet(Function<E, V> function) {
        return valid() ? val : function.apply(err);
    }

    /**
     * check if val is presented
     */
    public boolean valid() {
        return val != null;
    }

    /**
     * check if err has occurred
     */
    public boolean erred() {
        return err != null;
    }

    public V getVal() {
        return val;
    }

    public E getErr() {
        return err;
    }

}
