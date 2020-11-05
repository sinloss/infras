package com.sinlo.sponte;

import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.Typer;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The compile time constraint for annotations, mainly {@link Sponte} annotated
 *
 * @author sinlo
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Must.All.class)
@Documented
public @interface Must {

    String NAME = "com.sinlo.sponte.Must";

    /**
     * On where the constraint is applied
     */
    ElementKind[] value();

    /**
     * The class which the target element must extend
     */
    Class<?> extend() default All.class;

    /**
     * The class in which the target element must be
     */
    Class<?> in() default All.class;

    /**
     * The annotations with which the target element must be
     */
    Class<? extends Annotation> with() default All.class;

    /**
     * The specification
     *
     * @see Mirror#check(Must, Context.Subject)
     */
    Spec[] spec() default {};

    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface All {
        Must[] value();
    }

    @interface Spec {
        /**
         * The name of the property
         */
        String value() default "";

        /**
         * The class of the property value, if any
         */
        Class<?> clz() default Spec.class;

        /**
         * The primitive value of the property, if any
         */
        String val() default "";

        /**
         * If the specification adopt the logic "not" logic
         */
        boolean negative() default false;

        /**
         * If the specification adopt the logic "any" logic when the property value is a
         * collection
         */
        boolean any() default false;
    }

    enum Mirror {

        EXTEND(Must::extend, "The %s annotated element must extend the type %s"),
        IN(Must::in, "The type where %s annotated element reside must extend the type %s"),
        WITH(Must::with, "The %s must be annotated by the side of %s"),
        ;

        private final Function<Must, Class<?>> f;
        private final String message;

        Mirror(Function<Must, Class<?>> f, String message) {
            this.f = f;
            this.message = message;
        }

        /**
         * Check assignability
         */
        public void check(Must must, Context.Subject cs) {
            TypeMirror ref = get(must);
            if (ref == null) return;
            switch (this) {
                case EXTEND:
                case IN:
                    if (cs.ctx.types.isAssignable(cs.current.asType(), ref)) return;
                    break;
                case WITH:
                    AnnotationMirror am = Typer.annotated(cs.current, ref);
                    if (am != null) {
                        for (Spec spec : must.spec()) {
                            check(spec, am.getElementValues().entrySet().stream()
                                    .filter(e ->
                                            spec.value().equals(e.getKey().getSimpleName().toString()))
                                    .findFirst().map(Map.Entry::getValue).orElse(null), cs);
                        }
                        return;
                    }
                    break;
            }
            cs.error(String.format(message, cs.ctx.qname, ref.toString()));
        }

        /**
         * Check the specification for an annotation value
         */
        public void check(Spec spec, AnnotationValue v, Context.Subject cs) {
            Object value = v == null ? null : v.getValue();
            String specVal = spec.val();
            if (!specVal.isEmpty()) {
                if (value instanceof List) {
                    Stream<String> values = ((List<?>) value).stream().map(Object::toString);
                    if ((spec.any()
                            ? values.anyMatch(specVal::equals)
                            : values.allMatch(specVal::equals)) != spec.negative()) return;
                }
                if (value != null && specVal.equals(v.toString()) != spec.negative()) return;

                cs.error(String.format("The property [ %s ] must%s match %s",
                        spec.value(), spec.negative() ? " not" : "", specVal));
            }
            DeclaredType clz = Typer.mirrorType(spec::clz);
            if (!Spec.class.getCanonicalName().equals(clz.toString())) {
                if ((value instanceof TypeMirror &&
                        cs.ctx.types.isAssignable((TypeMirror) value, clz)) != spec.negative()) return;
                cs.error(String.format("The property [ %s ] must%s extend class %s",
                        spec.value(), spec.negative() ? " not" : "", clz.toString()));
            }
            cs.error("The @Spec must either specify a val() or a clz()");
        }

        /**
         * Get the underlying {@link TypeMirror} from a destined {@link MirroredTypeException} triggered
         * by the defined function, or null if the {@link TypeMirror} is a primitive class which is not
         * an available value
         */
        public TypeMirror get(Must must) {
            try {
                f.apply(must);
            } catch (MirroredTypeException e) {
                TypeMirror t = e.getTypeMirror();
                String name = t.toString();
                return All.class.getCanonicalName().equals(name)
                        || SponteAware.class.getCanonicalName().equals(name) ? null : t;
            }
            return null;
        }
    }
}
