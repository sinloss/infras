package com.sinlo.core.common.util;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinlo.core.common.wraparound.Cascader;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Jason the json util
 * <br/><strike>Jason boarded Argo, went for the Golden fleece ...</strike>
 * <br/><strike>yet finally ended up just parsing json objects for days</strike>
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
     * Get a {@link Thingama.Bob} the json object builder
     */
    public static Thingama.Bob map() {
        return new Thingama.Bob();
    }

    /**
     * The abstraction of {@link Thingama.Bob} and {@link Thingama.Jig}
     *
     * @param <T> the type of the subtype of {@link Thingama}, always be the type of {@code this}
     */
    public static abstract class Thingama<T extends Thingama<T>> extends HashMap<String, Object> {

        /**
         * Put an object
         *
         * @see HashMap#put(Object, Object)
         */
        @SuppressWarnings("unchecked")
        public T val(String key, Object value) {
            this.check(key);
            put(key, value);
            return (T) this;
        }

        /**
         * Put an array of objects
         */
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        public T optional(String key, Object value) {
            this.check(key);
            if (value == null) return (T) this;
            return val(key, value);
        }

        /**
         * Merge the value with the existing value. That is when there is an already existing
         * value then create a {@link ArrayList} to hold both the values, and associate the
         * list with the key, and if the existing value is an instance of {@link List}, then
         * add the given value to it.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public T merge(String key, Object value) {
            this.check(key);
            compute(key, (k, v) -> {
                if (v == null) return value;
                return (v instanceof List
                        ? Cascader.of((List) v)
                        : Cascader.of(ArrayList::new).apply(List::add, v))
                        .apply(List::add, value).get();
            });
            return (T) this;
        }

        /**
         * Prepare to plant the given {@code val}
         *
         * @see Val#into(String...)
         */
        public Val plant(Object val) {
            return new Val(val);
        }

        /**
         * Must provide sub-map creation method
         */
        public abstract Thingama<?> map(String key);

        @Override
        public String toString() {
            return stringify(this);
        }

        private void check(String key) {
            if (Strine.isEmpty(key))
                throw new IllegalArgumentException("Key empty");
        }

        /**
         * Thingamabob is a json object builder based on map
         *
         * @author sinlo
         */
        public static class Bob extends Thingama<Bob> {

            /**
             * Convert a given {@link HashMap} to a {@link Bob}
             */
            public static Bob from(HashMap<String, Object> map) {
                Bob bob = new Bob();
                bob.putAll(map);
                return bob;
            }

            /**
             * Map the key with a new {@link Jig}
             */
            public Jig<Bob> map(String key) {
                Jig<Bob> next = new Jig<>(this);
                put(key, next);
                return next;
            }

        }

        /**
         * Thingamajig is a json sub-object builder based on map
         *
         * @author sinlo
         */
        public static class Jig<T extends Thingama<T>> extends Thingama<Jig<T>> {

            @JsonIgnore
            private final T thingama;

            private Jig(T thingama) {
                this.thingama = thingama;
            }

            /**
             * Map the key with a new {@link Jig}
             */
            public Jig<Jig<T>> map(String key) {
                Jig<Jig<T>> next = new Jig<>(this);
                put(key, next);
                return next;
            }

            /**
             * Go back to the previous {@link Bob} or {@link Jig}
             */
            public T end() {
                return this.thingama;
            }

        }

        /**
         * The {@link #val} carrier
         */
        public class Val {

            private final Object val;

            private Val(Object val) {
                this.val = val;
            }

            /**
             * Put the underlying val along the given keys deep into the bottom
             * and associate it with the last one
             */
            @SuppressWarnings("unchecked")
            public T into(String... keys) {
                switch (Objects.requireNonNull(keys).length) {
                    case 0:
                        break;
                    case 1:
                        return Thingama.this.val(keys[0], val);
                    default:
                        // the last index
                        int last = keys.length - 1;
                        // the initial thingama
                        Thingama<?> thingma = Thingama.this;
                        for (int i = 0; i < last; i++) {
                            // create a map each time on the way deep into the bottom
                            thingma = thingma.map(keys[i]);
                        }
                        // associate the val with the last key
                        thingma.val(keys[last], val);
                }
                return (T) Thingama.this;
            }
        }
    }

}