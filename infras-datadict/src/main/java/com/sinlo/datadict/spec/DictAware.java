package com.sinlo.datadict.spec;

public interface DictAware {

    <T> Kind.Value<T> dict(String fieldName, Class<T> vt);

    class Pool {

    }
}
