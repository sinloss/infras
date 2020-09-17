package com.sinlo.spring.service;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ProxervicedProcessor.class)
public @interface EnableProxervice {

}
