package com.sinlo.security.tkn.spec;

/**
 * The knowledge of how to get a {@link State} from a token and how to create a token
 * of a specific lifespan
 *
 * @param <T> the type of the raw token
 * @param <K> the type of the parsed token
 * @param <A> the type of the subject
 * @author sinlo
 */
public interface Knowledge<T, K, A> {

    /**
     * Get the state of the given token {@code t}
     */
    State<T, K, A> stat(T t);

    /**
     * Create a token based on the given {@code lifespan} and {@code subject}
     */
    T create(Long lifespan, A subject);
}
