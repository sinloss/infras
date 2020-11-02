package com.sinlo.security.tkn

class Subject {

    String id

    String name

    @Override
    String toString() {
        return id + ":" + name
    }

    static Subject from(String expr) {
        def parts = expr.split(":")
        return new Subject(id: parts[0], name: parts[1])
    }
}
