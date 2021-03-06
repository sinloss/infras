package com.sinlo.sponte.spec;

import com.sinlo.sponte.util.Typer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The perch of the sponted annotation
 *
 * @author sinlo
 */
public enum Perch {
    PARAMETER("&", e -> "", ElementKind.PARAMETER),
    CONSTRUCTOR(":", e -> "", ElementKind.CONSTRUCTOR),
    METHOD("#", ElementKind.METHOD),
    FIELD("@", ElementKind.FIELD),
    TYPE("", e -> Typer.descriptor((TypeElement) e),
            ElementKind.CLASS, ElementKind.ENUM,
            ElementKind.INTERFACE, ElementKind.ANNOTATION_TYPE);

    public final String sign;
    public final Function<Element, String> namepicker;
    public final ElementKind[] kinds;

    Perch(String sign, ElementKind... kinds) {
        this(sign, e -> e.getSimpleName().toString(), kinds);
    }

    Perch(String sign, Function<Element, String> namepicker, ElementKind... kinds) {
        this.sign = sign;
        this.namepicker = namepicker;
        this.kinds = kinds;
    }

    public void pend(Element e, StringBuilder builder) {
        builder.append(sign).append(namepicker.apply(e));
    }

    private static final Map<ElementKind, Perch> kindMap = new HashMap<>();

    static {
        for (Perch perch : values()) {
            for (ElementKind kind : perch.kinds) {
                kindMap.put(kind, perch);
            }
        }
    }

    /**
     * Get the type name of a given expression
     */
    public static String typeName(String expr) {
        if (expr == null) return "";
        for (Perch perch : values()) {
            int idx;
            if (perch.sign != null && !perch.sign.isEmpty()
                    && (idx = expr.indexOf(perch.sign)) != -1)
                return expr.substring(0, idx);
        }
        return expr;
    }

    /**
     * Identify the perch type of the given expression
     */
    public static Perch identify(String expr) {
        for (Perch perch : values()) {
            if (expr.contains(perch.sign)) {
                return perch;
            }
        }
        return null;
    }

    /**
     * Get the corresponding {@link Perch} regarding the given {@link ElementKind}
     */
    public static Perch regard(ElementKind kind) {
        return kindMap.get(kind);
    }

    /**
     * All supported {@link ElementType element types} sorted in order
     */
    public static final int[] SUPPORTED = {ElementType.TYPE.ordinal(),
            ElementType.FIELD.ordinal(), ElementType.METHOD.ordinal(),
            ElementType.PARAMETER.ordinal(), ElementType.CONSTRUCTOR.ordinal(),
            ElementType.ANNOTATION_TYPE.ordinal()};

    /**
     * Check if the given {@link ElementType type} is supported
     */
    public static boolean support(ElementType type) {
        return Arrays.binarySearch(SUPPORTED, type.ordinal()) >= 0;
    }
}
