package com.sinlo.core.common.util

import spock.lang.Specification

import java.util.stream.Collectors

class StrineTest extends Specification {

    def "should splitter properly convert cases"() {
        expect:
        closure(Strine.split(raw)).collect(Collectors.joining())

        where:
        closure                           || raw                   || expected
        { s -> s.byCamel().cobol() }      || "HelloWorld"          || "HELLO-WORLD"
        { s -> s.byKebab().pascal() }     || "hello-world"         || "HelloWorld"
        { s -> s.byKebab().camel() }      || "hello-world"         || "helloWorld"
        { s -> s.by("@@").delimit("--") } || "hello@@world@@again" || "hello--world--again"
    }
}
