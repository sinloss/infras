package com.sinlo.security.tkn.knowledges;

import com.sinlo.security.jwt.Jwter;
import com.sinlo.security.tkn.spec.Knowledge;
import com.sinlo.security.tkn.spec.State;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/**
 * A jwt implementation of {@link Knowledge}
 *
 * @author sinlo
 */
public class JwtKnowledge<A> implements Knowledge<String, A> {

    public static final Jwter jwter = new Jwter();
    private final Jwter.Issuer<A> issuer;
    private final Function<String, A> des;

    public JwtKnowledge(String issuer,
                        Function<A, String> ser,
                        Function<String, A> des,
                        int leeway) {
        this.des = des;
        this.issuer = jwter.issuer(issuer, ser, leeway);
    }

    @Override
    public State<String, A> stat(String token) {
        Jwt jwt = jwter.decode(token);
        Instant exp = jwt.getExpiresAt();
        return State.of(des.apply(jwt.getSubject()),
                exp == null ? Long.MAX_VALUE : exp.toEpochMilli());
    }

    @Override
    public String create(Long lifespan, A subject) {
        return issuer.issue(
                UUID.randomUUID().toString(), subject, lifespan).serialize();
    }
}
