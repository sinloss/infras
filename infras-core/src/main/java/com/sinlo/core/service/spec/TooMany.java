package com.sinlo.core.service.spec;

public class TooMany extends Exception {

    public static final Object MANY = new Object();

    public TooMany(Class<?> c) {
        super(String.format("Too many objects for the requested type [ %s ]", c));
    }

    @SuppressWarnings("unchecked")
    public static <T> T shouldNot(Object obj, Class<T> c) throws TooMany {
        if (obj == MANY) {
            throw new TooMany(c);
        }
        return (T) obj;
    }

    /**
     * A convenient {@link java.util.function.BiFunction} reference that returns always
     * the {@link #MANY}
     */
    public static Object really(Object __, Object ___) {
        return MANY;
    }
}
