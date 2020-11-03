package com.sinlo.spring.service;

import com.sinlo.core.service.Proxistor;
import com.sinlo.spring.BasicProxistor;
import com.sinlo.spring.infrastructure.FooRepo;
import com.sinlo.spring.domain.BarEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FooServiceImpl implements FooService {

    @Autowired
    private FooRepo fooRepo;

    @Proxistor.Ignore
    @Override
    public BarEntity get() {
        return fooRepo.get();
    }

    @Override
    @BasicProxistor
    public void foo() {
        BarEntity bar = BarEntity.freshNew();
        bar.foo();
    }

    @Override
    @BasicProxistor
    public void bar() {
        BarEntity bar = fooRepo.get();
        bar.bar();
    }

    @Proxistor.Ignore
    public void feintBar() {
        fooRepo.get().bar();
    }
}
