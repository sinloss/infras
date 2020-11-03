package com.sinlo.spring.service.core;

import com.sinlo.core.service.Ponded;
import com.sinlo.sponte.SponteInitializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Optional;

/**
 * This processor is intended to replace the beans with delegates from the {@link com.sinlo.core.service.Pond}
 *
 * @author sinlo
 */
public class PondJoiner implements BeanPostProcessor, ImportAware, PriorityOrdered, ApplicationListener<ContextRefreshedEvent> {

    @SuppressWarnings("rawtypes")
    private final Map<String, Ponded.KeeperValue> keepers = Ponded.Manifest.keeperMap();

    @SuppressWarnings({"unchecked"})
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final Class<?> c = bean.getClass();
        return Optional.ofNullable(keepers.get(c.getName()))
                .map(kpv -> kpv.maintain(c, bean, null))
                .orElse(bean);
    }

    @Override
    public void setImportMetadata(AnnotationMetadata meta) {
    }

    /**
     * Make sure all other {@link BeanPostProcessor BeanPostProcessors} are applied
     * after all the services are delegated. So that something like @Transactional
     * could work for the delegated service
     */
    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

    /**
     * Cleanup
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        keepers.values().forEach(v -> v.keeper.finale(SponteInitializer.DECLINING));
        keepers.clear();
    }
}
