package com.sinlo.datadict.repo;

import com.sinlo.datadict.spec.Kind;

public interface DictStore {

    boolean watchable();

    long version();

    Kind get(String kind);

    void set(Kind.Value<?> value);
}
