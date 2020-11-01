package com.sinlo.security.tkn.spec;

/**
 * The state of a token
 *
 * @param <T> the type of tokens
 * @param <A> the type of subject
 * @author sinlo
 */
public class State<T, A> {

    public final A subject;

    public final Long expire;

    private State(A subject, Long expire) {
        this.subject = subject;
        this.expire = expire;
    }

    public static <T, A> State<T, A> of(A subject, Long expire) {
        return new State<>(subject, expire);
    }

}
