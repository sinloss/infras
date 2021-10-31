package com.sinlo.sponte.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;

/**
 * The signature of a {@link ExecutableElement} or a {@link Method}, they share the same
 * format to maintain the consistency between the compile-time elements and the runtime
 * reflect objects
 *
 * @author sinlo
 */
public class Signature {

    public static final char SEPARATOR = '/';

    private final StringBuilder builder;

    private Signature(String name) {
        this.builder = new StringBuilder(name)
                .append(SEPARATOR);
    }

    private Signature append(String argument) {
        this.builder.append(argument).append(SEPARATOR);
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public static Signature of(ExecutableElement e) {
        Signature signature = new Signature(
                ((TypeElement) e.getEnclosingElement()).getQualifiedName().toString());
        signature.append(e.getSimpleName().toString());
        e.getParameters().stream()
                .map(Element::asType)
                .map(Objects::toString)
                .map(Typer::descriptor)
                .forEach(signature::append);
        return signature;
    }

    public static Signature of(Method m) {
        Signature signature = new Signature(m.getDeclaringClass().getCanonicalName());
        signature.append(m.getName());
        Arrays.stream(m.getParameters())
                .map(Parameter::getType)
                .map(Class::getName)
                .forEach(signature::append);
        return signature;
    }
}
