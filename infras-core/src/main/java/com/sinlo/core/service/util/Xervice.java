package com.sinlo.core.service.util;

import com.sinlo.core.prototype.Prototype;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

/**
 * Proxy creation util
 *
 * @author sinlo
 */
@SuppressWarnings("unchecked")
public class Xervice {

    private Xervice() {
    }

    /**
     * Create a proxyed service using {@link Enhancer#create()} via cglib
     *
     * @param impl    the instance of a concrete class
     * @param invoker the designated {@link InvocationHandler}
     * @param <O>     the type of the [ impl ] instance
     */
    public static <O> O xervice(O impl, InvocationHandler invoker) {
        if (impl == null) {
            throw new IllegalArgumentException("The given impl should not be null");
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(impl.getClass());

        enhancer.setCallbacks(new MethodInterceptor[]{
                (o, method, args, methodProxy) -> invoker.invoke(o, method, args)
        });

        return (O) enhancer.create();
    }

    /**
     * Create a proxyed service using {@link Proxy#newProxyInstance(ClassLoader, Class[],
     * InvocationHandler)}
     *
     * @param spec    the specific interface which the [ impl ] should implement
     * @param impl    the instance of a concrete class implemented the given [ spec ]
     * @param invoker the designated {@link InvocationHandler}
     * @param <I>     the type of the [ spec ] interface
     * @param <O>     the type of the [ impl ] instance
     */
    public static <I, O extends I> I xervice(Class<I> spec, O impl, InvocationHandler invoker) {
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

        return (I) Proxy.newProxyInstance(
                impl.getClass().getClassLoader(), new Class[]{spec}, invoker);
    }

    /**
     * Check if the method of the given class meets the conditions of being delegated
     *
     * @param exclude annotation that excludes the annotated method
     */
    public static <T extends Annotation, E extends Annotation> boolean should(Method method,
                                                                              Class<E> exclude) {
        Class<?> type = method.getDeclaringClass();
        return !Object.class.equals(type) && method.getAnnotation(exclude) == null;
    }

    /**
     * Derive a list consisted of all the matching declared fields in the given
     * impl object
     */
    public static <O, T> List<T> derive(O impl, Class<T> c) {
        final List<T> li = new LinkedList<>();

        Prototype.of((Class<O>) impl.getClass()).every(property -> {
            if (property.is(c)) {
                T repo = (T) property.get(impl);
                if (repo != null) li.add(repo);
            }
        });
        return li;
    }
}
