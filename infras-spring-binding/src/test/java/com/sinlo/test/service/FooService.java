package com.sinlo.test.service;

import com.sinlo.test.domain.BarEntity;

public interface FooService {
    BarEntity get();

    void foo();

    void bar();

    void feintBar();
}
