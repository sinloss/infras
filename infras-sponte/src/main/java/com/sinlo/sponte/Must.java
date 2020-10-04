package com.sinlo.sponte;

import com.sinlo.sponte.spec.Perch;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.*;
import java.util.function.Consumer;
import java.util.function.Function;

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

    Class<?> UNASSIGNED = byte.class;

    /**
     * Where the constrained annotation should perch on
     */
    Perch value();

    /**
     * The class which the constrained annotation annotated element should extend
     */
    Class<?> extend() default byte.class;

    /**
     * The class which the constrained annotation annotated element should be in
     */
    Class<?> in() default byte.class;

    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface All {
        Must[] value();
    }

    enum Mirror {

        EXTEND(Must::extend),
        IN(Must::in),
        ;

        private final Function<Must, Class<?>> f;

        Mirror(Function<Must, Class<?>> f) {
            this.f = f;
        }

        /**
         * Check assignability
         */
        public void check(Must must, TypeMirror target, Types types, Consumer<TypeMirror> onFalse) {
            TypeMirror parent = get(must);
            if (parent != null && !types.isAssignable(target, parent)) {
                if (onFalse != null) onFalse.accept(parent);
            }
        }

        /**
         * Get the underlying {@link TypeMirror} from a destined {@link MirroredTypeException} triggered
         * by the defined function, or null if the {@link TypeMirror} is {@link #UNASSIGNED}
         */
        public TypeMirror get(Must must) {
            try {
                f.apply(must);
            } catch (MirroredTypeException e) {
                TypeMirror t = e.getTypeMirror();
                return UNASSIGNED.toString().equals(t.toString()) ? null : t;
            }
            return null;
        }
    }
}
