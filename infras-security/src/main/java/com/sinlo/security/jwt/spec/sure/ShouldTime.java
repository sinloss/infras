package com.sinlo.security.jwt.spec.sure;

import com.sinlo.core.common.functional.ImpatientFunction;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

/**
 * The abstraction of timestamp validators
 *
 * @author sinlo
 */
public abstract class ShouldTime implements ImpatientFunction<Jwt, Boolean, JwtException> {

    /**
     * Should the current time be after the specific {@link Jwt#notBefore()}
     */
    public static ShouldTime afterNbf() {
        return new ShouldTime(true) {
            @Override
            public Instant when(Jwt jwt) {
                return jwt.notBefore();
            }

            @Override
            public String ifNot() {
                return "The jwt should not be used before %s";
            }
        };
    }

    /**
     * Should the current time be before the specific {@link Jwt#expiresAt()}
     */
    public static ShouldTime beforeExp() {
        return new ShouldTime(false) {
            @Override
            public Instant when(Jwt jwt) {
                return jwt.expiresAt();
            }

            @Override
            public String ifNot() {
                return "The jwt has already expired at %s";
            }
        };
    }

    /**
     * To tolerate a specific {@link Duration} of time skew, default 60 seconds
     */
    private Duration leeway = Duration.of(60, ChronoUnit.SECONDS);

    /**
     * The basic {@link Clock}
     */
    private Clock clock = Clock.systemUTC();

    /**
     * Indicating the direction of the criterion
     */
    private final boolean afterwards;

    public ShouldTime(boolean afterwards) {
        this.afterwards = afterwards;
    }

    /**
     * The criterion which applies on the specific timestamp limit
     */
    private Predicate<Instant> criterion() {
        Instant now = Instant.now(clock);
        return afterwards
                ? now.plus(leeway)::isAfter : now.minus(leeway)::isBefore;
    }

    /**
     * The specific timestamp limit on which the criterion should apply
     */
    public abstract Instant when(Jwt jwt);

    /**
     * The message template when the criterion does not apply
     */
    public abstract String ifNot();

    @Override
    public Boolean employ(Jwt jwt) throws JwtException {
        Instant w = when(jwt);
        if (!criterion().test(w)) {
            throw new JwtException(
                    String.format(ifNot(), w));
        }
        return true;
    }

    public ShouldTime tolerate(Duration leeway) {
        this.leeway = leeway;
        return this;
    }

    public ShouldTime tolerate(long millis) {
        return tolerate(Duration.of(millis, ChronoUnit.MILLIS));
    }

    public ShouldTime use(Clock clock) {
        this.clock = clock;
        return this;
    }
}
