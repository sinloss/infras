package com.sinlo.core.domain.spec;

/**
 * Entity the domain entity
 *
 * @author sinlo
 */
public interface Entity {

    /**
     * @return the entity key
     */
    default String key() {
        return this.getClass().getName() + "#" + id();
    }

    /**
     * @return the id of this entity
     */
    String id();

}
