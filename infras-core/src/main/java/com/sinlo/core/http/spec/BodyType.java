package com.sinlo.core.http.spec;

/**
 * Type of the body
 *
 * @author sinlo
 */
public enum BodyType {

    MULTIPART("multipart/form-data"),
    FORM("application/x-www-form-urlencoded"),
    JSON("application/json"),
    XML("application/xml"),
    YAML("text/yaml"),
    EDN("application/edn");

    public final String value;

    BodyType(String value) {
        this.value = value;
    }
}
