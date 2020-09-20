package com.sinlo.spring.service;

import com.sinlo.core.domain.Persistor;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.domain.spec.Selector;
import com.sinlo.core.service.Proxervice;
import com.sinlo.spring.service.spec.ManagedReposSelector;
import com.sinlo.spring.service.spec.Proxerviced;
import com.sinlo.spring.service.spec.UnsupportedEntityTypeException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * This processor is intended to replace the {@link Proxerviced} annotated beans with
 * {@link Proxervice} created proxies. And also record all the {@link Repo} beans to
 * supply repos for the proxerice
 *
 * @author sinlo
 */
@SuppressWarnings("rawtypes")
public class ProxervicedProcessor implements BeanPostProcessor, ImportAware, PriorityOrdered {

    private final Map<String, Selector> customSelectors = new HashMap<>();
    private final Map<Persistor, Proxervice> pool = new HashMap<>();

    private boolean fallback = false;

    @SuppressWarnings("unchecked")
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> bclz = bean.getClass();
        if (Repo.class.isAssignableFrom(bclz)) {
            ManagedReposSelector.SPRING_MANAGED.add((Repo) bean);
            return bean;
        }

        Proxerviced proxerviced = bclz.getAnnotation(Proxerviced.class);
        if (proxerviced == null) return bean;

        Persistor persitor = Persistor.of(proxerviced.value());
        if (persitor == null) {
            throw new UnsupportedEntityTypeException(proxerviced.value());
        }
        switch (proxerviced.sel()) {
            case DERIVED:

                return proxervice(persitor).xervice(bean,
                        Proxervice.DERIVE_IMPL_DECLARED, fallback);
            case SPRING_MANAGED:
                return proxervice(persitor).xervice(bean, null, fallback);
            case CUSTOM:
                try {
                    Class<Selector> cuz = proxerviced.selector();
                    String cuzName = cuz.getName();
                    if (Selector.class.getName().equals(cuzName)) {
                        throw new IllegalArgumentException(
                                "A valid selector must be provided");
                    }
                    Selector selector = customSelectors.get(cuzName);
                    if (selector == null) {
                        customSelectors.put(cuzName,
                                selector = cuz.getDeclaredConstructor().newInstance());
                    }
                    return proxervice(persitor).xervice(bean, selector, fallback);
                } catch (InstantiationException | IllegalAccessException
                        | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
        }
        return proxervice(persitor).xervice(bean, null);
    }

    @SuppressWarnings("unchecked")
    private Proxervice proxervice(Persistor persitor) {
        Proxervice proxervice = pool.get(persitor);
        if (proxervice == null) {
            pool.put(persitor, proxervice = new Proxervice(persitor,
                    ManagedReposSelector.SPRING_MANAGED));
        }
        return proxervice;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata meta) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(meta.getAnnotationAttributes(
                EnableProxervice.class.getName(), true));
        if (attributes != null) {
            fallback = attributes.getBoolean("fallback");
        }
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
}
