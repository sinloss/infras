package com.sinlo.spring.service.spec;

import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.domain.spec.Selector;

import java.lang.annotation.*;

/**
 * The annotated class is going to be proxerviced
 *
 * @author sinlo
 */
@SuppressWarnings("rawtypes")
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxerviced {

    /**
     * The entity class used to get a corresponding {@link com.sinlo.core.domain.Persistor}
     */
    Class<? extends Entity> value();

    /**
     * Selector type
     */
    Sel sel() default Sel.DERIVED;

    /**
     * Used when {@link #sel()} is set as {@link Sel#CUSTOM}
     */
    Class<Selector> selector() default Selector.class;

    enum Sel {
        DERIVED, SPRING_MANAGED, CUSTOM
    }
}
