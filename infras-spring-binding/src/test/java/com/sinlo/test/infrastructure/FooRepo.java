package com.sinlo.test.infrastructure;

import com.sinlo.test.domain.BarEntity;
import org.springframework.stereotype.Repository;

@Repository
public class FooRepo extends BasicRepo<BarEntity, String> {

    private BarEntity store = null;

    public BarEntity get() {
        BarEntity barEntity = new BarEntity();
        if (store != null) {
            barEntity.setFoo(store.getFoo());
        }
        return barEntity;
    }

    @Override
    public void create(BarEntity barEntity) {
        this.store = barEntity;
    }

    @Override
    public void update(BarEntity barEntity) {
        this.store = barEntity;
    }

    @Override
    public void delete(BarEntity barEntity) {
        this.store = null;
    }
}
