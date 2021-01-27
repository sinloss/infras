package com.sinlo.security.jwt.spec.exception;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Indicating that the validation has failed
 *
 * @author sinlo
 * @see com.sinlo.security.jwt.Jwter#decode(String)
 */
public class ValidationFailedException extends BadJwtException {

    private Set<Throwable> causes;

    public ValidationFailedException(Set<Throwable> causes) {
        super(String.format("The validation failed on following causes: %s",
                causes.stream().map(Throwable::toString).collect(Collectors.joining(", \n"))));
    }

    public Set<Throwable> causes() {
        return causes;
    }
}
