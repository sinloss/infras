package com.sinlo.core.prototype.spec;

import com.sinlo.core.prototype.Prop;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.sponte.util.Pool;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The property
 *
 * @param <T> the type of the declaring class of this property
 * @param <V> the type of this property
 * @author sinlo
 */
public class Property<T, V> {

    private final Pool.Simple<Annotation[]> notes = new Pool.Simple<>();

    public final String name;

    public final Class<V> type;

    public final Field field;

    public final Method setter;

    public final Method getter;

    private Property(String name, Class<V> type, Field field, Method setter, Method getter) {
        this.name = name;
        this.type = type;
        this.field = field;
        this.setter = setter;
        this.getter = getter;
        if (this.field != null) this.field.setAccessible(true);
        if (this.setter != null) this.setter.setAccessible(true);
        if (this.getter != null) this.getter.setAccessible(true);
    }

    /**
     * Create a property from the given {@link Field} without any fallbacks
     */
    public static <T, V> Property<T, V> of(Field field) {
        return of(field, null, null);
    }

    /**
     * Create a property from the given {@link Field}, and use the fallback setter or getter if could
     * not find them of all public methods using the given field
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Property<T, V> of(Field field, Method fallbackSetter, Method fallbackGetter) {
        final Class<?> c = field.getDeclaringClass();
        final String name = field.getName();
        Method s = Prototype.fieldMethod(c, "set", name, field.getType());
        Method g = Prototype.fieldMethod(c, "get", name);
        return new Property<>(name, (Class<V>) field.getType(), field,
                s == null ? fallbackSetter : s, g == null ? fallbackGetter : g);
    }

    /**
     * Create a property from the given {@link Method setter}, and use the given fallback if could not
     * find the field in the setter declaring class
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Property<T, V> bySetter(Method setter, Field fallback) {
        String name = setter.getName();
        Class<?>[] types = setter.getParameterTypes();
        if ((name = Prototype.propertyName(name, "set")) == null || types.length != 1)
            throw new IllegalArgumentException("The given method is not a valid setter");
        Class<?> c = setter.getDeclaringClass();
        return new Property<>(name, (Class<V>) types[0], pick(c, name, fallback),
                setter, Prototype.fieldMethod(c, "get", name));
    }

    /**
     * Create a property from the given {@link Method getter}, and use the given fallback if could not
     * find the field in the getter declaring class
     */
    @SuppressWarnings("unchecked")
    public static <T, V> Property<T, V> byGetter(Method getter, Field fallback) {
        String name = getter.getName();
        if ((name = Prototype.propertyName(name, "get")) == null)
            throw new IllegalArgumentException("The given method is not a valid getter");
        Class<?> c = getter.getDeclaringClass();
        return new Property<>(name, (Class<V>) getter.getReturnType(), pick(c, name, fallback),
                Prototype.fieldMethod(c, "set", name, getter.getReturnType()), getter);
    }

    private static Field pick(Class<?> c, String name, Field fallback) {
        try {
            return c.getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
            return fallback;
        }
    }

    /**
     * If this all the property accessing element is populated
     */
    public boolean fullyPopulated() {
        return field != null && setter != null && getter != null;
    }

    /**
     * Check if this property is assignable to the given type
     */
    public boolean is(Class<?> type) {
        return type != null && type.isAssignableFrom(this.type);
    }

    /**
     * Set the value of this property from the given object, priorly uses setter if any
     */
    public void set(T obj, V value) {
        if (setter != null) try {
            setter.invoke(obj, value);
            return;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        if (field != null) try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the value of this property from the given object, priorly uses getter if any
     */
    @SuppressWarnings("unchecked")
    public V get(T obj) {
        if (getter != null) try {
            return (V) getter.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        if (field != null) try {
            return (V) field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get and cache annotations of the given type from all possible places, which includes
     * {@link #field}, {@link #getter} and {@link #setter}
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A[] notes(Class<A> type) {
        return (A[]) notes.get(type.getName(), () ->
                Stream.<AnnotatedElement>of(field, getter, setter)
                        .filter(Objects::nonNull)
                        .map(e -> e.getAnnotation(type))
                        .filter(Objects::nonNull)
                        .toArray(i -> (A[]) Array.newInstance(type, i)));
    }

    /**
     * Specifically get and cache the {@link Prop}
     */
    public Prop[] props() {
        return notes(Prop.class);
    }
}
