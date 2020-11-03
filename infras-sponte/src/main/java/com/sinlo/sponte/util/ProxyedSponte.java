package com.sinlo.sponte.util;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Agent;
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

    public final Sponte sponte;

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
        try {
            return sponte.key();
        } catch (NullPointerException ignored) {
            return new String[0];
        }
    }

    @SuppressWarnings("unchecked")
    public Class<? extends CompileAware> compiling() {
        try {
            return (Class<? extends CompileAware>) type(sponte::compiling);
        } catch (NullPointerException ignored) {
            return CompileAware.class;
        }
    }

    @Override
    public boolean inheritable() {
        try {
            return sponte.inheritable();
        } catch (NullPointerException ignored) {
            return true;
        }
    }

    @Override
    public Agent agent() {
        try {
            return sponte.agent();
        } catch (NullPointerException ignored) {
            return agent;
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

    /**
     * Default agent value
     */
    public static final Agent agent = ___.class.getAnnotation(Agent.class);

    @Agent(Agent.Bond.class)
    private static class ___ {
    }
}
