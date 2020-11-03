package com.sinlo.security.tkn.spec;

import java.util.function.Function;

/**
 * The compound token object containing an ephemeral token and a longevous
 * token
 *
 * @param <T> the type of tokens
 * @author sinlo
 */
public class Tkn<T> {

    /**
     * The ephemeral token used for authentication
     */
    public final T ephemeral;
    /**
     * The longevous token used for renew the ephemeral token
     */
    public final T longevous;

    private Tkn(T ephemeral, T longevous) {
        this.ephemeral = ephemeral;
        this.longevous = longevous;
    }

    /**
     * Create a {@link Tkn} without any check
     */
    public static <T> Tkn<T> of(T ephemeral, T longevous) {
        return new Tkn<>(ephemeral, longevous);
    }

    /**
     * Create a {@link Tkn} with only a {@link #ephemeral} token, it will throw a
     * {@link TknException.Null} if the given ephemeral token is null
     */
    public static <T> Tkn<T> ephemeral(T ephemeral) {
        if (ephemeral == null)
            throw new TknException.Null();
        return of(ephemeral, null);
    }

    /**
     * Create a {@link Tkn} with only a {@link #longevous} token, it will throw a
     * {@link TknException.Null} if the given longevous token is null
     */
    public static <T> Tkn<T> longevous(T longevous) {
        if (longevous == null)
            throw new TknException.Null();
        return of(null, longevous);
    }

    /**
     * Create a {@link Tkn} with both {@link #ephemeral} and {@link #longevous} tokens, it will
     * throw a {@link TknException.Null} if any one of the given tokens is null
     */
    public static <T> Tkn<T> both(T ephemeral, T longevous) {
        if (ephemeral == null || longevous == null)
            throw new TknException.Null();
        return of(ephemeral, longevous);
    }

    /**
     * Map to another type of token
     *
     * @param <U> the other type
     */
    public <U> Tkn<U> map(Function<T, U> mapper) {
        return Tkn.of(ephemeral == null ? null : mapper.apply(ephemeral),
                longevous == null ? null : mapper.apply(longevous));
    }
}
