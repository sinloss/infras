package com.sinlo.core.prototype

import spock.lang.Specification

class PrototypeTest extends Specification {

    def "should prototype compare all properties as intended"() {
        given:
        def proto = Prototype.of(SampleBean)

        expect:
        def details = proto.compare(new SampleBean(id: 2, monster: "wood", name: "first"),
                new SampleBean(id: 1, monster: "wood", name: "second"))
        details.size() == 2
    }
}
