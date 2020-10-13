package com.sinlo.core.service;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.core.service.spec.Pump;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.SponteInitializer;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Signature;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The pond of {@link Keeper} maintained services
 *
 * @author sinlo
 */
public class Pond {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] subjects = Sponte.Fo.lines(Ponded.class, line -> {
        try {
            return Typer.forName(line);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }).toArray(new Class[0]);

    private static final AtomicReference<Pond> instance = new AtomicReference<>();
    private static final Pool.Simple<Object> p = new Pool.Simple<>();

    private Pond() {
    }

    /**
     * Initialize and create without any {@link Pump}
     *
     * @see #of(Pump)
     */
    public static Pond of() {
        return of(null);
    }

    /**
     * Initialize and create with an {@link Pump} which will provide instantiation as well
     * as post processing of the service object before it is placed in the {@link #p pond}.
     * The pond instance is a singleton instance, and the initialization will be called
     * only once
     */
    public static Pond of(Pump pump) {
        Pond i = instance.get();
        if (i == null) {
            if (instance.compareAndSet(null, i = new Pond())) {
                // initialize
                new Initializer(pump);
                return i;
            }
            return instance.get();
        }
        return i;
    }

    /**
     * Get the corresponding object in the pond
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> c) {
        return Funny.maybe(c, (clz) -> (T) p.get(clz.getName()));
    }

    /**
     * Modify the object of the given class in the pond
     *
     * @return the new object in the pond
     */
    @SuppressWarnings("unchecked")
    public <T> T modify(Class<T> c, Pump pump) {
        return (T) Funny.maybe(c, (clz) -> p.on(Pool.Key.catstate(clz.getName()), (k, t) -> {
            if (t == null) {
                // new agent
                t = Agent.M.create(clz, pump.sink(clz));
            }
            return pump.sink(t);
        }));
    }

    /**
     * The initializer
     */
    private static class Initializer extends SponteInitializer {

        private final Pump pump;

        private Initializer(Pump pump) {
            this.pump = pump;
        }

        @Override
        public Class<? extends Annotation>[] subjects() {
            return subjects;
        }

        @Override
        public Object payload() {
            return pump;
        }
    }

    /**
     * The delegate who can do something more than {@link Agent.Bond}
     *
     * @param <L> type of the payload
     */
    public interface Delegate<L> extends Agent.Bond {

        /**
         * Handle the invocation
         */
        <R> R handle(Agent.Context context, Callable<R> mission, L payload);

        @SuppressWarnings("unchecked")
        @Override
        default <R> R act(Agent.Context context, Callable<R> mission) {
            L payload = (L) Pond.p.get(Keeper.key(context.pivot, context.signature));
            if (payload != null) {
                return handle(context, mission, payload);
            }
            // no payload, call the mission directly
            try {
                return mission.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The pond keeper who maintain the service objects in the pond by wrapping new
     * functionalities around them, and maintain the payloads for them
     *
     * @param <T> the dedicated type of the annotation which represents the specific
     *            functionality which are wrapped around the service objects
     * @param <L> the type of the payload
     */
    public interface Keeper<T extends Annotation, L> extends SponteAware {

        String IDENTIFIER = "@payloads~";

        /**
         * Get a payload regarding the given annotation, object and method. The method
         * would be null in case of annotation annotated on type
         */
        L payload(T t, Object service, Method method);

        /**
         * Should the given method be delegated
         */
        boolean should(T t, Method method, boolean lazy);

        static String key(Class<? extends Annotation> pivot, Method method) {
            return key(pivot, Signature.of(method).toString());
        }

        static String key(Class<? extends Annotation> pivot, String signature) {
            return IDENTIFIER.concat(pivot.getName()).concat("~").concat(signature);
        }

        @SuppressWarnings("unchecked")
        @Override
        default void onExplore(Profile profile, Object payload) {
            // the pond key
            final String pk = profile.type.getName();
            // the noter
            final Profile.Subjectifier<T> noter =
                    (Profile.Subjectifier<T>) profile.subjectifier;
            // the pump
            final Pump pump = payload instanceof Pump ? (Pump) payload : null;
            // the target object
            final Object service = Pond.p.get(pk, pump != null
                    ? () -> pump.sink(profile.type) : () -> Pump.create(profile.type));
            final Class<? extends Annotation> subject = profile.subject.annotationType();

            // the pivot annotation on the enclosing type
            T noted = noter.apply(profile.type);
            final T enc = pump == null ? noted : pump.note(noted, profile.type);
            // lazy delegation when the pivot annotation is not annotated on the enclosing
            // type, which means only the methods annotated with the pivot annotation will
            // be delegated
            final boolean lazy = enc == null;
            // a general annotation that works only when it is not lazy, that is, when a
            // method is not annotated with the pivot annotation and the enclosing type on
            // the other hand is annotated, then use the general payload created from the
            // enclosing type as the said method's payload
            final L generalPayload = lazy ? null
                    : payload(enc, service, null);

            // find and keep all the method specific payloads
            Prototype.methods(profile.type)
                    .forEach(m -> {
                        T pivot = noter.apply(m);
                        if (pump != null) pivot = pump.note(pivot, m);
                        // not should then return
                        if (!should(pivot == null ? enc : pivot, m, lazy)) return;
                        // maintain payloads
                        if (pivot != null) {
                            Pond.p.place(key(subject, m), payload(pivot, service, m));
                        } else if (!lazy) {
                            Pond.p.place(key(subject, m), generalPayload);
                        }
                        // lazy and not annotated with the pivot annotation, keep it null
                    });

            // maintain the readied service object
            Object readied = Agent.M.create(profile.type, service);
            Pond.p.place(pk, pump == null ? readied : pump.sink(readied));
        }

        @Override
        default boolean filter(Profile profile) {
            return !(profile.subject instanceof Sponte)
                    && Prototype.instantiable(profile.type);
        }
    }
}
