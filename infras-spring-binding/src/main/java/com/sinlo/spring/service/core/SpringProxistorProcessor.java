package com.sinlo.spring.service.core;

import com.sinlo.core.domain.persistor.spec.Repo;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.spring.service.SpringProxistor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.AnnotationMetadata;

/**
 * This processor is intended to replace the {@link SpringProxistor} annotated beans with delegated
 * proxies. And also record all the {@link Repo} beans to supply repos
 *
 * @author sinlo
 */
@SuppressWarnings("rawtypes")
public class SpringProxistorProcessor implements BeanPostProcessor, ImportAware, PriorityOrdered, ApplicationListener<ContextRefreshedEvent> {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> c = bean.getClass();
        if (Repo.class.isAssignableFrom(c)) {
            ManagedSelector.add((Repo) bean);
            return bean;
        }

        SpringProxistor proxistor = c.getAnnotation(SpringProxistor.class);
        if (proxistor == null) {
            Prototype.methods(c).noneMatch(
                    m -> m.isAnnotationPresent(SpringProxistor.class));
        }
        return bean;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata meta) {
    }

    /**
     * Make sure all other {@link BeanPostProcessor BeanPostProcessors} are applied
     * after all the services are proxerviced. So that something like @Transactional
     * could work for the proxerviced service
     */
    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

    }
}
