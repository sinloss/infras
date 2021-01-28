package com.sinlo.security.tkn

import spock.lang.Specification

@spock.lang.Subject(TkBuilder)
class TkBuilderTest extends Specification {

    def "should builder properly build and renew"() {
        given:
        TknKeeper<String, Subject> tk = TkBuilder.of(String, Subject)
                .ephemeral(7200000)
                .longevous(7200000 * 24 * 7)
                .jwt()
                .surefire()
                .des(Subject.&from)
                .ser({ s -> s.toString() })
                .ok().build() as TknKeeper<String, Subject>

        expect:
        def tkn = tk.create(new Subject(id: "3.1415926535897932384626433832795", name: "PI"))
        def renewed = tk.renew(tkn)
        tkn.longevous == renewed.longevous
        tkn.ephemeral != renewed.ephemeral
    }
}
