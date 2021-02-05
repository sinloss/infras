package com.sinlo.security.tkn

import com.sinlo.security.tkn.spec.Subject

class Client implements Subject {

    String id

    String name

    @Override
    String toString() {
        return id + ":" + name
    }

    static Client from(String expr) {
        def parts = expr.split(":")
        return new Client(id: parts[0], name: parts[1])
    }
}
