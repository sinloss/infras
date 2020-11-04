package com.sinlo.sponte.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        PRIMITIVE_ARRAY.put("char[]", "[C");
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
        PRIMITIVE_TYPE.put("char", char.class);
    }

    private static final Map<String, String> PRIMITIVE_ZERO_VALUE = new HashMap<>();

    static {
        PRIMITIVE_ZERO_VALUE.put("byte", "0");
        PRIMITIVE_ZERO_VALUE.put("short", "0");
        PRIMITIVE_ZERO_VALUE.put("int", "0");
        PRIMITIVE_ZERO_VALUE.put("long", "0L");
        PRIMITIVE_ZERO_VALUE.put("float", "0.0");
        PRIMITIVE_ZERO_VALUE.put("double", "0.0");
        PRIMITIVE_ZERO_VALUE.put("boolean", "false");
        PRIMITIVE_ZERO_VALUE.put("char", "0");
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
     * Same as {@link #forName(String)} but for sure that the class could always be
     * found, thus would panic if not
     */
    public static Class<?> forSure(String name) {
        try {
            return forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
     * Get the zero value of the given type name
     */
    public static String zeroValue(String name) {
        return PRIMITIVE_ZERO_VALUE.getOrDefault(name, "null");
    }

    /**
     * Get the type descriptor of the given {@link TypeMirror}
     */
    public static String descriptor(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType) {
            return descriptor((TypeElement) ((DeclaredType) typeMirror).asElement());
        }
        return descriptor(typeMirror.toString());
    }

    /**
     * Get the descriptor of the given type
     */
    public static String descriptor(TypeElement te) {
        return descriptor(te, te.getSimpleName().toString());
    }

    public static String descriptor(TypeElement te, String name) {
        Element enc = te.getEnclosingElement();
        // the enclosing element is a package
        if (enc instanceof PackageElement) {
            PackageElement p = (PackageElement) enc;
            return p.isUnnamed() ? name : (p.getQualifiedName() + "." + name);
        }
        // means that the enclosed element is an inner class
        return descriptor((TypeElement) enc, enc.getSimpleName() + "$" + name);
    }

    /**
     * Create a new instance of the given name using the default constructor
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(String name) {
        try {
            return (T) forName(name).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException
                | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
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
     * Create a new instance of the given {@link Class type} using the constructor
     * with the given argument types, and arguments
     */
    public static <T> T create(Class<T> c, Class<?>[] argt, Object... args) {
        try {
            if (argt != null && argt.length >= 1) {
                return c.getDeclaredConstructor(argt).newInstance(args);
            }
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

    /**
     * Get the package where the given element resides
     */
    public static PackageElement where(Element te) {
        if (te instanceof PackageElement) {
            return (PackageElement) te;
        }
        return where(te.getEnclosingElement());
    }

    /**
     * Get the {@link AnnotationMirror} of the requested {@link TypeMirror} on the given
     * {@link AnnotatedConstruct}
     */
    public static AnnotationMirror annotated(AnnotatedConstruct e, TypeMirror requested) {
        List<? extends AnnotationMirror> ams = e.getAnnotationMirrors();
        for (AnnotationMirror am : ams) {
            if (am.getAnnotationType().equals(requested)) return am;
        }
        return null;
    }

    /**
     * Get assigned values inside of the requested {@link AnnotationMirror} which itself is in
     * a list of {@link AnnotationMirror}s of the given {@link AnnotatedConstruct}
     */
    public static Map<String, String> values(AnnotatedConstruct e, String requested) {
        List<? extends AnnotationMirror> ams = e.getAnnotationMirrors();
        for (AnnotationMirror am : ams) {
            if (am.getAnnotationType().toString().equals(requested)) {
                final Map<String, String> values = new HashMap<>();
                am.getElementValues().forEach((k, v) ->
                        values.put(k.getSimpleName().toString(), v.toString()));
                return values;
            }
        }
        return null;
    }

    /**
     * Get all elements defined in the given enclosing element
     */
    public static Set<String> values(TypeElement enc) {
        return enc.getEnclosedElements().stream()
                .map(Element::getSimpleName)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    /**
     * Check if the given {@link Element} is abstract, which means it contains an
     * {@link java.lang.reflect.Modifier#ABSTRACT abstract} modifier
     */
    public static boolean isAbstract(Element te) {
        return te.getModifiers().contains(Modifier.ABSTRACT);
    }

    /**
     * Get the super element of the given {@link TypeElement}
     *
     * @return the super {@link TypeElement} or null if the super type is {@link NoType}
     * @see TypeElement#getSuperclass()
     */
    public static TypeElement superElement(TypeElement te) {
        TypeMirror t = te.getSuperclass();
        if (t instanceof NoType) {
            return null;
        }
        return (TypeElement) ((DeclaredType) t).asElement();
    }

    /**
     * Get all methods of the given implicit {@link TypeElement}
     */
    public static Stream<ExecutableElement> methods(Element te) {
        if (!(te instanceof TypeElement)) return Stream.empty();
        return te.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .map(e -> (ExecutableElement) e);
    }

    /**
     * Get the underlying {@link TypeElement}from a destined {@link MirroredTypeException} triggered
     * by the given supplier
     *
     * @see #mirrorType(Supplier)
     */
    public static TypeElement mirror(Supplier<Class<?>> s) {
        return (TypeElement) mirrorType(s).asElement();
    }

    /**
     * Get the underlying {@link TypeMirror} from a destined {@link MirroredTypeException} triggered
     * by the given supplier
     */
    public static DeclaredType mirrorType(Supplier<Class<?>> s) {
        try {
            s.get();
            throw new RuntimeException(
                    "The given supplier did not throw a MirroredTypeException, which it should");
        } catch (MirroredTypeException e) {
            return (DeclaredType) e.getTypeMirror();
        }
    }

    /**
     * Get the underlying {@link TypeMirror}s from a destined {@link MirroredTypesException} triggered
     * by the given supplier
     */
    public static DeclaredType[] mirrorTypes(Supplier<Class<?>[]> s) {
        try {
            s.get();
            throw new RuntimeException(
                    "The given supplier did not throw a MirroredTypesException, which it should");
        } catch (MirroredTypesException e) {
            return e.getTypeMirrors().stream()
                    .map(t -> (DeclaredType) t)
                    .toArray(DeclaredType[]::new);
        }
    }
}
