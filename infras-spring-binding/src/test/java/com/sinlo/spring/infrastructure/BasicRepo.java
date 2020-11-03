package com.sinlo.spring.infrastructure;

import com.sinlo.core.domain.persistor.spec.Entity;
import com.sinlo.core.domain.persistor.spec.Repo;

public abstract class BasicRepo<P extends Entity, K> implements Repo<P> {
}
