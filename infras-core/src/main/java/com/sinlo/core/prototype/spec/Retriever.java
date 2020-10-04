package com.sinlo.core.prototype.spec;

/**
 * The property value retriever
 *
 * @author sinlo
 */
@FunctionalInterface
public interface Retriever {

    /**
     * Represent the logic concept of skip
     */
    Object SKIP = new Object();

    default Object retrieve(String name, Class<?> type) {
        return retrieve(name, type, null);
    }

    /**
     * Retrieve a property value of the given name and type, cloud return
     * the {@link #SKIP} to express the logic concept of skip
     */
    Object retrieve(String name, Class<?> type, Object originValue);
}
