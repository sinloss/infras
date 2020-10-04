package com.sinlo.spring.service.core;

import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Selector;
import com.sinlo.core.service.Proxistor;
import com.sinlo.spring.service.SpringProxistor;

import java.lang.annotation.Annotation;

@SuppressWarnings({"rawtypes", "ClassExplicitlyAnnotation"})
public class ProxistorAdapter implements Proxistor {

    private final SpringProxistor proxistor;

    public ProxistorAdapter(SpringProxistor proxistor) {
        this.proxistor = proxistor;
    }

    @Override
    public Class<? extends Entity> value() {
        return proxistor.value();
    }

    @Override
    public Class<? extends Selector> selector() {
        switch (proxistor.sel()) {
            case SPRING_MANAGED:
                return ManagedSelector.class;
            case CUSTOM:
                return proxistor.selector();
            case DERIVED:
            default:
                return Selector.class;
        }
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Proxistor.class;
    }
}
