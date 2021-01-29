package com.sinlo.jdbc.spec;

import com.sinlo.core.common.util.Genericia;
import com.sinlo.jdbc.util.Jype;

import java.sql.ResultSet;

/**
 * The shaper who shapes a basic type defined in {@link Jype} into
 * a more database unrelated complex object
 *
 * @param <A> the database unrelated complex object
 * @param <T> the basic type defined in {@link Jype}
 * @author sinlo
 */
public interface Shaper<A, T> extends Genericia.Aware<A>, Genericia.Bware<T> {

    /**
     * Unshape the database unrelated complex {@link A object} back to the basic type
     * defined in {@link Jype}
     *
     * @see Jype#get(ResultSet, String, Class)
     */
    T unshape(A a);

    /**
     * The default method of shaping something into {@link A}
     *
     * @see Jype#get(ResultSet, String, Class)
     */
    A shape(T t, Class<A> c);

    /**
     * The proxy shaper with priority number
     */
    class Ranked<A, T> implements Shaper<A, T> {

        public final int priority;
        private final Shaper<A, T> underlying;

        public Ranked(int priority, Shaper<A, T> underlying) {
            this.priority = priority;
            this.underlying = underlying;
        }

        @Override
        public T unshape(A a) {
            return underlying.unshape(a);
        }

        @Override
        public A shape(T t, Class<A> c) {
            return underlying.shape(t, c);
        }
    }
}
