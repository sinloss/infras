package com.sinlo.security.verify

import com.sinlo.security.jwt.spec.Jwt
import com.sinlo.security.tkn.Client
import com.sinlo.security.tkn.TkBuilderTest
import com.sinlo.security.tkn.TknKeeper
import com.sinlo.security.verify.spec.VerificationFailure
import spock.lang.Specification

class VerifierTest extends Specification {

    @SuppressWarnings('GrUnresolvedAccess')
    def "should verifier properly verify"() {
        given:
        TknKeeper<String, Jwt, Client> tk = TkBuilderTest.tknKeeper()
        def verifier = Verifier.of(tk)
                .whenAny().match("/user/.*-stat", "/business/get-.*")
                .except("/user/p-stat")
                .and().when("POST")
                .match("/client/set-.*", "/client/.*-stat")
                .except("/client/private-stat")
                .then().pass()

        when:
        def failure
        try {
            verifier.verify(type, path, null)
        } catch (VerificationFailure e) {
            failure = e
        }

        then:
        (failure != null) == caught

        where:
        type   || path                      || caught
        "GET"  || "/user/what-stat-nothing" || true
        "GET"  || "/user/p-stat"            || true
        "GET"  || "/user/profile-stat"      || false
        "GET"  || "/business/get-prices"    || false
        "GET"  || "/business/set-prices"    || true
        "GET"  || "/client/private-stat"    || true
        "GET"  || "/client/set-name"        || true
        "GET"  || "/client/public-stat"     || true

        "POST" || "/user/what-stat-nothing" || true
        "POST" || "/user/p-stat"            || true
        "POST" || "/user/profile-stat"      || false
        "POST" || "/business/get-prices"    || false
        "POST" || "/business/set-prices"    || true
        "POST" || "/client/private-stat"    || true
        "POST" || "/client/set-name"        || false
        "POST" || "/client/public-stat"     || false
    }
}
