package com.sinlo.security.tkn;

import com.sinlo.security.jwt.Jwter;
import com.sinlo.security.tkn.knowledges.JwtKnowledge;
import com.sinlo.security.tkn.spec.Knowledge;
import com.sinlo.security.tkn.spec.Tkn;

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
    public static <T, A> TkBuilder<T, A> of(Class<T> tc, Class<A> ac) {
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
    public class FinalBuilder {

        private final KnowledgeBuilder<T, A> kb;

        private FinalBuilder(KnowledgeBuilder<T, A> kb) {
            this.kb = kb;
        }

        /**
         * Finally build
         */
        public TknKeeper<T, A> build() {
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
    public interface KnowledgeBuilder<T, A> {

        Knowledge<T, A> knowledge();
    }

    /**
     * The producer of thw {@link FinalBuilder}
     */
    public abstract class FinalBuilderProducer {
        /**
         * Produces a {@link FinalBuilder}
         */
        public abstract FinalBuilder ok();
    }

    /**
     * An implementation of the {@link KnowledgeBuilder} that builds {@link JwtKnowledge}
     */
    public class JwtBuilder extends FinalBuilderProducer implements KnowledgeBuilder<T, A> {
        private String pri;
        private String pub;
        private String issuer;
        private Function<A, String> ser;
        private Function<String, A> des;
        private Integer leeway;

        /**
         * The {@link JwtKnowledge#jwter#pri}
         */
        public JwtBuilder pri(String pri) {
            this.pri = pri;
            return this;
        }

        /**
         * The {@link JwtKnowledge#jwter#pub}
         */
        public JwtBuilder pub(String pub) {
            this.pub = pub;
            return this;
        }

        /**
         * The {@link com.sinlo.security.jwt.Jwter.Issuer#iss}
         */
        public JwtBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * The serializer that serializes a given subject of type {@link A} to a string
         */
        public JwtBuilder ser(Function<A, String> ser) {
            this.ser = ser;
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
         * The {@link com.sinlo.security.jwt.Jwter.Issuer#leeway}
         */
        public JwtBuilder leeway(int leeway) {
            this.leeway = leeway;
            return this;
        }

        /**
         * @see FinalBuilderProducer#ok()
         */
        public FinalBuilder ok() {
            return new FinalBuilder(this);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Knowledge<T, A> knowledge() {
            if (des == null)
                throw new IllegalArgumentException("Must provide a deserializer");
            if (pri == null) pri = Jwter.DEFAULT_PRI;
            if (pub == null) pub = Jwter.DEFAULT_PUB;
            if (issuer == null) issuer = TkBuilder.class.getCanonicalName();
            if (leeway == null) leeway = 30000;
            if (ser == null) ser = Object::toString;
            return new JwtKnowledge(pri, pub, issuer, ser, des, leeway);
        }
    }
}
