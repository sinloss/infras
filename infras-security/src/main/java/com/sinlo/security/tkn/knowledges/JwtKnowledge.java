package com.sinlo.security.tkn.knowledges;

import com.sinlo.security.jwt.Jwter;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.tkn.spec.Knowledge;
import com.sinlo.security.tkn.spec.State;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/**
 * A jwt implementation of {@link Knowledge}
 *
 * @author sinlo
 */
public class JwtKnowledge<A, J> implements Knowledge<String, A> {

    public final Jwter<J> jwter;
    private final Jwter<J>.Issuer<A> issuer;
    private final Function<String, A> des;

    public JwtKnowledge(Jwter<J> jwter, String issuer,
                        Function<A, String> ser,
                        Function<String, A> des,
                        int leeway) {
        this.jwter = jwter;
        this.des = des;
        this.issuer = this.jwter.issuer(issuer, ser, leeway);
    }

    @Override
    public State<String, A> stat(String token) {
        Jwt jwt = jwter.decode(token);
        Instant exp = jwt.expiresAt();
        return State.of(des.apply(jwt.subject()),
                exp == null ? Long.MAX_VALUE : exp.toEpochMilli());
    }

    @Override
    public String create(Long lifespan, A subject) {
        return issuer.issue(
                UUID.randomUUID().toString(), subject, lifespan).serialize();
    }
}
