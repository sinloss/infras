package com.sinlo.core.domain.spec;

import com.sinlo.core.domain.Persistor;

/**
 * Entity the domain entity
 *
 * @author sinlo
 */
@SuppressWarnings("unchecked")
public interface Entity {

    /**
     * @return the id of this entity
     */
    String id();

    /**
     * @return a proper persistor for this entity
     */
    @SuppressWarnings("rawtypes")
    Persistor persistor();

    /**
     * @return the entity key
     */
    default String key() {
        String id = id();
        return getClass().getName()
                + "#"
                + (id == null ? String.valueOf(this.hashCode()) : id);
    }

    /**
     * tag as create
     */
    default void create() {
        persistor().tag(Channel.CREATE, this);
    }

    /**
     * tag as update
     */
    default void update() {
        if (persistor().stat(this) != Channel.CREATE) {
            // as long as this is tagged as create, it infers that there's no existing entity
            // in the repository, thus here it should be tagged as create as well
            create();
        } else {
            persistor().tag(Channel.UPDATE, this);
        }
    }

    /**
     * tag as delete
     */
    default void delete() {
        persistor().tag(Channel.DELETE, this);
    }
}
