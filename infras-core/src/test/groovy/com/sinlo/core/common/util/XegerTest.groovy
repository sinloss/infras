package com.sinlo.core.common.util

import spock.lang.Specification

class XegerTest extends Specification {

    def "should create merged regex"() {
        given:
        def parts = ["/user/login/by-password",
                     "/user/co.*",
                     "/user/login/by-special-code",
                     ".*save",
                     "/business/charge",
                     "/business/refund"]

        expect:
        Xeger.zip("/", *parts).toString() ==
                "^(?:/(?:user/(?:login/(?:by-password|by-special-code)|co.*)|business/(?:charge|refund))|.*save)\$"
    }
}
