package com.sinlo.sponte.util;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.SponteAware;

import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import java.lang.annotation.Annotation;
import java.util.function.Supplier;

/**
 * The proxy of {@link Sponte} who tolerates the {@link MirroredTypeException} and
 * returns the underlying {@link Class}
 *
 * @author sinlo
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
public class ProxyedSponte implements Sponte {

    private final Sponte sponte;

    public ProxyedSponte(Element e) {
        this.sponte = e.getAnnotation(Sponte.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SponteAware> value() {
        try {
            return (Class<? extends SponteAware>) type(sponte::value);
        } catch (NullPointerException ignored) {
            return SponteAware.class;
        }
    }

    @Override
    public String[] key() {
        return sponte.key();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends CompileAware> compiling() {
        try {
            return (Class<? extends CompileAware>) type(sponte::compiling);
        } catch (NullPointerException ignored) {
            return CompileAware.class;
        }
    }

    public static Class<?> type(Supplier<Class<?>> supplier) {
        try {
            return supplier.get();
        } catch (MirroredTypeException mte) {
            try {
                return Class.forName(mte.getTypeMirror().toString());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Sponte.class;
    }
}
