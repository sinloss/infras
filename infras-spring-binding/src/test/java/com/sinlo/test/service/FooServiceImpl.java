package com.sinlo.test.service;

import com.sinlo.core.service.spec.ProxerviceIgnore;
import com.sinlo.spring.service.spec.Proxerviced;
import com.sinlo.test.domain.BarEntity;
import com.sinlo.test.domain.common.BasicEntity;
import com.sinlo.test.infrastructure.FooRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Proxerviced(BasicEntity.class)
@Service
public class FooServiceImpl implements FooService {

    @Autowired
    private FooRepo fooRepo;

    @ProxerviceIgnore
    @Override
    public BarEntity get() {
        return fooRepo.get();
    }

    @Override
    public void foo() {
        BarEntity bar = BarEntity.freshNew();
        bar.foo();
    }

    @Override
    public void bar() {
        BarEntity bar = fooRepo.get();
        bar.bar();
    }

    @ProxerviceIgnore
    @Override
    public void feintBar() {
        fooRepo.get().bar();
    }
}
