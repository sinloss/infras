package com.sinlo.core.common.util

import spock.lang.Specification

class XegerTest extends Specification {

    def "should create merged regex"() {
        given:
        def parts = ["/user/login/by-password", "/user/lo.*", "/user/login/by-special-code", "*.save", "/business/charge", "/business/refund"]

        expect:
        Xeger.zip("/", *parts) != null
    }
}
