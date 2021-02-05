package com.sinlo.security.tkn.spec;

/**
 * The state of a token
 *
 * @param <T> the type of the raw token
 * @param <K> the type of the parsed token
 * @param <A> the type of the subject
 * @author sinlo
 */
public class State<T, K, A extends Subject> {

    /**
     * The raw, non-modified token
     */
    public final T raw;

    /**
     * The parsed token
     */
    public final K token;

    /**
     * The deserialized subject carried by the token
     */
    public final A subject;

    /**
     * The milliseconds of the expiration time
     */
    public final long expire;

    private State(T raw, K token, A subject, long expire) {
        this.raw = raw;
        this.token = token;
        this.subject = subject;
        this.expire = expire;
    }

    public static <T, K, A extends Subject> State<T, K, A> of(T raw, K token, A subject, long expire) {
        return new State<>(raw, token, subject, expire);
    }

}
