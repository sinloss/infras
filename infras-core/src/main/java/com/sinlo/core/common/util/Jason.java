package com.sinlo.core.common.util;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Jason the json util
 *
 * @author sinlo
 */
public class Jason {

    private Jason() {
    }

    private static ObjectMapper om = new ObjectMapper()
            // default date format
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    /**
     * Stringify the given object
     */
    public static String stringify(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#readTree(String)
     */
    public static JsonNode parse(String json) {
        try {
            return om.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#readValue(String, Class)
     */
    public static <T> T parse(String json, Class<T> clz) {
        try {
            return om.readValue(json, clz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#readValue(String, TypeReference)
     */
    public static <T> T parse(String json, TypeReference<T> typeReference) {
        try {
            return om.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#writeValueAsBytes(Object)
     */
    public static byte[] serialize(Object obj) {
        try {
            return om.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#readTree(byte[])
     */
    public static JsonNode deserialize(byte[] source) {
        try {
            return om.readTree(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#readValue(String, Class)
     */
    public static <T> T deserialize(byte[] source, Class<T> clz) {
        try {
            return om.readValue(source, clz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ObjectMapper#readValue(byte[], TypeReference)
     */
    public static <T> T deserialize(byte[] source, TypeReference<T> typeReference) {
        try {
            return om.readValue(source, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the {@link DateFormat} used by the underlying {@link #om}
     */
    public static DateFormat df() {
        return om.getDateFormat();
    }

    /**
     * Set the {@link DateFormat} used by the underlying {@link #om}
     */
    public static void df(DateFormat df) {
        om.setDateFormat(df);
    }

    public static void with(ObjectMapper om) {
        Jason.om = om;
    }

    public static ObjectMapper om() {
        return om;
    }

    /**
     * Get a {@link Thingamabob} the json object builder
     */
    public static Thingamabob map() {
        return new Thingamabob();
    }

    @SuppressWarnings("unchecked")
    private static class Thingama<T extends Thingama<T>> extends HashMap<String, Object> {

        /**
         * Put an object
         *
         * @see HashMap#put(Object, Object)
         */
        public T val(String key, Object value) {
            this.check(key);
            put(key, value);
            return (T) this;
        }

        /**
         * Put an array of objects
         */
        public T val(String key, Object... values) {
            this.check(key);
            put(key, values);
            return (T) this;
        }

        /**
         * Put the key/value only if the value is not null
         *
         * @see #val(String, Object)
         */
        public T optional(String key, Object value) {
            this.check(key);
            if (value == null) return (T) this;
            return val(key, value);
        }

        @Override
        public String toString() {
            return stringify(this);
        }

        private void check(String key) {
            if (Strine.isEmpty(key))
                throw new IllegalArgumentException("Key empty");
        }
    }

    /**
     * Thingamabob is a json object builder based on map
     *
     * @author sinlo
     */
    public static class Thingamabob extends Thingama<Thingamabob> {

        /**
         * Convert a given {@link HashMap} to a {@link Thingamabob}
         */
        public static Thingamabob from(HashMap<String, Object> map) {
            Thingamabob thingamabob = new Thingamabob();
            thingamabob.putAll(map);
            return thingamabob;
        }

        /**
         * Map the key with a new {@link Thingamajig}
         */
        public Thingamajig<Thingamabob> map(String key) {
            Thingamajig<Thingamabob> next = new Thingamajig<>(this);
            put(key, next);
            return next;
        }

    }

    /**
     * Thingamajig is a json sub-object builder based on map
     *
     * @author sinlo
     */
    public static class Thingamajig<T extends Thingama<T>> extends Thingama<Thingamajig<T>> {

        @JsonIgnore
        private final T thingama;

        private Thingamajig(T thingama) {
            this.thingama = thingama;
        }

        /**
         * Map the key with a new {@link Thingamajig}
         */
        public Thingamajig<Thingamajig<T>> map(String key) {
            Thingamajig<Thingamajig<T>> next = new Thingamajig<>(this);
            put(key, next);
            return next;
        }

        /**
         * Go back to the previous {@link Thingamabob} or {@link Thingamajig}
         */
        public T end() {
            return this.thingama;
        }

    }

}