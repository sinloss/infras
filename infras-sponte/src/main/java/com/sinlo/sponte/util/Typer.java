package com.sinlo.sponte.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The type uitl
 *
 * @author sinlo
 */
public class Typer {

    private static final Map<String, String> PRIMITIVE_ARRAY = new HashMap<>();

    static {
        PRIMITIVE_ARRAY.put("byte[]", "[B");
        PRIMITIVE_ARRAY.put("short[]", "[S");
        PRIMITIVE_ARRAY.put("int[]", "[I");
        PRIMITIVE_ARRAY.put("long[]", "[J");
        PRIMITIVE_ARRAY.put("float[]", "[F");
        PRIMITIVE_ARRAY.put("double[]", "[D");
        PRIMITIVE_ARRAY.put("boolean[]", "[Z");
    }

    private static final Map<String, Class<?>> PRIMITIVE_TYPE = new HashMap<>();

    static {
        PRIMITIVE_TYPE.put("byte", byte.class);
        PRIMITIVE_TYPE.put("short", short.class);
        PRIMITIVE_TYPE.put("int", int.class);
        PRIMITIVE_TYPE.put("long", long.class);
        PRIMITIVE_TYPE.put("float", float.class);
        PRIMITIVE_TYPE.put("double", double.class);
        PRIMITIVE_TYPE.put("boolean", boolean.class);
    }

    /**
     * Returns possible primitive types before calling {@link Class#forName(String)}
     */
    public static Class<?> forName(String name) throws ClassNotFoundException {
        Class<?> primitive = PRIMITIVE_TYPE.get(name);
        if (primitive != null) return primitive;
        return Class.forName(name);
    }

    /**
     * Get the type descriptor of the given source code expression of a specific type
     *
     * @param name e.g. java.lang.Integer[]
     */
    public static String descriptor(String name) {
        if (name.endsWith("[]")) {
            String d = PRIMITIVE_ARRAY.get(name);
            if (d != null) return d;
            name = name.substring(0, name.length() - 2);
            boolean more = name.endsWith("[]");
            return "[".concat(more ? "" : "L")
                    .concat(descriptor(name)).concat(more ? "" : ";");
        }
        int lt = name.indexOf("<");
        if (lt != -1) {
            return name.substring(0, lt);
        }
        return name;
    }

    /**
     * Get the type descriptor of the given {@link TypeMirror}
     */
    public static String descriptor(TypeMirror typeMirror) {
        return descriptor(typeMirror.toString());
    }

    /**
     * Create a new instance of the given {@link Class type} using the default
     * constructor
     */
    public static <T> T create(Class<T> c) {
        try {
            return c.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Same as {@link #trace(Element, Function)} but with an identity function as the
     * {@code supervisor}
     */
    public static TypeElement trace(Element e) {
        return trace(e, t -> t);
    }

    /**
     * Trace the declared type of the given {@link Element}, and submit every
     * traced {@link Element} to the given {@code supervisor}
     */
    public static TypeElement trace(Element e,
                                    Function<Element, Element> supervisor) {
        if (supervisor == null) supervisor = t -> t;
        if (e instanceof TypeElement) {
            return (TypeElement) supervisor.apply(e);
        }
        Element enc = e.getEnclosingElement();
        if (enc == null || enc instanceof PackageElement) return null;

        TypeElement type = trace(enc, supervisor);
        supervisor.apply(e);
        return type;
    }
}
