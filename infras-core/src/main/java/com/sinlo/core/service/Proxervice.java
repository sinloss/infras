package com.sinlo.core.service;

import com.sinlo.core.domain.Persistor;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.service.spec.ProxerviceIgnore;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The proxyed service creator
 * <p/>
 * All the methods of the original service would be wrapped in a {@link Persistor#enclose(Consumer, Repo[])}
 * process except for the methods annotated by the {@link ProxerviceIgnore}
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Proxervice {

    public static final Repo[] ZERO_REPOS = new Repo[0];
    /**
     * The dummy supplier just represent the logic concept of derive declared repos of impl
     */
    public static final Supplier<Repo[]> DERIVE_IMPL_DECLARED = () -> ZERO_REPOS;

    public final Persistor<?> persistor;
    public final Supplier<Repo[]> supplier;

    /**
     * @see Proxervice#Proxervice(Persistor, Supplier)
     */
    public Proxervice(Persistor<?> persistor) {
        this(persistor, null);
    }

    /**
     * @param persistor {@link Persistor}
     * @param supplier  the default repos' supplier
     */
    public Proxervice(Persistor<?> persistor, Supplier<Repo[]> supplier) {
        if (persistor == null) {
            throw new IllegalArgumentException(
                    "The given persistor should not be null");
        }
        this.persistor = persistor;
        this.supplier = supplier;
    }

    /**
     * @see #xervice(Object, Supplier)
     */
    public final <T> T xervice(T impl) {
        return xervice(impl, DERIVE_IMPL_DECLARED);
    }

    /**
     * Create a proxyed service using {@link Enhancer#create()} via cglib
     *
     * @param impl     the instance of a concrete class
     * @param supplier the designated repos' supplier used by the {@link Proxervice#persistor}
     * @param <T>      the type of the [ impl ] instance
     */
    public final <T> T xervice(T impl, Supplier<Repo[]> supplier) {
        if (impl == null) {
            throw new IllegalArgumentException("The given impl should not be null");
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(impl.getClass());

        final Supplier<Repo[]> r = pick(impl, supplier);

        enhancer.setCallbacks(new MethodInterceptor[]{
                (o, method, args, methodProxy) -> invoke(impl, method, args, r)
        });

        return (T) enhancer.create();
    }

    /**
     * @see #xervice(Class, Object, Supplier)
     */
    public final <I, O extends I> I xervice(Class<I> spec, O impl) {
        return xervice(spec, impl, DERIVE_IMPL_DECLARED);
    }

    /**
     * Create a proxyed service using {@link Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)}
     *
     * @param spec     the specific interface which the [ impl ] should implement
     * @param impl     the instance of a concrete class implemented the given [ spec ]
     * @param supplier the designated repos' supplier used by the {@link Proxervice#persistor}
     * @param <I>      the type of the [ spec ] interface
     * @param <O>      the type of the [ impl ] instance
     */
    public final <I, O extends I> I xervice(Class<I> spec, O impl, Supplier<Repo[]> supplier) {
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

        final Supplier<Repo[]> r = pick(impl, supplier);

        return (I) Proxy.newProxyInstance(impl.getClass().getClassLoader(),
                new Class[]{spec},
                (proxy, method, args) -> invoke(impl, method, args, r));
    }

    private Object invoke(Object impl, Method method, Object[] args, Supplier<Repo[]> repos) throws Exception {
        if (should(impl.getClass(), method)) {
            try (Persistor<?>.Stub ignored =
                         persistor.enclose(null, repos.get())) {
                return method.invoke(impl, args);
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

    private Supplier<Repo[]> pick(Object impl, Supplier<Repo[]> supplier) {
        if (supplier == null) {
            if (this.supplier == null) {
                return deriver(impl);
            }
            return this.supplier;
        } else if (DERIVE_IMPL_DECLARED == supplier) {
            return deriver(impl);
        }
        return supplier;
    }

    private Supplier<Repo[]> deriver(Object impl) {
        try {
            Class<?> xclz = impl.getClass();
            List<Repo> li = new LinkedList<>();
            for (Field field : xclz.getDeclaredFields()) {
                if (Repo.class.isAssignableFrom(field.getType())) {
                    li.add((Repo) field.get(impl));
                }
            }
            Repo[] r = li.toArray(ZERO_REPOS);
            return () -> r;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return DERIVE_IMPL_DECLARED;
    }
}
