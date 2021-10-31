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
import com.sinlo.security.jwt.nimbus.spec.ProcessorBuilder;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.SigningFailedException;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

/**
 * The {@link Jwter.Scheme} of nimbus
 *
 * @author sinlo
 */
public interface NimbusScheme extends Jwter.Scheme<SignedJWT> {

    /**
     * An instance of the simplest {@link NimbusScheme} which uses {@link JWSAlgorithm#RS256}
     * as its {@link #alg()}
     */
    NimbusScheme Simple = of(JWSAlgorithm.RS256);

    /**
     * Instantiate a simple {@link NimbusScheme} using the given {@link JWSAlgorithm}
     *
     * @param alg the {@link JWSAlgorithm} to be returned by {@link #alg()}
     */
    static NimbusScheme of(JWSAlgorithm alg) {
        return () -> alg;
    }

    static NimbusScheme dec(ProcessorBuilder pb) {
        return new JustDec(NimbusDec.of(pb));
    }

    /**
     * Provide the basic {@link JWSAlgorithm} for both {@link #issue(String, String, String, Date, Date, Date, List)}
     * and {@link #dec(RSAPublicKey)}
     */
    JWSAlgorithm alg();

    @Override
    default Jwt.Dec dec(RSAPublicKey key) {
        return NimbusDec.of(key, alg());
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
                new JWSHeader.Builder(alg())
                        .keyID(NimbusScheme.class.toString()).build(),
                builder.build());
    }

    @Override
    default String serialize(SignedJWT jwt) {
        return jwt.serialize();
    }

    /**
     * An implementation of decode-only {@link NimbusScheme}
     */
    class JustDec implements NimbusScheme {

        private final NimbusDec dec;

        private JustDec(NimbusDec dec) {
            this.dec = dec;
        }

        @Override
        public Jwt.Dec dec(RSAPublicKey key) {
            return dec;
        }

        @Override
        public boolean pub() {
            return false;
        }

        @Override
        public boolean pri() {
            return false;
        }

        @Override
        public JWSAlgorithm alg() {
            return null;
        }
    }
}
