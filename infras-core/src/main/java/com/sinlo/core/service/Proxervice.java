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

/**
 * The proxyed service creator
 * <p/>
 * All the methods of the original service would be wrapped in a {@link Persistor#enclose(Consumer, Repo[])}
 * process except for the methods annotated by the {@link ProxerviceIgnore}
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Proxervice {

    public final Persistor<?> persistor;
    public final Repo[] repos;

    /**
     * @param persistor {@link Persistor}
     * @param repos     the default repos
     */
    public Proxervice(Persistor<?> persistor, Repo... repos) {
        if (persistor == null) {
            throw new IllegalArgumentException(
                    "The given persistor should not be null");
        }
        this.persistor = persistor;
        this.repos = repos == null ? new Repo[0] : repos;
    }

    /**
     * Create a proxyed service using {@link Enhancer#create()} via cglib
     *
     * @param impl  the instance of a concrete class
     * @param repos the designated repos used by the {@link Proxervice#persistor}
     * @param <T>   the type of the [ impl ] instance
     */
    public final <T> T xervice(T impl, Repo... repos) {
        if (impl == null) {
            throw new IllegalArgumentException("The given impl should not be null");
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(impl.getClass());

        final Repo[] r = pick(impl, repos);
        enhancer.setCallbacks(new MethodInterceptor[]{
                (o, method, args, methodProxy) -> invoke(impl, method, args, r)
        });

        return (T) enhancer.create();
    }

    /**
     * Create a proxyed service using {@link Proxy#newProxyInstance(ClassLoader, Class[], InvocationHandler)}
     *
     * @param spec  the specific interface which the [ impl ] should implement
     * @param impl  the instance of a concrete class implemented the given [ spec ]
     * @param repos the designated repos used by the {@link Proxervice#persistor}
     * @param <I>   the type of the [ spec ] interface
     * @param <O>   the type of the [ impl ] instance
     */
    public final <I, O extends I> I xervice(Class<I> spec, O impl, Repo... repos) {
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

        final Repo[] r = pick(impl, repos);

        return (I) Proxy.newProxyInstance(impl.getClass().getClassLoader(),
                new Class[]{spec},
                (proxy, method, args) -> invoke(impl, method, args, r));
    }

    private Object invoke(Object impl, Method method, Object[] args, Repo... repos) throws Exception {
        if (should(impl.getClass(), method)) {
            try (Persistor<?>.Stub ignored =
                         persistor.enclose(null, repos)) {
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

    private Repo[] pick(Object impl, Repo... repos) {
        Repo[] r = (repos == null || repos.length == 0) ? this.repos : repos;
        if (r.length == 0) {
            Class<?> xclz = impl.getClass();
            List<Repo> li = new LinkedList<>();
            try {
                for (Field field : xclz.getDeclaredFields()) {
                    if (Repo.class.isAssignableFrom(field.getType())) {
                        li.add((Repo) field.get(impl));
                    }
                }
                r = li.toArray(new Repo[0]);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return r;
    }
}
