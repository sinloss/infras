package com.sinlo.test.service;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.test.JamesBond;
import com.sinlo.test.domain.BarEntity;

@Sponte(agent = @Agent(JamesBond.class))
public interface FooService {
    BarEntity get();

    void foo();

    void bar();

    void feintBar();
}
