package com.sinlo.security.jwt.nimbus.spec;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Try;
import com.sinlo.core.common.wraparound.Cascader;
import com.sinlo.core.http.Fetcha;
import com.sinlo.security.jwt.spec.exception.BadJwtException;

import java.net.URL;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The processor builder abstraction
 *
 * @author sinlo
 */
public abstract class ProcessorBuilder {

    protected final Set<JWSAlgorithm> algorithms = new HashSet<>();

    public abstract JWSKeySelector<SecurityContext> selector();

    /**
     * Create a {@link ProcessorBuilder.SingleKey} using the given {@link Key}
     */
    public static <K extends Key> ProcessorBuilder.SingleKey<K> key(K key) {
        return new SingleKey<>(key);
    }

    /**
     * Create a {@link ProcessorBuilder.JwkSet} using the given {@code uri}
     */
    public static ProcessorBuilder.JwkSet uri(String uri) {
        return new JwkSet(uri);
    }

    public JWTProcessor<SecurityContext> processor() {
        return Cascader.of(DefaultJWTProcessor::new)
                .apply(DefaultJWTProcessor::setJWSKeySelector, selector())
                .peek(t -> t.setJWTClaimsSetVerifier((claims, context) -> {
                })).get();
    }

    /**
     * Add an algorithm
     */
    public ProcessorBuilder alg(JWSAlgorithm algorithm) {
        this.algorithms.add(algorithm);
        return this;
    }

    /**
     * Get a single algorithm by select the first algorithm out of the assigned algorithms
     * or {@link JWSAlgorithm#RS256} if none
     */
    protected JWSAlgorithm singleAlgorithm() {
        if (algorithms.isEmpty()) {
            return JWSAlgorithm.RS256;
        }
        return algorithms.iterator().next();
    }

    /**
     * A general single keyed {@link ProcessorBuilder}
     *
     * @param <K> the type of key
     */
    public static class SingleKey<K extends Key> extends ProcessorBuilder {

        private final K key;

        public SingleKey(K key) {
            this.key = key;
        }

        @Override
        public JWSKeySelector<SecurityContext> selector() {
            return new Selector<>(singleAlgorithm(), key);
        }

        /**
         * An implementation of {@link JWSKeySelector} with a single key
         */
        public static class Selector<C extends SecurityContext> implements JWSKeySelector<C> {

            private final JWSAlgorithm expected;
            private final List<Key> keys;

            public Selector(JWSAlgorithm expected, Key key) {
                this.expected = Objects.requireNonNull(expected, "The expected Jws algorithm is mandatory");
                this.keys = Collections.singletonList(Objects.requireNonNull(key, "The key is mandatory"));
            }

            @Override
            public List<? extends Key> selectJWSKeys(JWSHeader header, C context) throws KeySourceException {
                if (!this.expected.equals(header.getAlgorithm())) {
                    throw BadJwtException.unsupported(header.getAlgorithm());
                }
                return this.keys;
            }
        }
    }

    /**
     * The jwk set processor builder
     */
    public static class JwkSet extends ProcessorBuilder {

        private final String uri;
        private Fetcha.Course<String> course;

        public JwkSet(String uri) {
            this.uri = uri;
        }

        public JwkSet course(Fetcha.Course<String> course) {
            this.course = course;
            return this;
        }

        @Override
        public JWSKeySelector<SecurityContext> selector() {
            if (course == null) course = Fetcha.Course.simple();
            RemoteJWKSet<SecurityContext> source = new RemoteJWKSet<>(Try.of(() -> new URL(uri))
                    .otherwiseThrow().exert(), new RemoteRetriever(course));
            if (this.algorithms.size() <= 1) {
                return new JWSVerificationKeySelector<>(singleAlgorithm(), source);
            }
            return new MapSelector<>(this.algorithms.stream()
                    .collect(Collectors.toMap(Funny::identity,
                            alg -> new JWSVerificationKeySelector<>(alg, source))));
        }

        /**
         * An implementation that select {@link JWSKeySelector} from a map of selectors
         */
        public static class MapSelector<C extends SecurityContext> implements JWSKeySelector<C> {

            private final Map<JWSAlgorithm, JWSKeySelector<C>> selectors;

            public MapSelector(Map<JWSAlgorithm, JWSKeySelector<C>> jwsKeySelectors) {
                this.selectors = jwsKeySelectors;
            }

            @Override
            public List<? extends Key> selectJWSKeys(JWSHeader header, C context) throws KeySourceException {
                JWSKeySelector<C> sel = this.selectors.get(header.getAlgorithm());
                if (sel == null) {
                    throw BadJwtException.unsupported(header.getAlgorithm());
                }
                return sel.selectJWSKeys(header, context);
            }

        }
    }
}
