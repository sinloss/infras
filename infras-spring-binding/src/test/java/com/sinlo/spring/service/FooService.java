package com.sinlo.spring.service;

import com.sinlo.spring.domain.BarEntity;

public interface FooService {
    BarEntity get();

    void foo();

    void bar();

    void feintBar();
}
