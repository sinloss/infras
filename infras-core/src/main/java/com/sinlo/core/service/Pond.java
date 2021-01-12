package com.sinlo.core.service;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.core.service.spec.Pump;
import com.sinlo.core.service.spec.TooMany;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.SponteInitializer;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Signature;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The pond of {@link Keeper} maintained services
 *
 * @author sinlo
 */
public class Pond {

    private static final Class<? extends Annotation>[] subjects = Ponded.Manifest.get();

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
    public <T> T get(Class<T> c) throws TooMany {
        if (c == null) return null;
        return TooMany.shouldNot(p.get(c.getName()), c);
    }

    /**
     * Same as {@link #get(Class)}, but handles the {@link TooMany} by wrapping it
     * inside a {@link RuntimeException}
     */
    public <T> T single(Class<T> c) {
        try {
            return get(c);
        } catch (TooMany tooMany) {
            throw new RuntimeException(tooMany);
        }
    }

    /**
     * Same as {@link #get(Class)}, but returns null when {@link TooMany}
     */
    public <T> T just(Class<T> c) {
        try {
            return get(c);
        } catch (TooMany tooMany) {
            return null;
        }
    }

    /**
     * Modify the object of the given class in the pond
     *
     * @return the new object in the pond
     */
    public <T> T modify(Class<T> c, Pump pump) {
        return edit(c, pump);
    }

    @SuppressWarnings("unchecked")
    private static <T> T edit(Class<T> c, Pump pump) {
        return (T) Funny.maybe(c, (clz) -> p.on(Pool.Key.catstate(clz.getName()), (k, t) -> {
            final Object v = pump.sink(t == null ? pump.sink(clz) : t);
            // maintain keys of interfaces
            Arrays.stream(c.getInterfaces())
                    .map(Class::getName)
                    // make the many the MANY, and filter them out
                    .filter(n -> Pond.p.on(Pool.Key.present(n),
                            TooMany::really) != TooMany.MANY)
                    .forEach(n -> Pond.p.place(n, v));
            return v;
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
        <R> R handle(Agent.Context context, Agent.Mission<R> mission, L payload);

        @SuppressWarnings("unchecked")
        @Override
        default <R> R act(Agent.Context context, Agent.Mission<R> mission) {
            L payload = (L) Pond.p.get(Keeper.key(context.self.pivot(), context.signature));
            if (payload != null) {
                return handle(context, mission, payload);
            }
            // no payload, call the mission directly
            try {
                return mission.call(context.args);
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

        /**
         * The keeper maintains the delegation of a given type here
         *
         * @param type   the type of the delegated object
         * @param target optional, an instance of the delegated object
         * @param noter  the noter that fetches the pivot annotation from an {@link AnnotatedElement}
         * @param pp     a specific {@link Pump}
         */
        @SuppressWarnings("unchecked")
        default Object maintain(final Class<?> type, final Object target,
                                final Function<AnnotatedElement, T> noter, Pump pp) {
            // the pond key
            final String pk = type.getName();

            final Pump pump = pp == null ? Pump.DEFAULT : pp;

            // the target object
            final Object service = target == null ? (
                    Pond.p.get(pk, () -> pump.sink(type)))
                    : target;

            // check if should do the maintenance
            if (!pump.should(type, service)) return service;

            // the pivot annotation on the enclosing type
            T noted = noter.apply(type);
            final T enc = pump.note(noted, type);
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
            Prototype.methods(type)
                    .forEach(m -> {
                        T pivot = pump.note(noter.apply(m), m);
                        // not should then return
                        if (!should(pivot == null ? enc : pivot, m, lazy)) return;
                        // maintain payloads
                        if (pivot != null) {
                            Pond.p.place(key(pivot.annotationType(), m), payload(pivot, service, m));
                        } else if (!lazy) {
                            Pond.p.place(key(enc.annotationType(), m), generalPayload);
                        }
                        // lazy and not annotated with the pivot annotation, keep it null
                    });

            // maintain the readied service object
            return edit(type, new Pump() {
                @Override
                public <A> A sink(Class<A> type) {
                    return (A) service;
                }

                @Override
                public <A> A sink(A a) {
                    A readied = (A) Agent.M.create(type, a);
                    return pump.sink(readied);
                }
            });
        }

        /**
         * Process the given {@link Profile}
         */
        @SuppressWarnings("unchecked")
        @Override
        default void onExplore(Profile profile, Object payload) {
            maintain(profile.type, null,
                    (Profile.Subjectifier<T>) profile.subjectifier,
                    payload instanceof Pump ? (Pump) payload : null);
        }

        @Override
        default boolean filter(Profile profile) {
            return !(profile.subject instanceof Sponte)
                    && Prototype.instantiable(profile.type);
        }
    }
}
