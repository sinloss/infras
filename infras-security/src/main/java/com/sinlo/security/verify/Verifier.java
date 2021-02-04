package com.sinlo.security.verify;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Strine;
import com.sinlo.core.common.util.Xeger;
import com.sinlo.security.tkn.TknKeeper;
import com.sinlo.security.tkn.spec.State;
import com.sinlo.security.tkn.spec.Tkn;
import com.sinlo.security.tkn.spec.TknException;

import java.util.*;
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

    public static final String TYPE_ANY = "*";

    private final ThreadLocal<State<T, K, A>> state = new ThreadLocal<>();

    private final Map<String, Rule> rules = new HashMap<>();
    private final TknKeeper<T, K, A> tknKeeper;
    private boolean fallback;

    private Verifier(TknKeeper<T, K, A> tknKeeper) {
        this.tknKeeper = Objects.requireNonNull(
                tknKeeper, String.format("Must provide a proper %s", TknKeeper.class.getName()));
    }

    /**
     * Get a default policy instance with the "/" as its delim
     *
     * @see #of(TknKeeper, String)
     */
    public static <T, K, A> Verifier<T, K, A>.Policy of(TknKeeper<T, K, A> tknKeeper) {
        return of(tknKeeper, "/");
    }

    /**
     * Get a policy instance of the {@link Verifier} that is about to be create
     *
     * @param tknKeeper the {@link TknKeeper} which handles token authentication
     * @param delim     the delimiter of the paths to be handled
     * @see TknKeeper
     */
    public static <T, K, A> Verifier<T, K, A>.Policy of(TknKeeper<T, K, A> tknKeeper, String delim) {
        return new Verifier<>(tknKeeper).new Policy(delim);
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
        state.set(null);
        // get the rule for the current verify type
        Rule rule = rules.containsKey(type) ? rules.get(type) : rules.get(TYPE_ANY);
        if ((fallback && rule == null) || Funny.nvl(rule.should(path), fallback)) {
            try {
                // verify the token as per the demand of the chosen rule
                state.set(tknKeeper.stat(Tkn.ephemeral(token)).ephemeral);
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
        private static Verifier<?, ?, ?> v;

        /**
         * Set the verifier
         */
        public static <T, K, A> void set(Verifier<T, K, A> v) {
            Context.v = v;
        }

        /**
         * Get the verifier
         */
        @SuppressWarnings("unchecked")
        public static <T, K, A> Verifier<T, K, A> get() {
            return (Verifier<T, K, A>) v;
        }

        /**
         * Get the verified state if any
         */
        public static <T, K, A> Optional<State<T, K, A>> state() {
            if (v == null) return Optional.empty();
            return Context.<T, K, A>get().state();
        }
    }

    /**
     * The specification of the implementation of rules
     */
    public interface Rule {

        /**
         * Should the given path be verified for tokens
         */
        boolean should(String path);
    }

    /**
     * The policy originates rules
     */
    public class Policy {

        private final String delim;

        private final Map<String, Item> items = new HashMap<>();

        public Policy(String delim) {
            this.delim = delim;
        }

        /**
         * Build the policy starting from {@link #TYPE_ANY}
         *
         * @return {@link Policy.When}
         */
        public When whenAny() {
            return new When(TYPE_ANY);
        }

        /**
         * The policy {@link Item}s are configured here
         */
        public class When {
            private final String[] types;
            private final Item item = new Item();

            private When(String... types) {
                this.types = types;
            }

            /**
             * When path strings of current types match the given {@code exprs}
             */
            public When match(String... exprs) {
                Collections.addAll(item.exprs, exprs);
                return this;
            }

            /**
             * Except the given {@code excepts}
             */
            public When except(String... excepts) {
                Collections.addAll(item.excepts, excepts);
                return this;
            }

            /**
             * And... prepare to build another {@link Item}
             *
             * @return {@link And}
             */
            public And and() {
                set();
                return new And();
            }

            /**
             * Then...prepare to define what action should be taken when path strings match
             *
             * @return {@link Then}
             */
            public Then then() {
                set();
                return new Then();
            }

            // set policy items
            private void set() {
                Arrays.stream(types).forEach(t -> Policy.this.items.put(t, item));
            }
        }

        /**
         * Prepare to build another {@link Item}
         */
        public class And {

            /**
             * Prepare to build another {@link Item} regarding the given types of requests
             *
             * @see #verify(String, String, Object)
             */
            public When when(String... types) {
                // try to get the already set policy items
                String already = Stream.of(types)
                        .filter(rules.keySet()::contains)
                        .collect(Collectors.joining());
                if (Strine.nonEmpty(already)) {
                    throw new IllegalStateException(String.format(
                            "Already setup the ignore rule for request type(s) [ %s ]", already));
                }
                return new When(types);
            }
        }

        /**
         * Prepare to define what action should be taken when path strings match
         */
        public class Then {

            /**
             * Should pass the authentication verification when it matches
             */
            public Verifier<T, K, A> pass() {
                return set(false);
            }

            /**
             * Should do the authentication verification when it matches
             */
            public Verifier<T, K, A> verify() {
                return set(true);
            }

            // an abstraction of putting the rules into the Verifier.this.rules
            private Verifier<T, K, A> set(boolean should) {
                Verifier.this.fallback = !should;
                items.forEach((key, value) -> {
                    // merge with the rule for TYPE_ANY
                    if (!TYPE_ANY.equals(key)) {
                        value.merge(items.get(TYPE_ANY));
                    }
                    Verifier.this.rules.put(key, value.rule(should));
                });
                // set as the global verifier
                Context.set(Verifier.this);
                return Verifier.this;
            }
        }

        /**
         * The policy item
         */
        private class Item {
            // the expressions for matching
            private final List<String> exprs = new LinkedList<>();
            // the exceptions that results the different result against the exprs
            private final List<String> excepts = new LinkedList<>();

            /**
             * Produce a {@link Rule} based on the current policy item
             */
            private Rule rule(boolean should) {
                if (exprs.isEmpty())
                    // no expressions at all
                    return s -> should;

                Pattern e = Xeger.zip(delim, exprs);
                if (excepts.isEmpty())
                    // no exceptions
                    return s -> e.matcher(s).matches() == should;

                Pattern x = Xeger.zip(delim, excepts);
                // with exceptions
                return s -> (!x.matcher(s).matches() && e.matcher(s).matches()) == should;
            }

            // merge with another Item
            private void merge(Item another) {
                if (another == null) return;
                this.exprs.addAll(another.exprs);
                this.excepts.addAll(another.excepts);
            }
        }
    }

}
