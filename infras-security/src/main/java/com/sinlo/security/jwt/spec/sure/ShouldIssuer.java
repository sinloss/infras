package com.sinlo.security.jwt.spec.sure;

import com.sinlo.core.common.functional.ImpatientFunction;
import com.sinlo.core.common.util.Try;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.JwtException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Validate that the issuer should ...
 *
 * @author sinlo
 */
public class ShouldIssuer implements ImpatientFunction<Jwt, Boolean, JwtException> {

    /**
     * Should be the given {@code iss}
     */
    public static ShouldIssuer be(String iss) {
        try {
            return new ShouldIssuer(new URL(iss)::equals);
        } catch (MalformedURLException e) {
            return Try.toss(e);
        }
    }

    /**
     * Should be in the given {@code iss}s
     */
    public static ShouldIssuer in(String... iss) {
        return new ShouldIssuer(url ->
                Arrays.stream(iss)
                        .map(Try.tolerated(URL::new))
                        .anyMatch(url::equals));
    }

    public ShouldIssuer(Predicate<URL> criterion) {
        this.criterion = criterion;
    }

    private final Predicate<URL> criterion;

    @Override
    public Boolean employ(Jwt jwt) throws JwtException {
        return criterion.test(jwt.issuer());
    }
}
