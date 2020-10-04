package com.sinlo.core.service;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.core.service.util.Xervice;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.SponteInitializer;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The pond of {@link Keeper} maintained services
 *
 * @author sinlo
 */
public class Pond {
    private static final Pool.Simple<Object> p = new Pool.Simple<>();

    private Pond() {
    }

    /**
     * Initialize and create without any initiator
     *
     * @see #create(Consumer)
     */
    public static Pond create() {
        return create(null);
    }

    /**
     * Initialize and create with an initiator which will act before any
     * initialization taking place. The initiator could be used to place
     * some service objects beforehand
     */
    public static Pond create(Consumer<Pool.Simple<Object>> initiator) {
        if (initiator != null) initiator.accept(p);
        new Initializer();
        return new Pond();
    }

    @SuppressWarnings("unchecked")
    public <T> T service(Class<T> c) {
        return Funny.maybe(c, (clz) -> (T) p.get(clz.getName()));
    }

    /**
     * The initializer
     */
    private static class Initializer extends SponteInitializer {

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Annotation>[] subjects() {
            return new Class[]{Proxistor.class};
        }
    }

    /**
     * The pond keeper who maintain the service objects in the pond by wrapping new
     * functionalities around them
     *
     * @param <T> the dedicated type of the annotation which represents the specific
     *            functionality which are wrapped around the service objects
     */
    public interface Keeper<T extends Annotation> extends SponteAware {

        /**
         * Do the maintenance
         */
        Object maintain(Object service, Function<AnnotatedElement, T> noter);

        @SuppressWarnings("unchecked")
        @Override
        default void onExplore(Profile profile) {
            String key = profile.type.getName();
            Pond.p.place(key, maintain(Pond.p.get(key, () -> Typer.create(profile.type)),
                    e -> e.getAnnotation((Class<T>) profile.subject.getClass())));
        }
    }

    public abstract static class Delegation<T extends Annotation, L> implements Keeper<T> {

        private Set<Object> delegates = new HashSet<>();
        private final Pool.Simple<L> specialization = new Pool.Simple<>();

        /**
         * Supply the payload
         */
        protected abstract L payload(T t, Object service);

        /**
         * Provide the annotation that flags exclusion
         */
        protected abstract Class<? extends Annotation> excluded();

        /**
         * Handle the invocation with the given payload
         */
        protected abstract Object handle(L payload, Callable<Object> invocation);

        @Override
        public void finale(int fin) {
            if (SponteInitializer.DECLINING == fin) {
                delegates.clear();
                delegates = null;
            }
        }

        @Override
        public boolean filter(Profile profile) {
            return !(profile.subject instanceof Sponte)
                    && Prototype.instantiable(profile.type);
        }

        @Override
        public Object maintain(Object service, Function<AnnotatedElement, T> noter) {
            Class<?> type = service.getClass();
            final Class<? extends Annotation> ex = excluded();

            // do not delegate delegates that are already delegated by this delegation
            if (delegates.contains(service)) return service;

            // find and keep all the method specific payloads
            Prototype.methods(type)
                    .forEach(m -> {
                        T pivot = noter.apply(m);
                        if (pivot != null) {
                            specialization.get(spk(type, m), () -> payload(pivot, service));
                        }
                    });

            // use the pivot annotation on the enclosing type to get a general payload
            final T enc = noter.apply(type);
            final boolean general = enc != null;
            final L payload = enc == null ? null : payload(enc, service);

            Object delegated = Xervice.xervice(service, (proxy, method, args) -> {
                if (!Xervice.should(method, ex) // not should
                        // or not having the pivot annotation on method in a non-general
                        // case
                        || (!general && noter.apply(method) == null)) {
                    return method.invoke(service, args);
                }
                // use the method specific payload or the general payload
                L specialized = specialization.get(spk(type, method));
                return handle(specialized != null ? specialized : payload,
                        () -> method.invoke(service, args));
            });

            delegates.add(delegated);
            return delegated;
        }

        public static String spk(Class<?> type, Method m) {
            return type.getName().concat("#").concat(m.getName());
        }
    }
}
