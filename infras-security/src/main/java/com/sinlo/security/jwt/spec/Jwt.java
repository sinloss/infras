package com.sinlo.security.jwt.spec;

import com.sinlo.core.common.functional.ImpatientFunction;
import com.sinlo.core.common.util.Arria;
import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Try;
import com.sinlo.security.jwt.spec.exception.JwtException;
import com.sinlo.security.jwt.spec.sure.ShouldIssuer;
import com.sinlo.security.jwt.spec.sure.ShouldTime;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The jwt core
 *
 * @author sinlo
 */
public class Jwt {

    public final String token;
    private final Map<String, Object> headers;
    private final Map<String, Object> claims;

    public Jwt(String token, Map<String, Object> headers, Map<String, Object> claims) {
        this.token = Objects.requireNonNull(token);
        this.headers = Objects.requireNonNull(headers, "headers cannot be empty");
        this.claims = Objects.requireNonNull(claims, "claims cannot be empty");
    }

    /**
     * @return a {@code Map} of the JOSE header(s)
     */
    public Map<String, Object> getHeaders() {
        return this.headers;
    }

    /**
     * @return a {@code Map} of the JWT Claims Set
     */
    public Map<String, Object> getClaims() {
        return this.claims;
    }

    /**
     * Validate using the given validators
     */
    @SafeVarargs
    public final Set<Throwable> sure(ImpatientFunction<Jwt, Boolean, JwtException>... validators) {
        return sure(Arrays.asList(validators));
    }

    /**
     * Validate using the given validators
     */
    public Set<Throwable> sure(List<ImpatientFunction<Jwt, Boolean, JwtException>> validators) {
        return validators.stream()
                .map(v -> Funny.bind(v, this))
                .map(Try::capture)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Surefire! Create a list of {@code validators} containing {@link ShouldTime#afterNbf()} and
     * {@link ShouldTime#beforeExp()}. Also, may add an extra {@link ShouldIssuer} based on the
     * given {@code iss} strings
     *
     * @param iss <ul>
     *            <li>no {@link ShouldIssuer} when it is null or empty</li>
     *            <li>an {@link ShouldIssuer#be(String)} when it only has one element</li>
     *            <li>an {@link ShouldIssuer#in(String...)} when it has more elements</li>
     *            </ul>
     */
    public static List<ImpatientFunction<Jwt, Boolean, JwtException>> surefire(String... iss) {
        List<ImpatientFunction<Jwt, Boolean, JwtException>> sures = new LinkedList<>();
        Collections.addAll(sures, ShouldTime.afterNbf(), ShouldTime.beforeExp());
        if (Arria.nonEmpty(iss))
            sures.add(iss.length == 1
                    ? ShouldIssuer.be(iss[0]) : ShouldIssuer.in(iss));
        return sures;
    }

    /**
     * Get a claim out of the {@code claims}
     */
    public <T> T get(String name, Function<Object, T> conv) {
        return Claim.get(claims, name, conv);
    }

    /**
     * Get the {@link Claim#ISS}
     */
    public URL issuer() {
        return Claim.ISS.get(claims, s -> {
            if (s instanceof URL) return (URL) s;
            return Try.tolerate(() -> new URL(String.valueOf(s)));
        });
    }

    /**
     * Get the {@link Claim#SUB} string
     */
    public String subject() {
        return Claim.SUB.get(claims);
    }

    /**
     * Get the {@link Claim#SUB} of a specific type which can be converted from {@link String}
     * using the given {@code conv}
     *
     * @param conv the converter
     * @param <T>  the expected type
     */
    public <T> T subject(Function<String, T> conv) {
        return conv.apply(subject());
    }

    /**
     * Get the {@link Claim#AUD} list of strings
     */
    @SuppressWarnings("unchecked")
    public List<String> audience() {
        return Claim.AUD.get(claims, s -> {
            if (s instanceof List) return (List<String>) s;
            return Arrays.asList(String.valueOf(s).split(","));
        });
    }

    /**
     * Get the {@link Claim#EXP}
     */
    public Instant expiresAt() {
        return Claim.EXP.get(claims, Jwt::toInstant);
    }

    /**
     * Get the {@link Claim#NBF}
     */
    public Instant notBefore() {
        return Claim.NBF.get(claims, Jwt::toInstant);
    }

    /**
     * Get the {@link Claim#IAT}
     */
    public Instant issuedAt() {
        return Claim.IAT.get(claims, Jwt::toInstant);
    }

    /**
     * Get the {@link Claim#JTI}
     */
    public String id() {
        return Claim.JTI.get(claims);
    }

    /**
     * Convert an object from the claims to an instant, the object must be of the same type
     * as {@link Claim#IAT}, {@link Claim#NBF} or {@link Claim#EXP}
     */
    public static Instant toInstant(Object s) {
        if (s instanceof Date) return ((Date) s).toInstant();
        if (s instanceof LocalDateTime) return ((LocalDateTime) s).toInstant(ZoneOffset.UTC);
        if (s instanceof Long) return Instant.ofEpochMilli((Long) s);
        return Instant.ofEpochMilli((long) s);
    }

    /**
     * Which provides jwt decoding methods
     */
    public interface Dec {
        Jwt decode(String token) throws JwtException;
    }

    /**
     * Which provides jwt signing methods
     */
    public interface Signer<T> {
        T sign(T signer) throws JwtException;
    }

    /**
     * Jwt claims
     */
    public enum Claim {

        /**
         * {@code iss} - Issuer
         */
        ISS("iss"),

        /**
         * {@code sub} - Subject
         */
        SUB("sub"),

        /**
         * {@code aud} - Audience
         */
        AUD("aud"),

        /**
         * {@code exp} - Expiration
         */
        EXP("exp"),

        /**
         * {@code nbf} - Not Before
         */
        NBF("nbf"),

        /**
         * {@code iat} - Issued at
         */
        IAT("iat"),

        /**
         * {@code jti} - JWT ID
         */
        JTI("jti"),
        ;

        /**
         * The name of the claim
         */
        public final String name;

        /**
         * Get the string representation of the claim out of the given {@code claims}
         */
        public String get(Map<String, Object> claims) {
            return get(claims, String::valueOf);
        }

        /**
         * Get this claim out of claims
         *
         * @param claims the source claims
         * @param as     the type convertor converts a raw claim string to a specific type
         * @param <T>    the expected type of this claim
         * @see #get(Map, String, Function)
         */
        public <T> T get(Map<String, Object> claims, Function<Object, T> as) {
            return get(claims, name, as);
        }

        /**
         * Get a claim out of claims
         *
         * @param claims the source claims
         * @param name   the name of the expected claim
         * @param as     the type convertor converts a raw claim string to a specific type
         * @param <T>    the expected type of the claim
         */
        public static <T> T get(Map<String, Object> claims, String name, Function<Object, T> as) {
            if (!claims.containsKey(name)) {
                return null;
            }
            return as.apply(claims.get(name));
        }

        Claim(String name) {
            this.name = name;
        }
    }
}
