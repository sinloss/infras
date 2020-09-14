package com.sinlo.core.service;

public class Err {

    public final int status;
    public final String message;

    public Err(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public final Ret<?, Err> ret() {
        return Ret.err(this);
    }

    public static Ret<?, Err> ret(int status, String message) {
        return Ret.err(new Err(status, message));
    }
}