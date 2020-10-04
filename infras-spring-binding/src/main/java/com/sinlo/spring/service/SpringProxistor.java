package com.sinlo.spring.service;

import com.sinlo.core.domain.persistor.Persistor;
import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Selector;
import com.sinlo.spring.service.core.SpringProxistorProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * The annotated class is going to be delegated
 *
 * @author sinlo
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpringProxistor {

    /**
     * The entity class used to get a corresponding {@link Persistor}
     */
    Class<? extends Entity> value();

    /**
     * Selector type
     */
    Sel sel() default Sel.DERIVED;

    /**
     * Used when {@link #sel()} is set as {@link Sel#CUSTOM}
     */
    Class<? extends Selector> selector() default Selector.class;

    enum Sel {
        DERIVED, SPRING_MANAGED, CUSTOM
    }

    /**
     * Enable the {@link SpringProxistor} support by importing the
     * {@link SpringProxistorProcessor}
     *
     * @author sinlo
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Import({SpringProxistorProcessor.class})
    @interface Enable {
    }
}
