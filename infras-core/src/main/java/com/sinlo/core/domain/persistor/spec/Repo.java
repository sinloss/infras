package com.sinlo.core.domain.persistor.spec;

import com.sinlo.core.common.util.Genericia;

/**
 * Repo the infrastructure repository
 *
 * @author sinlo
 */
public interface Repo<T extends Entity> extends Genericia.Aware<T> {

    void create(T t);

    void update(T t);

    void delete(T t);

}
