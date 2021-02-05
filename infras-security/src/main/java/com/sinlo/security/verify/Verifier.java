package com.sinlo.security.verify;

import com.sinlo.core.common.util.Try;
import com.sinlo.security.tkn.TknKeeper;
import com.sinlo.security.tkn.spec.State;
import com.sinlo.security.tkn.spec.Subject;
import com.sinlo.security.tkn.spec.Tkn;
import com.sinlo.security.tkn.spec.TknException;
import com.sinlo.security.verify.spec.NotAllowed;
import com.sinlo.security.verify.spec.Rule;
import com.sinlo.security.verify.spec.VerificationFailure;

import java.util.*;

/**
 * The token verifier
 *
 * @param <T> the type of the raw token
 * @param <K> the type of the parsed token
 * @param <A> the type of the subject carried by the token
 */
public class Verifier<T, K, A extends Subject> extends Policy.Rules {

    private final ThreadLocal<State<T, K, A>> state = new ThreadLocal<>();

    private final TknKeeper<T, K, A> tknKeeper;
    private final boolean fallback;

    private Verifier(Map<String, Rule> rules, TknKeeper<T, K, A> tknKeeper, boolean fallback) {
        super(rules);
        this.tknKeeper = Objects.requireNonNull(
                tknKeeper, String.format("Must provide a proper %s", TknKeeper.class.getName()));
        this.fallback = fallback;
    }

    /**
     * Get a default policy instance with the "/" as its delim
     *
     * @see #of(TknKeeper, String)
     */
    public static <T, K, A extends Subject> Policy<Then<T, K, A>> of(TknKeeper<T, K, A> tknKeeper) {
        return of(tknKeeper, "/");
    }

    /**
     * Get a policy instance of the {@link Verifier} that is about to be create
     *
     * @param tknKeeper the {@link TknKeeper} which handles token authentication
     * @param delim     the delimiter of the paths to be handled
     * @see TknKeeper
     */
    public static <T, K, A extends Subject> Policy<Then<T, K, A>> of(TknKeeper<T, K, A> tknKeeper, String delim) {
        return new Policy<>(delim, c -> new Then<>(c, tknKeeper));
    }

    /**
     * Verify a specific {@code type} of request identified by the given {@code path} using the given
     * {@code token}
     *
     * @param type  the type of the request
     * @param path  the path identifying the request
     * @param token the authentication token
     * @return the verified result represented by the {@link State}
     */
    public Optional<State<T, K, A>> verify(String type, String path, T token) throws VerificationFailure, NotAllowed {
        // clear the maybe existing state due to the behaviour of the thread pool
        state.set(null);
        // check if should verify
        if (Try.of(() -> check(type, path))
                .caught(NoRule.class).then(fallback)
                .exert()) {
            try {
                // verify the token as per the demand of the chosen rule
                State<T, K, A> sta = tknKeeper.stat(Tkn.ephemeral(token)).ephemeral;
                state.set(sta);
                if (!sta.subject.allow(path)) {
                    throw new NotAllowed(path);
                }
            } catch (TknException e) {
                throw new VerificationFailure(e);
            }
        }
        return Optional.ofNullable(state.get());
    }

    /**
     * Get the verified {@link State}
     */
    public Optional<State<T, K, A>> state() {
        return Optional.ofNullable(state.get());
    }

    /**
     * The single global context of verifier and state, the global verifier by default would
     * be the latest created {@link Verifier}, but it can be changed explicitly by using the
     * {@link Context#set(Verifier)}
     */
    public static class Context {
        private static Verifier<?, ?, ? extends Subject> v;

        /**
         * Set the verifier
         */
        public static <T, K, A extends Subject> void set(Verifier<T, K, A> v) {
            Context.v = v;
        }

        /**
         * Get the verifier
         */
        @SuppressWarnings("unchecked")
        public static <T, K, A extends Subject> Verifier<T, K, A> get() {
            return (Verifier<T, K, A>) v;
        }

        /**
         * Get the verified state if any
         */
        public static <T, K, A extends Subject> Optional<State<T, K, A>> state() {
            if (v == null) return Optional.empty();
            return Context.<T, K, A>get().state();
        }
    }

    /**
     * Prepare to define what action should be taken when path strings match
     */
    public static class Then<T, K, A extends Subject> {

        private final Policy<Then<T, K, A>>.RuleCrafter crafter;
        private final TknKeeper<T, K, A> tknKeeper;

        private Then(Policy<Then<T, K, A>>.RuleCrafter crafter, TknKeeper<T, K, A> tknKeeper) {
            this.crafter = crafter;
            this.tknKeeper = tknKeeper;
        }

        /**
         * Should pass the authentication verification when it matches
         */
        public Verifier<T, K, A> pass() {
            return new Verifier<>(crafter.craft(false), tknKeeper, true);
        }

        /**
         * Should do the authentication verification when it matches
         */
        public Verifier<T, K, A> verify() {
            return new Verifier<>(crafter.craft(true), tknKeeper, false);
        }

    }

}
