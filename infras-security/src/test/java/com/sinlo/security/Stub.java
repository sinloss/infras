package com.sinlo.security;

import com.sinlo.security.jwt.MakeKey;
import com.sinlo.security.jwt.spec.Keys;

@MakeKey("../src/test/resources")
public class Stub {

    @MakeKey(value = "../src/test/resources",
            keys = @Keys(pri = "kiki", pub = "kiki.public"))
    private Object stub;
}
