package com.sinlo.jdbc.util;

import com.sinlo.jdbc.Shape;
import com.sinlo.jdbc.Jadebee;
import com.sinlo.jdbc.spec.Shaper;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

/**
 * The {@link Shaper} registry
 *
 * @author sinlo
 * @see Shape
 * @see SponteAware
 * @see Sponte#value()
 */
public class Shapeherder implements SponteAware {

    @SuppressWarnings("rawtypes")
    private final Pool.Simple<Shaper> shapers = new Pool.Simple<>();

    /**
     * Get the instance created in the initialization process of {@link com.sinlo.sponte.SponteInitializer}
     * which is introduced by its subclass {@link Jadebee}
     */
    public static Shapeherder get() {
        return (Shapeherder) pri.get(Shapeherder.class);
    }

    /**
     * Get the shaper targeting at the given type
     */
    @SuppressWarnings({"rawtypes"})
    public Shaper shaper(Class type) {
        // nobody wants to shape something into an object
        if (Object.class.equals(type)) return null;

        Shaper shaper = shapers.get(type.getName());
        if (shaper == null) {
            return shaper(type.getSuperclass());
        }
        return shaper;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onExplore(Profile profile, Object payload) {
        Shaper instance = (Shaper) Typer.create(profile.type);
        if (instance == null) return;

        Shape shape = (Shape) profile.subject;
        String targeting = instance.aw();

        shapers.on(Pool.Key.catstate(targeting), (k, v) -> {
            if (v == null || (v instanceof Shaper.Ranked
                    && ((Shaper.Ranked<?, ?>) v).priority < shape.priority())) {
                return new Shaper.Ranked(shape.priority(), instance);
            }
            return v;
        });
    }

}
