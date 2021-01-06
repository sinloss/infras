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
            e.printStackTrace();
        }
        return null;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see ObjectMapper#readValue(String, TypeReference)
     */
    public static <T> T parse(String json, TypeReference<T> typeReference) {
        try {
            return om.readValue(json, typeReference);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see ObjectMapper#writeValueAsBytes(Object)
     */
    public static byte[] serialize(Object obj) {
        try {
            return om.writeValueAsBytes(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
            if (source == null) return null;
            return om.readValue(source, clz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @see ObjectMapper#readValue(byte[], TypeReference)
     */
    public static <T> T deserialize(byte[] source, TypeReference<T> typeReference) {
        try {
            return om.readValue(source, typeReference);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    /**
     * Thingamabob is a json object builder based on map
     *
     * @author sinlo
     */
    public static class Thingamabob extends HashMap<String, Object> {

        /**
         * @see HashMap#put(Object, Object)
         */
        public Thingamabob val(String key, Object value) {
            put(key, value);
            return this;
        }

        /**
         * Map the key with a new {@link Thingamajig}
         */
        public Thingamajig<Thingamabob> map(String key) {
            Thingamajig<Thingamabob> next = new Thingamajig<>(this);
            put(key, next);
            return next;
        }

        @Override
        public String toString() {
            return stringify(this);
        }
    }

    /**
     * Thingamajig is a json sub-object builder based on map
     *
     * @author sinlo
     */
    public static class Thingamajig<T> extends HashMap<String, Object> {

        @JsonIgnore
        private final T thingama;

        private Thingamajig(T thingama) {
            this.thingama = thingama;
        }

        /**
         * @see HashMap#put(Object, Object)
         */
        public Thingamajig<T> val(String key, Object value) {
            put(key, value);
            return this;
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

        @Override
        public String toString() {
            return stringify(this);
        }
    }

}