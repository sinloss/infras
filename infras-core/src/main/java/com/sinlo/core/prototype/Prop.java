package com.sinlo.core.prototype;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.spec.CompileAware;

import javax.lang.model.element.ElementKind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The property information for the {@link com.sinlo.core.prototype.spec.Property}
 *
 * @author sinlo
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Sponte(compiling = Prop.Must.class, inheritable = false)
@CompileAware.Neglect
public @interface Prop {

    String value();

    class Must implements CompileAware {

        @Override
        public void onCompile(Context.Subject cs) {
            if (!ElementKind.METHOD.equals(cs.kind)) return;

            String methodName = cs.current.getSimpleName().toString();
            if (Prototype.propertyName(methodName, "set") != null
                    || Prototype.propertyName(methodName, "get") != null) return;

            cs.error("The @Prop could only be annotated on setters or getters except fields");
        }
    }
}
