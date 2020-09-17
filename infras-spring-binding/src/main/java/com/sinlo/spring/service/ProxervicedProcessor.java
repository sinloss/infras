package com.sinlo.spring.service;

import com.sinlo.core.domain.Persistor;
import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.service.Proxervice;
import com.sinlo.spring.service.spec.Proxerviced;
import com.sinlo.spring.service.spec.UnsupportedEntityTypeException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashMap;
import java.util.Map;

public class ProxervicedProcessor implements BeanPostProcessor {

    private final Map<Persistor<? extends Entity>, Proxervice> pool = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Proxerviced proxerviced = bean.getClass().getAnnotation(Proxerviced.class);
        if (proxerviced == null) return bean;

        Persistor<? extends Entity> persitor = Persistor.of(proxerviced.value());
        if (persitor == null) {
            throw new UnsupportedEntityTypeException(proxerviced.value());
        }
        return proxervice(persitor).xervice(bean);
    }

    private Proxervice proxervice(Persistor<? extends Entity> persitor) {
        Proxervice proxervice = pool.get(persitor);
        if (proxervice == null) {
            pool.put(persitor, proxervice = new Proxervice(persitor));
        }
        return proxervice;
    }
}
