package com.sinlo.core.domain.persistor.spec;

import com.sinlo.core.domain.persistor.Persistor;

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
     * This method could be overridden to provide the expected persistor directly
     * instead of tracing all the ancestors
     *
     * @return a proper persistor for this entity
     * @see Persistor#find(Class)
     */
    @SuppressWarnings("rawtypes")
    default Persistor persistor() {
        Persistor<? extends Entity> persistor = Persistor.find(this.getClass());
        if (persistor != null) {
            return persistor;
        }
        throw new IllegalStateException("Could not find a valid persistor for [ "
                .concat(this.getClass().getName()).concat(" ]"));
    }

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
        persistor().tag(Tag.Channel.CREATE, this);
    }

    /**
     * tag as update
     */
    default void update() {
        if (persistor().stat(this) == Tag.Channel.CREATE) {
            // as long as this is tagged as create, it infers that there's no existing entity
            // in the repository, thus here it should be tagged as create as well
            create();
        } else {
            persistor().tag(Tag.Channel.UPDATE, this);
        }
    }

    /**
     * tag as delete
     */
    default void delete() {
        persistor().tag(Tag.Channel.DELETE, this);
    }
}
