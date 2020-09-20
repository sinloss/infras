package com.sinlo.test.domain;

import com.sinlo.test.domain.common.BasicEntity;

public class BarEntity extends BasicEntity {

    private String foo;

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public void foo() {
        this.foo = "foo";
        this.create();
    }

    public void bar() {
        this.foo = "bar";
        this.update();
    }

    public static BarEntity freshNew() {
        return new BarEntity();
    }
}
