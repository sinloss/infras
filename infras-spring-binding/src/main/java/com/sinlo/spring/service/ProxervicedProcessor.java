package com.sinlo.spring.service;

import com.sinlo.core.domain.Persistor;
import com.sinlo.core.domain.spec.Entity;
import com.sinlo.core.domain.spec.Repo;
import com.sinlo.core.service.Proxervice;
import com.sinlo.spring.service.spec.Proxerviced;
import com.sinlo.spring.service.spec.UnsupportedEntityTypeException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("rawtypes")
public class ProxervicedProcessor implements BeanPostProcessor {

    private final Map<Persistor<? extends Entity>, Proxervice> pool = new HashMap<>();
    private final List<Repo> repos = new LinkedList<>();
    private final Supplier<Repo[]> supplier = () -> repos.toArray(Proxervice.ZERO_REPOS);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> bclz = bean.getClass();
        if (Repo.class.isAssignableFrom(bclz)) {
            repos.add((Repo) bean);
            return bean;
        }

        Proxerviced proxerviced = bclz.getAnnotation(Proxerviced.class);
        if (proxerviced == null) return bean;

        Persistor<? extends Entity> persitor = Persistor.of(proxerviced.value());
        if (persitor == null) {
            throw new UnsupportedEntityTypeException(proxerviced.value());
        }
        if (proxerviced.deriveRepos()) {
            return proxervice(persitor).xervice(bean, Proxervice.DERIVE_IMPL_DECLARED);
        }
        return proxervice(persitor).xervice(bean);
    }

    private Proxervice proxervice(Persistor<? extends Entity> persitor) {
        Proxervice proxervice = pool.get(persitor);
        if (proxervice == null) {
            pool.put(persitor, proxervice = new Proxervice(persitor, supplier));
        }
        return proxervice;
    }
}
