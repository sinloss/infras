package com.sinlo.security.verify;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Strine;
import com.sinlo.core.common.util.Xeger;
import com.sinlo.security.tkn.TknKeeper;
import com.sinlo.security.tkn.spec.State;
import com.sinlo.security.tkn.spec.Tkn;
import com.sinlo.security.tkn.spec.TknException;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The token verifier
 *
 * @param <T> the type of the raw token
 * @param <K> the type of the parsed token
 * @param <A> the type of the subject carried by the token
 */
public class Verifier<T, K, A> {

    private final ThreadLocal<Optional<State<T, K, A>>> state = new ThreadLocal<>();

    private final Map<String, Rule> ignored = new HashMap<>();
    private final TknKeeper<T, K, A> tknKeeper;

    private Verifier(TknKeeper<T, K, A> tknKeeper) {
        this.tknKeeper = Objects.requireNonNull(
                tknKeeper, String.format("Must provide a proper %s", TknKeeper.class.getName()));
    }

    /**
     * Get a ignore rules setting up instance of the {@link Verifier} that is about to be create
     *
     * @param tknKeeper the {@link TknKeeper} which handles token authentication
     * @see TknKeeper
     */
    public static <T, K, A> Verifier<T, K, A>.Regarding of(TknKeeper<T, K, A> tknKeeper) {
        return new Verifier<>(tknKeeper).new Regarding();
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
    public Optional<State<T, K, A>> verify(String type, String path, T token) throws VerificationFailure {
        // clear the maybe existing state due to the behaviour of the thread pool
        state.set(Optional.empty());
        // get the ignore rule for the current verify type
        Rule ignore = ignored.get(type);
        if (!Funny.nvl(ignore.apply(path), false)) {
            try {
                // verify the token if not ignored
                state.set(Optional.of(tknKeeper.stat(Tkn.ephemeral(token)).ephemeral));
            } catch (TknException e) {
                throw new VerificationFailure(e);
            }
        }
        return state.get();
    }

    /**
     * Get the verified {@link State}
     */
    public Optional<State<T, K, A>> state() {
        return state.get();
    }

    /**
     * The specification of the implementation of rules
     */
    public interface Rule extends Function<String, Boolean> {
    }

    /**
     * All ignore rule setup processes must be originated from here
     */
    public class Regarding {

        /**
         * Prepare to setup an ignore rule regarding the given types of requests
         *
         * @see #verify(String, String, Object)
         */
        public When when(String... types) {
            // try to get the already setup request types
            String already = Stream.of(types)
                    .filter(ignored.keySet()::contains)
                    .collect(Collectors.joining());
            if (Strine.nonEmpty(already)) {
                throw new IllegalStateException(String.format(
                        "Already setup the ignore rule for request type(s) [ %s ]", already));
            }
            return new When(types);
        }
    }

    /**
     * This can setup ignore rules
     */
    public class When {

        private final List<String> exprs = new LinkedList<>();
        private final String[] types;
        private String delim = "/";
        private Function<Matcher, Boolean> matching = Matcher::matches;

        private When(String[] types) {
            this.types = types;
        }

        /**
         * Any given expressions that...
         *
         * @see #matches()
         * @see #mismatches()
         */
        public When any(String... exprs) {
            Collections.addAll(this.exprs, exprs);
            return this;
        }

        /**
         * Any given expressions that...
         *
         * @see #matches()
         * @see #mismatches()
         */
        public When any(Collection<String> exprs) {
            this.exprs.addAll(exprs);
            return this;
        }

        /**
         * Partially, instead of the default fully matching manner
         */
        public When partially() {
            matching = Xeger::partiallyMatches;
            return this;
        }

        /**
         * Using the given delimiter to split the path of the requests
         */
        public When delimiter(String delim) {
            this.delim = delim;
            return this;
        }

        /**
         * That matches
         *
         * @see #any(String...)
         * @see #any(Collection)
         * @see Ignore#ignore()
         */
        public Ignore matches() {
            return new Ignore(true);
        }

        /**
         * That mismatch
         *
         * @see #any(String...)
         * @see #any(Collection)
         * @see Ignore#ignore()
         */
        public Ignore mismatches() {
            return new Ignore(false);
        }

        /**
         * To finally explicitly set those matches or mismatches as ignored
         */
        public class Ignore {

            private final boolean should;

            private Ignore(boolean should) {
                this.should = should;
            }

            /**
             * Just ignore
             */
            public Next ignore() {
                final Pattern pattern = Xeger.zip(delim, exprs);
                Stream.of(types).forEach(t -> Verifier.this.ignored.put(
                        t, s -> should == matching.apply(pattern.matcher(s))));
                return new Next();
            }
        }

    }

    /**
     * The next step after a serial of ignore rules being set up
     */
    public class Next {

        /**
         * Prepare to setup another ignore rule
         */
        public Regarding otherwise() {
            return new Regarding();
        }

        /**
         * No more setting up and create the {@link Verifier} already
         */
        public Verifier<T, K, A> create() {
            return Verifier.this;
        }
    }

}
