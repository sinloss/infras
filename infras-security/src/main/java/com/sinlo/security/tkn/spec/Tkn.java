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

    public static <T> Tkn<T> of(T ephemeral, T longevous) {
        return new Tkn<>(ephemeral, longevous);
    }

    /**
     * Map to another type of token
     *
     * @param <U> the other type
     */
    public <U> Tkn<U> map(Function<T, U> mapper) {
        return Tkn.of(mapper.apply(ephemeral),
                mapper.apply(longevous));
    }
}
