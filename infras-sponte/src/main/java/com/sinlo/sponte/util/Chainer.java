package com.sinlo.sponte.util;

import java.util.*;

/**
 * Collection chainer
 *
 * @param <E> the type of element
 * @param <T> the type of collection
 * @author sinlo
 */
public class Chainer<E, T extends Collection<E>> implements Collection<E> {
    private final T t;

    private Chainer(T t) {
        this.t = t;
    }

    public static <E, T extends Collection<E>> Chainer<E, T> of(T t) {
        return new Chainer<>(t);
    }

    @SafeVarargs
    public static <E> Chainer<E, List<E>> of(E... e) {
        List<E> t = new ArrayList<>();
        Collections.addAll(t, e);
        return Chainer.of(t);
    }

    @SafeVarargs
    public final Chainer<E, T> and(E... others) {
        Collections.addAll(t, others);
        return this;
    }

    public Chainer<E, T> and(Collection<E> others) {
        t.addAll(others);
        return this;
    }

    @Override
    public int size() {
        return t.size();
    }

    @Override
    public boolean isEmpty() {
        return t.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return t.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return t.iterator();
    }

    @Override
    public Object[] toArray() {
        return t.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <A> A[] toArray(A[] a) {
        return t.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return t.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return t.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return t.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return t.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return t.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return t.retainAll(c);
    }

    @Override
    public void clear() {
        t.clear();
    }
}
