package com.sinlo.spring.service;


import com.sinlo.spring.service.pond.PondJoiner;
import com.sinlo.spring.service.pond.PondRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable the {@link com.sinlo.core.service.Proxistor} support by importing the
 * {@link PondJoiner}
 *
 * @author sinlo
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({PondRegistrar.class, PondJoiner.class})
public @interface JoinPond {

    /**
     * To automatically register all ponded objects as spring beans. If it is set false none
     * of the ponded objects will be registered unless they are explicitly annotated as
     * spring beans
     */
    boolean auto() default true;
}