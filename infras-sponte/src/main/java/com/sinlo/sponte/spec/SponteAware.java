package com.sinlo.sponte.spec;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.core.Pri;

/**
 * Handle the sponted annotations at runtime
 *
 * @author sinlo
 */
@FunctionalInterface
public interface SponteAware {

    Pri<SponteAware> pri = new Pri<>();

    /**
     * A finalizer
     */
    default void finale(int fin) {
    }

    /**
     * By default this filters out {@link Profile profiles} of the subject
     * {@link Sponte}, which means normally the filtered profiles is what
     * we just need
     */
    default boolean filter(Profile profile) {
        return !(profile.subject instanceof Sponte);
    }

    <A> void onExplore(Profile profile, A payload);
}
