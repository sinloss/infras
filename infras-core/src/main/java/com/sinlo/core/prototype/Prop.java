package com.sinlo.core.prototype;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.spec.CompileAware;

import javax.lang.model.element.ElementKind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * The property information for the {@link com.sinlo.core.prototype.spec.Property}
 *
 * @author sinlo
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Sponte(compiling = Prop.Must.class, inheritable = false)
@Sponte.CompilingNeglect
public @interface Prop {

    /**
     * Property description
     */
    String value();

    /**
     * A specific comparator to compare the annotated property
     */
    Class<? extends Comparator> comparator() default Comparator.class;

    class Must implements CompileAware {

        private static final Set<String> annotated = new HashSet<>();

        @Override
        public void onCompile(Context.Subject cs) {
            String name = cs.current.getSimpleName().toString();
            if (ElementKind.METHOD.equals(cs.kind)) {
                final String methodName = name;
                if ((name = Prototype.propertyName(methodName, "set")) == null &&
                        (name = Prototype.propertyName(methodName, "get")) == null) {
                    cs.error("The @Prop could only be annotated on setters " +
                            "or getters except fields");
                    return;
                }
            }

            if (annotated.contains(name)) {
                cs.error("The @Prop could only be annotated once per property " +
                        "on any of its setter, getter or field");
                return;
            }
            annotated.add(name);
        }
    }
}
