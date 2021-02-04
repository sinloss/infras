package com.sinlo.spring.service.core;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Basic registrar
 *
 * @author sinlo
 */
public abstract class Registrar implements ImportBeanDefinitionRegistrar {

    /**
     * ResourceLoader
     */
    protected ResourceLoader resourceLoader;
    /**
     * current bean definition registry
     */
    protected BeanDefinitionRegistry registry;

    protected AnnotationMetadata metadata;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        this.registry = registry;
        this.metadata = importingClassMetadata;
        this.registering();
    }

    protected abstract void registering();

    /**
     * Register a bean without constructor arguments or property values
     *
     * @see #register(String, Class, ConstructorArgumentValues, MutablePropertyValues)
     */
    protected BeanReference register(String beanName, Class<?> beanClass) {
        return this.register(beanName, beanClass, null, null);
    }

    /**
     * CRegister a bean with constructor arguments, and without constructor arguments
     *
     * @see #register(String, Class, ConstructorArgumentValues, MutablePropertyValues)
     */
    protected BeanReference register(String beanName, Class<?> beanClass, ConstructorArgumentValues args) {
        return this.register(beanName, beanClass, args, null);
    }

    /**
     * Register a bean with constructor arguments, and without constructor arguments
     *
     * @see #register(String, Class, ConstructorArgumentValues, MutablePropertyValues)
     */
    protected BeanReference register(String beanName, Class<?> beanClass, MutablePropertyValues propertyValues) {
        return this.register(beanName, beanClass, null, propertyValues);
    }

    /**
     * Register a bean
     *
     * @param beanName       the name of the bean
     * @param beanClass      the type of the bean
     * @param args           constructor arguments
     * @param propertyValues the field values
     * @return bean reference that can be easily used in other bean registering
     */
    protected BeanReference register(String beanName, Class<?> beanClass, ConstructorArgumentValues args, MutablePropertyValues propertyValues) {
        if (!this.registry.containsBeanDefinition(beanName)) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanClass);
            beanDefinition.setSynthetic(false);
            if (propertyValues != null) beanDefinition.setPropertyValues(propertyValues);
            if (args != null) beanDefinition.setConstructorArgumentValues(args);
            this.registry.registerBeanDefinition(beanName, beanDefinition);
        }
        return new RuntimeBeanReference(beanName);
    }
}
