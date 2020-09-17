package com.sinlo.spring.service.spec;

import com.sinlo.core.domain.spec.Entity;

/**
 * @author sinlo
 */
public class UnsupportedEntityTypeException extends RuntimeException {

    public UnsupportedEntityTypeException(Class<? extends Entity> clz) {
        super("The specified entity type [ "
                .concat(String.valueOf(clz))
                .concat(" ] does not have a corresponding persistor"));
    }
}
