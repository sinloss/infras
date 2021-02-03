package com.sinlo.security.verify;

import com.sinlo.security.tkn.spec.TknException;

import java.util.Objects;

/**
 * The verification failure carrying the cause of the failure
 *
 * @author sinlo
 */
public class VerificationFailure extends Exception {

    public VerificationFailure(TknException cause) {
        super(Objects.requireNonNull(cause,
                "The VerificationFailure won't happen if there's no cause")
                .getMessage(), cause);
    }

    /**
     * Check if the current verification failure is caused by the given type of {@link TknException}
     * or not
     */
    public <T extends TknException> boolean because(Class<T> clz) {
        return clz.isAssignableFrom(getCause().getClass());
    }
}
