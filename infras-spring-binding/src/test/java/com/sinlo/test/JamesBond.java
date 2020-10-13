package com.sinlo.test;

import com.sinlo.sponte.spec.Agent;

import java.util.concurrent.Callable;

public class JamesBond implements Agent.Bond {

    @Override
    public <T> T act(Agent.Context context, Callable<T> mission) {
        return null;
    }
}
