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
@Import(ProxervicedProcessor.class)
public @interface EnableProxervice {

}
