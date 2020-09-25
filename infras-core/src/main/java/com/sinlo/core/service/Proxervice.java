package com.sinlo.core.service;

import com.sinlo.core.domain.Persistor;
import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.domain.RepositoriesSelector;
import com.sinlo.core.domain.spec.Selector;
import com.sinlo.core.service.spec.ProxerviceIgnore;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The proxyed service creator
 * <p/>
 * All the methods of the original service would be wrapped in a {@link Persistor#enclose(Consumer,
 * Selector)}
 * process except for the methods annotated by the {@link ProxerviceIgnore}
 *
 * @author sinlo
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Proxervice<T extends Entity> {

    /**
     * The dummy selector just represent the logic concept of derive declared repos of impl
     */
    public static final Selector DERIVE_IMPL_DECLARED = (entity) -> null;

    public final Persistor<T> persistor;
    public final Selector<T> selector;

    /**
     * @see Proxervice#Proxervice(Persistor, Selector)
     */
    public Proxervice(Persistor<T> persistor) {
        this(persistor, null);
    }

    /**
     * @param persistor {@link Persistor}
     * @param selector  the default repos' selector
     */
    public Proxervice(Persistor<T> persistor, Selector<T> selector) {
        if (persistor == null) {
            throw new IllegalArgumentException(
                    "The given persistor should not be null");
        }
        this.persistor = persistor;
        this.selector = selector;
    }

    /**
     * @see #xervice(Object, Selector)
     */
    public final <O> O xervice(O impl) {
        return xervice(impl, (Selector<T>) DERIVE_IMPL_DECLARED);
    }

    /**
     * @see #xervice(Object, Selector, boolean)
     */
    public final <O> O xervice(O impl, Selector<T> selector) {
        return xervice(impl, selector, false);
    }

    /**
     * Create a proxyed service using {@link Enhancer#create()} via cglib
     *
     * @param impl     the instance of a concrete class
     * @param selector the designated repos' selector used by the {@link
     *                 Proxervice#persistor}
     * @param <O>      the type of the [ impl ] instance
     * @param fallback use the {@link Proxervice#selector} as a fallback if true
     */
    public final <O> O xervice(O impl, Selector<T> selector, boolean fallback) {
        if (impl == null) {
            throw new IllegalArgumentException("The given impl should not be null");
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(impl.getClass());

        final Selector r = pick(impl, selector, fallback);

        enhancer.setCallbacks(new MethodInterceptor[]{
                (o, method, args, methodProxy) -> invoke(impl, method, args, r)
        });

        return (O) enhancer.create();
    }

    /**
     * @see #xervice(Class, Object, Selector)
     */
    public final <I, O extends I> I xervice(Class<I> spec, O impl) {
        return xervice(spec, impl, (Selector<T>) DERIVE_IMPL_DECLARED);
    }

    /**
     * @see #xervice(Class, Object, Selector, boolean)
     */
    public final <I, O extends I> I xervice(Class<I> spec, O impl, Selector<T> selector) {
        return xervice(spec, impl, selector, false);
    }

    /**
     * Create a proxyed service using {@link Proxy#newProxyInstance(ClassLoader, Class[],
     * InvocationHandler)}
     *
     * @param spec     the specific interface which the [ impl ] should implement
     * @param impl     the instance of a concrete class implemented the given [ spec ]
     * @param selector the designated repos' selector used by the {@link Proxervice#persistor}
     * @param <I>      the type of the [ spec ] interface
     * @param <O>      the type of the [ impl ] instance
     */
    public final <I, O extends I> I xervice(Class<I> spec, O impl, Selector<T> selector,
                                            boolean fallback) {
        if (spec == null || !spec.isInterface()) {
            throw new IllegalArgumentException("The given spec [ "
                    .concat(String.valueOf(spec))
                    .concat(" ] is not a valid interface"));
        }

        if (impl == null) {
            throw new IllegalArgumentException("The given impl should not be null");
        }

        if (!spec.isAssignableFrom(impl.getClass())) {
            throw new IllegalArgumentException(
                    "The given impl does not implement the given interface spec [ "
                            .concat(String.valueOf(spec)).concat(" ]"));
        }

        final Selector<T> r = pick(impl, selector, fallback);

        return (I) Proxy.newProxyInstance(impl.getClass().getClassLoader(),
                new Class[]{spec},
                (proxy, method, args) -> invoke(impl, method, args, r));
    }

    private Object invoke(Object impl, Method method, Object[] args, Selector<T> selector)
            throws Exception {
        if (should(impl.getClass(), method)) {
            try (Persistor<T>.Stub stub =
                         persistor.enclose(null, selector)) {
                try {
                    return method.invoke(impl, args);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    stub.cancel();
                    return null;
                }
            }
        }
        return method.invoke(impl, args);
    }

    private boolean should(Class<?> clz, Method method) {
        try {
            return !Object.class.equals(method.getDeclaringClass()) &&
                    clz.getDeclaredMethod(
                            method.getName(), method.getParameterTypes())
                            .getAnnotation(ProxerviceIgnore.class) == null;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Selector pick(Object impl, Selector selector, boolean fallback) {
        if (selector == null) {
            if (this.selector == null) {
                return tryWrap(derive(impl), fallback);
            }
            return this.selector;
        } else if (DERIVE_IMPL_DECLARED == selector) {
            return tryWrap(derive(impl), fallback);
        }
        return tryWrap(selector, fallback);
    }

    private Selector tryWrap(Selector selector, boolean fallback) {
        return fallback
                ? new FallbackableSelector(selector, this.selector)
                : selector;
    }

    /**
     * Derive a selector consisted of all the declared repos in the given
     * impl object
     */
    public static Selector derive(Object impl) {
        try {
            Class<?> xclz = impl.getClass();
            List<Repo> li = new LinkedList<>();
            for (Field field : xclz.getDeclaredFields()) {
                if (Repo.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    li.add((Repo) field.get(impl));
                }
            }
            return new RepositoriesSelector(li);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return DERIVE_IMPL_DECLARED;
    }
}
