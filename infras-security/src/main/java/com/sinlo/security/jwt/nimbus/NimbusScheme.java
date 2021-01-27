package com.sinlo.security.jwt.nimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import com.sinlo.core.common.util.Arria;
import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Try;
import com.sinlo.security.jwt.Jwter;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.SigningFailedException;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

public interface NimbusScheme extends Jwter.Scheme<SignedJWT> {

    NimbusScheme Simple = new NimbusScheme() {
    };

    @Override
    default Jwt.Dec dec(RSAPublicKey key) {
        return Decoder.of(key, JWSAlgorithm.RS256);
    }

    @Override
    default Jwt.Signer<SignedJWT> signer(PrivateKey key) {
        final RSASSASigner signer = new RSASSASigner(key);
        return jwt -> Try.of(Funny.cascade(Funny.bind(SignedJWT::sign, jwt, signer), jwt))
                .caught(JOSEException.class)
                .thenThrow(SigningFailedException::new).exert();
    }

    @Override
    default SignedJWT issue(String iss, String sub, String jti, Date iat, Date nbf, Date exp, List<String> aud) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(iss).subject(sub)
                .jwtID(jti).issueTime(iat).notBeforeTime(nbf).expirationTime(exp);
        if (Arria.nonEmpty(aud)) {
            builder.audience(aud);
        }
        return new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(NimbusScheme.class.toString()).build(),
                builder.build());
    }

}
