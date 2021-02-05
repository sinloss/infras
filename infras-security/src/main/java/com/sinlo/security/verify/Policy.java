package com.sinlo.security.verify;

import com.sinlo.core.common.util.Strine;
import com.sinlo.core.common.util.Xeger;
import com.sinlo.security.verify.spec.Rule;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The policy that originates rules
 *
 * @author sinlo
 */
public class Policy<T> {

    public static final String TYPE_ANY = "*";

    private final String delim;

    private final Map<String, Policy<T>.Item> items = new HashMap<>();
    private final Function<RuleCrafter, T> finisher;

    /**
     * {@link #general(String)}("/")
     */
    public static Policy<Rules.Then> general() {
        return new Policy<>("/", Rules.Then::new);
    }

    /**
     * Create a {@link Policy} for {@link Rules}
     *
     * @see #Policy(String, Function)
     */
    public static Policy<Rules.Then> general(String delim) {
        return new Policy<>(delim, Rules.Then::new);
    }

    /**
     * @param delim    the delimiter of the requesting paths
     * @param finisher the function that can accept a {@link RuleCrafter} and supply the final object
     *                 {@link T}
     */
    public Policy(String delim, Function<Policy<T>.RuleCrafter, T> finisher) {
        this.delim = delim;
        this.finisher = Objects.requireNonNull(finisher,
                "How could it be possible to finish the creation without the finisher provided");
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
     * A general rule checker
     */
    public static class Rules {

        private final Map<String, Rule> rules;

        public Rules(Map<String, Rule> rules) {
            this.rules = rules;
        }

        /**
         * Check the given path of a specific type
         *
         * @param type the rule type
         * @param path the path for test
         * @return check passed or not
         * @throws NoRule if there's no rule could be found for the given {@code type}
         */
        public boolean check(String type, String path) throws NoRule {
            // get the rule for the current rule type
            Rule rule = rules.containsKey(type)
                    ? rules.get(type) : rules.get(TYPE_ANY);
            if (rule == null) {
                throw new NoRule(type);
            }
            return rule.should(path);
        }

        /**
         * The {@link Rules} checker finisher
         */
        public static class Then {

            private final Policy<Then>.RuleCrafter crafter;

            private Then(Policy<Then>.RuleCrafter crafter) {
                this.crafter = crafter;
            }

            /**
             * Pass the check if the assigned rules match
             */
            public Rules pass() {
                return new Rules(crafter.craft(true));
            }

            /**
             * Not pass the check if the assigned rules match
             */
            public Rules reject() {
                return new Rules(crafter.craft(false));
            }
        }

        /**
         * No rule is related to the specific type
         */
        public static class NoRule extends Exception {

            public final String type;

            public NoRule(String type) {
                super(String.format(
                        "No rule is related to the specific type [ %s ]", type));
                this.type = type;
            }
        }
    }

    /**
     * The policy {@link Policy.Item}s are configured here
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
         * And... prepare to build another {@link Policy.Item}
         *
         * @return {@link Policy.And}
         */
        public And and() {
            set();
            return new And();
        }

        /**
         * Then...prepare to define what action should be taken when path strings match
         *
         * @return {@link T}
         */
        public T then() {
            set();
            return finisher.apply(new RuleCrafter());
        }

        // set policy items
        private void set() {
            Arrays.stream(types).forEach(t -> Policy.this.items.put(t, item));
        }
    }

    /**
     * The crafter who crafts rules from the {@link Policy.Item}s
     */
    public class RuleCrafter {

        public Map<String, Rule> craft(boolean should) {
            return items.entrySet().stream().map(e -> {
                String key = e.getKey();
                Item value = e.getValue();
                // merge with the rule for TYPE_ANY
                if (!TYPE_ANY.equals(key)) {
                    value.merge(items.get(TYPE_ANY));
                }
                return new AbstractMap.SimpleEntry<>(key, value.rule(should));
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    /**
     * Prepare to build another {@link Policy.Item}
     */
    public class And {

        /**
         * Prepare to build another {@link Policy.Item} regarding the given types of requests
         */
        public When when(String... types) {
            // try to get the already set policy items
            String already = Stream.of(types)
                    .filter(items.keySet()::contains)
                    .collect(Collectors.joining());
            if (Strine.nonEmpty(already)) {
                throw new IllegalStateException(String.format(
                        "Already setup the ignore rule for request type(s) [ %s ]", already));
            }
            return new When(types);
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

