package com.sinlo.spring.service.spec;

import com.sinlo.core.domain.spec.Entity;

import java.lang.annotation.*;

/**
 * The annotated class is going to be proxerviced
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxerviced {

    /**
     * The entity class used to get a corresponding {@link com.sinlo.core.domain.Persistor}
     */
    Class<? extends Entity> value();
}
