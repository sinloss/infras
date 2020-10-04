package com.sinlo.core.jdbc.util;

import com.sinlo.core.jdbc.Mould;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;

/**
 * Sculptor caches the moulds
 *
 * @author sinlo
 * @see Mould
 * @see Prototype#of(Class)
 */
public class Sculptor implements SponteAware {

    @Override
    public void onExplore(Profile profile) {
        // Prototype itself has an object pool
        Prototype.of(profile.type);
    }
}
