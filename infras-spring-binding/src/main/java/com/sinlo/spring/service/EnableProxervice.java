package com.sinlo.spring.service;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable the proxervice support by using the {@link ProxervicedProcessor}
 *
 * @author sinlo
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ProxervicedProcessor.class})
public @interface EnableProxervice {

    /**
     * The spring managed repos will be used as a fallback selector
     * if the assigned selector could not provide a proper repo when
     * this is set to true
     */
    boolean fallback() default false;
}
