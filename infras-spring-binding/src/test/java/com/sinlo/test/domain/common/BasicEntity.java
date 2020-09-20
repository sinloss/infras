package com.sinlo.test.domain.common;

import com.sinlo.core.domain.Persistor;
import com.sinlo.core.domain.spec.Entity;

public class BasicEntity implements Entity {

    public static final Persistor<BasicEntity> persistor = Persistor.of(BasicEntity.class)
            .on(Persistor.State.FORE, () -> System.out.println(
                    "Before all tags being committed"))
            .on(Persistor.State.AFT, () -> System.out.println(
                    "After all tags being committed"))
            .on(Persistor.State.FORE, t -> System.out.println("Before committing the entity [ "
                    .concat(String.valueOf(t.entity))
                    .concat(" ] in channel [ ")
                    .concat(String.valueOf(t.chan))
                    .concat(" ]"))).on(Persistor.State.AFT, t -> System.out.println("After the entity [ "
                    .concat(String.valueOf(t.entity))
                    .concat(" ] being committed in channel [ ")
                    .concat(String.valueOf(t.chan))
                    .concat(" ]")))
            .except(t -> System.out.println("Exception [ "
                    .concat(String.valueOf(t.ex))
                    .concat(" ] caught when committing [ ")
                    .concat(String.valueOf(t.entity))
                    .concat(" ] in channel [ ")
                    .concat(String.valueOf(t.chan))
                    .concat(" ]")));

    private String id;

    @Override
    public String id() {
        return id;
    }

    @Override
    public Persistor<BasicEntity> persistor() {
        return persistor;
    }
}
