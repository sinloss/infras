package com.sinlo.security.tkn.spec;

public interface Knowledge<T, A> {

    State<T, A> stat(T t);

    T create(Long lifespan, A subject);
}
