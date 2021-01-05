package com.sinlo.sponte;

import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.core.SponteExplorer;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The initializer for sponte, the initialization happens when every first instance of any
 * subclass of this is instantiated unless it is {@link #willing()} for more
 *
 * @author sinlo
 */
public abstract class SponteInitializer implements Predicate<Profile> {

    public static final int WILLING = 0;
    public static final int INITIALIZING = 1;
    public static final int DECLINING = -1;

    private static final Pool.Simple<AtomicInteger> states = new Pool.Simple<>();

    private AtomicInteger state() {
        return states.get(this.getClass().getName(),
                () -> new AtomicInteger(WILLING));
    }

    public SponteInitializer() {
        final AtomicInteger state = state();
        if (state.get() == DECLINING) return;
        // spin when initializing
        while (!state.compareAndSet(WILLING, INITIALIZING)) {
            if (state.get() == DECLINING) {
                // when the state turns out to decline any more initialization, then
                // we better just back off
                return;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException ignored) {
                return;
            }
        }
        Class<? extends Annotation>[] subjects = subjects();
        // INITIALIZING is the only state that can reach here and needs to be alter
        state.compareAndSet(INITIALIZING, DECLINING);

        // the real initializing process does not really care about threads
        List<? extends SponteAware> explored = SponteExplorer
                .of(this, payload(), subjects).explore().collect(Collectors.toList());

        int fin = state.get();

        // purge the profile pool if declining
        if (fin == DECLINING) Profile.purge();

        // call finale methods
        finale(fin);
        explored.forEach(aware -> aware.finale(fin));
    }

    /**
     * Willing for more
     */
    @SuppressWarnings("StatementWithEmptyBody")
    protected final void willing() {
        // spin until successfully set
        final AtomicInteger state = state();
        while (!state.compareAndSet(state.get(), WILLING)) ;
    }

    /**
     * The calling of this method in the {@link #SponteInitializer() constructor} is
     * guaranteed to be atomic
     */
    public abstract Class<? extends Annotation>[] subjects();

    /**
     * A finalizer for subclasses to override
     */
    @SuppressWarnings("EmptyMethod")
    public void finale(int state) {
    }

    /**
     * The filter to be provided to the {@link SponteExplorer#of(Predicate, Object, Class[])}
     * called in then {@link #SponteInitializer() constructor}
     */
    @Override
    public boolean test(Profile profile) {
        return true;
    }

    /**
     * @return the payload
     */
    public Object payload() {
        return null;
    }
}
