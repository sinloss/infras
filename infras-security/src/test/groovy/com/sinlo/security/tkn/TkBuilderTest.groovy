package com.sinlo.security.tkn

import com.sinlo.security.jwt.spec.Jwt
import spock.lang.Specification

class TkBuilderTest extends Specification {

    static tknKeeper() {
        TkBuilder.of(String, Client)
                .ephemeral(7200000)
                .longevous(7200000 * 24 * 7)
                .jwt()
                .surefire()
                .des(Client.&from)
                .ser({ s -> s.toString() })
                .ok().build() as TknKeeper<String, Jwt, Client>
    }

    def "should builder properly build and renew"() {
        given:
        TknKeeper<String, Jwt, Client> tk = tknKeeper()

        expect:
        def tkn = tk.create(new Client(id: "3.1415926535897932384626433832795", name: "PI"))
        def renewed = tk.renew(tkn)
        tkn.longevous == renewed.longevous
        tkn.ephemeral != renewed.ephemeral
    }
}
