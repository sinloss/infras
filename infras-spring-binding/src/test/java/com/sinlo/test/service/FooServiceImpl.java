package com.sinlo.test.service;

import com.sinlo.core.service.Proxistor;
import com.sinlo.spring.service.SpringProxistor;
import com.sinlo.test.domain.BarEntity;
import com.sinlo.test.domain.common.BasicEntity;
import com.sinlo.test.infrastructure.FooRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//@SpringProxistor(BasicEntity.class)
@Service
@Proxistor.Default
public class FooServiceImpl implements FooService {

    @Autowired
    private FooRepo fooRepo;

    //@Proxistor.Ignore
    @Override
    public BarEntity get() {
        return fooRepo.get();
    }

    @Override
    @SpringProxistor(BasicEntity.class)
    public void foo() {
        BarEntity bar = BarEntity.freshNew();
        bar.foo();
    }

    @Override
    @SpringProxistor(BasicEntity.class)
    public void bar() {
        BarEntity bar = fooRepo.get();
        bar.bar();
    }

    //@Proxistor.Ignore
    public void feintBar() {
        fooRepo.get().bar();
    }

    public void testt(Object a, String b, int c) {

    }
}
