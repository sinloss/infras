package com.sinlo.security.tkn;

import com.sinlo.security.tkn.knowledges.JwtKnowledge;
import com.sinlo.security.tkn.spec.Knowledge;
import com.sinlo.security.tkn.spec.Tkn;

import java.util.function.Function;

/**
 * The builder of the {@link TknKeeper}
 *
 * @param <T> the type of {@code T} of {@link TknKeeper}
 * @param <A> the type of {@code A} of {@link TknKeeper}
 */
public class TkBuilder<T, A> {

    private Long ephemeral;
    private Long longevous;
    private Long transition;

    public TkBuilder<T, A> ephemeral(long ephemeral) {
        this.ephemeral = ephemeral;
        return this;
    }

    public TkBuilder<T, A> longevous(long longevous) {
        this.longevous = longevous;
        return this;
    }

    public TkBuilder<T, A> transition(long transition) {
        this.transition = transition;
        return this;
    }

    public JwtBuilder jwt() {
        return new JwtBuilder();
    }

    public class FinalBuilder {

        private final KnowledgeBuilder<T, A> kb;

        public FinalBuilder(KnowledgeBuilder<T, A> kb) {
            this.kb = kb;
        }

        public TknKeeper<T, A> build() {
            if (ephemeral == null) ephemeral = 7_200_000L;
            if (longevous == null) longevous = ephemeral * 84;
            if (transition == null) transition = longevous / 5;
            return new TknKeeper<>(Tkn.of(ephemeral, longevous),
                    transition, kb.knowledge());
        }
    }

    public interface KnowledgeBuilder<T, A> {
        Knowledge<T, A> knowledge();
    }

    public class JwtBuilder implements KnowledgeBuilder<T, A> {
        private String issuer;
        private Function<A, String> ser;
        private Function<String, A> des;
        private Integer leeway;

        public JwtBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public JwtBuilder ser(Function<A, String> ser) {
            this.ser = ser;
            return this;
        }

        public JwtBuilder des(Function<String, A> des) {
            this.des = des;
            return this;
        }

        public JwtBuilder leeway(int leeway) {
            this.leeway = leeway;
            return this;
        }

        public FinalBuilder ok() {
            return new FinalBuilder(this);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Knowledge<T, A> knowledge() {
            if (des == null)
                throw new IllegalArgumentException("Must provide a deserializer");
            if (issuer == null) issuer = TkBuilder.class.getCanonicalName();
            if (leeway == null) leeway = 30000;
            if (ser == null) ser = Object::toString;
            return new JwtKnowledge(issuer, ser, des, leeway);
        }
    }
}
