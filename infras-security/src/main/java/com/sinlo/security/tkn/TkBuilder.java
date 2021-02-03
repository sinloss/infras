package com.sinlo.security.tkn;

import com.sinlo.core.common.functional.ImpatientFunction;
import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.wraparound.Lazy;
import com.sinlo.security.jwt.Jwter;
import com.sinlo.security.jwt.nimbus.NimbusScheme;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.JwtException;
import com.sinlo.security.tkn.knowledges.JwtKnowledge;
import com.sinlo.security.tkn.spec.Knowledge;
import com.sinlo.security.tkn.spec.Tkn;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * The builder of the {@link TknKeeper}
 *
 * @param <T> the type of type param {@code T} of {@link TknKeeper}
 * @param <A> the type of type param {@code A} of {@link TknKeeper}
 * @author sinlo
 */
@SuppressWarnings("JavadocReference")
public class TkBuilder<T, A> {

    private Long ephemeral;
    private Long longevous;
    private Long transition;

    private TkBuilder() {
    }

    /**
     * Create a {@link TkBuilder} of the given token and subject type
     *
     * @param tc the type of the token of the finally built {@link TknKeeper}
     * @param ac the type of the subject of the finally built {@link TknKeeper}
     */
    public static <T, K, A> TkBuilder<T, A> of(Class<T> tc, Class<A> ac) {
        return new TkBuilder<>();
    }

    /**
     * The lifespan of the ephemeral token
     *
     * @see TknKeeper#lifespan
     */
    public TkBuilder<T, A> ephemeral(long ephemeral) {
        this.ephemeral = ephemeral;
        return this;
    }

    /**
     * The lifespan of the longevous token
     *
     * @see TknKeeper#lifespan
     */
    public TkBuilder<T, A> longevous(long longevous) {
        this.longevous = longevous;
        return this;
    }

    /**
     * @see TknKeeper#transition
     */
    public TkBuilder<T, A> transition(long transition) {
        this.transition = transition;
        return this;
    }

    /**
     * Use a {@link JwtBuilder} that produces a {@link FinalBuilder}
     */
    public JwtBuilder jwt() {
        return new JwtBuilder();
    }

    /**
     * The final builder that finally builds the {@link TknKeeper}
     */
    public class FinalBuilder<K> {

        private final KnowledgeBuilder<T, K, A> kb;

        private FinalBuilder(KnowledgeBuilder<T, K, A> kb) {
            this.kb = kb;
        }

        /**
         * Finally build
         */
        public TknKeeper<T, K, A> build() {
            if (ephemeral == null) ephemeral = 7_200_000L;
            if (longevous == null) longevous = ephemeral * 84;
            if (transition == null) transition = longevous / 5;
            return new TknKeeper<>(Tkn.of(ephemeral, longevous),
                    transition, kb.knowledge());
        }
    }

    /**
     * The knowledge builder that builds {@link Knowledge}
     *
     * @param <T> {@link T}
     * @param <A> {@link A}
     */
    public interface KnowledgeBuilder<T, K, A> {

        Knowledge<T, K, A> knowledge();
    }

    /**
     * The producer of thw {@link FinalBuilder}
     */
    public abstract class FinalBuilderProducer<K> {
        /**
         * Produces a {@link FinalBuilder}
         */
        public abstract FinalBuilder<K> ok();
    }

    /**
     * An implementation of the {@link KnowledgeBuilder} that builds {@link JwtKnowledge}
     */
    public class JwtBuilder extends FinalBuilderProducer<Jwt> implements KnowledgeBuilder<T, Jwt, A> {
        private final Lazy<String>.Default
                pri = Lazy.justDefault(Funny.just(Jwter.DEFAULT_PRI));
        private final Lazy<String>.Default
                pub = Lazy.justDefault(Funny.just(Jwter.DEFAULT_PUB));
        private final Lazy<String>.Default issuer =
                Lazy.justDefault(Funny.just(
                        String.format("https://%s", TkBuilder.class.getCanonicalName())));
        private final Lazy<Integer>.Default
                leeway = Lazy.justDefault(Funny.just(30000));
        private final Lazy<Function<A, String>>.Default
                ser = Lazy.justDefault(Funny.just(Object::toString));
        private final Lazy<Jwter.Scheme<?>>.Default
                scheme = Lazy.justDefault(() -> NimbusScheme.Simple);
        private final List<ImpatientFunction<Jwt, Boolean, JwtException>> validators = new LinkedList<>();

        private Function<String, A> des;

        /**
         * The {@link JwtKnowledge#jwter#pri}
         */
        public JwtBuilder pri(String pri) {
            this.pri.provide(pri);
            return this;
        }

        /**
         * The {@link JwtKnowledge#jwter#pub}
         */
        public JwtBuilder pub(String pub) {
            this.pub.provide(pub);
            return this;
        }

        /**
         * The {@link Jwter.Issuer#iss}
         */
        public JwtBuilder issuer(String issuer) {
            this.issuer.provide(issuer);
            return this;
        }

        /**
         * The serializer that serializes a given subject of type {@link A} to a string
         */
        public JwtBuilder ser(Function<A, String> ser) {
            this.ser.provide(ser);
            return this;
        }

        /**
         * The deserializer that deserializes a given string to a subject of type {@link A}
         */
        public JwtBuilder des(Function<String, A> des) {
            this.des = des;
            return this;
        }

        /**
         * The {@link Jwter.Issuer#leeway}
         */
        public JwtBuilder leeway(int leeway) {
            this.leeway.provide(leeway);
            return this;
        }

        /**
         * The {@link Jwter.Scheme} instance
         */
        public <J> JwtBuilder scheme(Jwter.Scheme<J> scheme) {
            this.scheme.provide(scheme);
            return this;
        }

        /**
         * The {@link Jwter#ensure(ImpatientFunction[])}
         */
        @SafeVarargs
        public final JwtBuilder ensure(ImpatientFunction<Jwt, Boolean, JwtException>... validators) {
            Collections.addAll(this.validators, validators);
            return this;
        }

        /**
         * The {@link Jwter#surefire(String...)}
         *
         * @see Jwt#surefire(String...)
         */
        public final JwtBuilder surefire(String... iss) {
            this.validators.addAll(Jwt.surefire(iss));
            return this;
        }

        /**
         * @see FinalBuilderProducer#ok()
         */
        public FinalBuilder<Jwt> ok() {
            return new FinalBuilder<>(this);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Knowledge<T, Jwt, A> knowledge() {
            if (des == null)
                throw new IllegalArgumentException("Must provide a deserializer");
            return new JwtKnowledge(
                    new Jwter(scheme.get(), pri.get(), pub.get()).ensure(validators),
                    issuer.get(), ser.get(), des, leeway.get());
        }
    }
}
