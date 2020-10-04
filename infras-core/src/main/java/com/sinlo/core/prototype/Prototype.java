package com.sinlo.core.prototype;

import com.sinlo.core.common.wraparound.Iterational;
import com.sinlo.core.prototype.spec.Property;
import com.sinlo.core.prototype.spec.Retriever;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The prototype of a class
 *
 * @param <T> the type of the class of this prototype
 * @author sinlo
 */
public class Prototype<T> {

    private static final Pool.Simple<Prototype<?>> pool = new Pool.Simple<>();

    private final Map<String, Property<T, ?>> properties;

    public final Class<T> c;

    public final String name;

    private Prototype(Class<T> c) {
        this.properties = props(this.c = c);
        this.name = c.getName();
    }

    @SuppressWarnings("unchecked")
    public static <T> Prototype<T> of(Class<T> c) {
        if (c == null) return null;
        return (Prototype<T>) pool.get(c.getName(), () -> new Prototype<>(c));
    }

    /**
     * Get a {@link Stub}
     */
    public Stub stub(T t) {
        return new Stub(t);
    }

    /**
     * Get a {@link Property} with the given name
     */
    public Property<T, ?> property(String name) {
        return properties.get(name);
    }

    /**
     * Do the given action on every property
     */
    public void every(Consumer<Property<T, ?>> action) {
        properties.values().forEach(action);
    }

    /**
     * Create an instance of {@link T} using a {@link Retriever}
     */
    public T from(Retriever retriever) {
        return stub(Typer.create(c)).copy(retriever);
    }

    /**
     * Create a copy of a given {@link A}
     *
     * @param any    the given object which is the source
     * @param filter the filter who predicates if a property with the given name and
     *               type should be copied
     * @param <A>    the type of the given source object
     */
    public <A> T from(A any, BiFunction<String, Class<?>, Boolean> filter) {
        return stub(Typer.create(c)).copy(any, filter);
    }

    /**
     * Same as {@link #from(Object, BiFunction)} but filter nothing
     *
     * @see #from(Object, BiFunction)
     */
    public <A> T from(A any) {
        return from(any, (n, t) -> true);
    }

    /**
     * Create a clone of the given {@link T} by making use of the JDK serialization
     * mechanism
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(T t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(t);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the method fitting the name of camel case concatenated leading and filedName
     */
    public static Method fieldMethod(Class<?> c, String leading, String fieldName) {
        if (fieldName != null && !fieldName.isEmpty()) {
            try {
                return c.getMethod((leading == null ? "" : leading)
                        .concat(fieldName.substring(0, 1).toUpperCase())
                        .concat(fieldName.length() >= 2 ? fieldName.substring(1) : ""));
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    /**
     * Get a stream of methods of the given class and all its ancestors
     */
    @SuppressWarnings("rawtypes")
    public static Stream<Method> methods(Class<?> c) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                Iterational.of((Class) c,
                        (t, i) -> t != null && !Object.class.equals(t),
                        (t, i) -> t.getSuperclass()).iterator(),
                Spliterator.ORDERED), false)
                .flatMap(t -> Stream.of(t.getDeclaredMethods()));
    }

    /**
     * If the given class is instantiable
     */
    public static <T> boolean instantiable(final Class<T> clz) {
        return clz != null && !clz.isInterface()
                && !clz.isAnnotation() && !Modifier.isAbstract(clz.getModifiers());
    }

    /**
     * Get a property map fo the given class
     *
     * @param clz the given class
     * @param <T> the type generic of the given class
     */
    public static <T> Map<String, Property<T, ?>> props(final Class<T> clz) {
        Map<String, Property<T, ?>> props = new HashMap<>();
        Class<?> c = clz;
        for (; !Object.class.equals(c); c = c.getSuperclass()) {
            if (c == null) break;
            for (Field field : c.getDeclaredFields()) {
                Property<T, ?> prop = new Property<>(field);
                props.put(prop.name, prop);
            }
        }
        return props;
    }

    /**
     * The stub of an object which are targeted by the accessor
     */
    public class Stub {
        public final T t;

        public Stub(T t) {
            this.t = t;
        }

        /**
         * Is the property with the given name the given type ?
         *
         * @param name the given name of the property
         * @param type the estimated type of the property
         */
        public boolean is(String name, Class<?> type) {
            Property<T, ?> prop = Prototype.this.properties.get(name);
            return prop != null && prop.is(type);
        }

        /**
         * Get the value of the property with the given name
         *
         * @param name the given name
         */
        public Object get(String name) {
            Property<T, ?> prop = Prototype.this.properties.get(name);
            if (prop != null) {
                return prop.get(t);
            }
            return null;
        }

        /**
         * Set the value of the property with the given name
         *
         * @param name the given name
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Stub set(String name, Object value) {
            Property prop = Prototype.this.properties.get(name);
            if (prop != null) {
                prop.set(t, value);
            }
            return this;
        }

        /**
         * Do action on every property and its corresponding value
         */
        public Stub every(BiConsumer<Property<T, ?>, Object> action) {
            if (action != null) {
                Prototype.this.every(property ->
                        action.accept(property, property.get(t)));
            }
            return this;
        }

        /**
         * Copy properties from the given {@link Retriever}
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public T copy(Retriever retriever) {
            if (retriever == null) return null;
            for (Map.Entry<String, Property<T, ?>> entry : properties.entrySet()) {
                Property prop = entry.getValue();
                if (prop == null) continue;
                Object val = retriever.retrieve(entry.getKey(), prop.type, prop.get(t));
                if (val != Retriever.SKIP) {
                    prop.set(t, val);
                }
            }
            return t;
        }

        /**
         * Copy properties from the given {@link A}
         *
         * @param any    the given object which is the source
         * @param filter the filter who predicates if a property with the given name and
         *               type should be copied
         * @param <A>    the type of the given source object
         */
        @SuppressWarnings("unchecked")
        public <A> T copy(A any, BiFunction<String, Class<?>, Boolean> filter) {
            if (any == null) return null;

            Prototype<A> prototype = of((Class<A>) any.getClass());
            if (prototype == null) return null;

            return copy((n, t, v) -> {
                Property<A, ?> prop = prototype.property(n);
                if (prop != null && prop.is(t) && filter.apply(n, t)) {
                    return prop.get(any);
                }
                return Retriever.SKIP;
            });
        }

        /**
         * Same as {@link #copy(Object, BiFunction)} but filter nothing
         *
         * @see #copy(Object, BiFunction)
         */
        public <A> T copy(A any) {
            return copy(any, (n, t) -> true);
        }
    }
}
