package com.sinlo.datadict.spec;

import java.util.Map;
import java.util.function.Function;

public class Kind {

    public final String kind;

    private final Map<String, String> kvs;

    public Kind(String kind, Map<String, String> kvs) {
        this.kind = kind;
        this.kvs = kvs;
    }

    public class Value<T> {

        public final Kind kind;

        public final String key;

        public final T value;

        @SuppressWarnings("unchecked")
        public Value(String key, Function<String, T> converter) {
            this.kind = Kind.this;
            this.key = key;
            if (converter == null)
                converter = v -> (T) v;
            this.value = converter.apply(Kind.this.kvs.get(key));
        }

        @SuppressWarnings("unchecked")
        public Value(String key) {
            this(key, v -> (T) v);
        }

    }
}
