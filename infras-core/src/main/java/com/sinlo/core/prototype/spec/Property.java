package com.sinlo.core.prototype.spec;

import com.sinlo.core.prototype.Prototype;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The property
 *
 * @param <T> the type of the declaring class of this property
 * @param <V> the type of this property
 * @author sinlo
 */
public class Property<T, V> {

    public final String name;

    public final Class<V> type;

    public final Field field;

    public final Method setter;

    public final Method getter;

    @SuppressWarnings("unchecked")
    public Property(Field field) {
        name = (this.field = field).getName();
        this.type = (Class<V>) field.getType();
        Class<?> c = this.field.getDeclaringClass();
        setter = Prototype.fieldMethod(c, "set", name);
        getter = Prototype.fieldMethod(c, "set", name);
        this.field.setAccessible(true);
    }

    /**
     * Check if this property is assignable to the given type
     */
    public boolean is(Class<?> type) {
        return type != null && type.isAssignableFrom(this.type);
    }

    public void set(T obj, V value) {
        if (setter != null) try {
            setter.invoke(obj, value);
            return;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public V get(T obj) {
        if (getter != null) try {
            return (V) getter.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        try {
            return (V) field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
