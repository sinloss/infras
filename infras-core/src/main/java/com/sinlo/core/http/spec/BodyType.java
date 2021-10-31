package com.sinlo.core.http.spec;

import com.sinlo.core.http.Fetcha;

/**
 * Type of the body
 *
 * @author sinlo
 */
public enum BodyType {

    /**
     * Will no body-type be presented
     */
    NONE("") {
        /**
         * Remove the needed header of any {@link BodyType} from the given
         * {@link Fetcha}
         */
        @Override
        public void set(Fetcha<?> fetcha) {
            fetcha.getHeaders().remove(HEADER);
        }
    },
    MULTIPART("multipart/form-data"),
    FORM("application/x-www-form-urlencoded"),
    JSON("application/json"),
    XML("application/xml"),
    YAML("text/yaml"),
    EDN("application/edn");

    public static final String HEADER = "Content-Type";

    public final String value;

    BodyType(String value) {
        this.value = value;
    }

    /**
     * Set the needed header of the current {@link BodyType} to the given
     * {@link Fetcha}
     */
    public void set(Fetcha<?> fetcha) {
        fetcha.header(HEADER, this.value);
    }
}
