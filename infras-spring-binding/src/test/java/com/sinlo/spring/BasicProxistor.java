package com.sinlo.spring;

import com.sinlo.core.service.Proxistor;
import com.sinlo.spring.domain.common.BasicEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Proxistor(BasicEntity.class)
public @interface BasicProxistor {
}
