package com.sinlo.core.prototype

import spock.lang.Specification

class PrototypeTest extends Specification {

    def "should prototype extract all properties as intended"() {
        expect:
        def proto = Prototype.of(SampleBean)
        proto.property("aDragon") != null
        proto.property("aDragon").props().size() == 1
        proto.property("id").props().size() == 2
    }
}
