package com.sinlo.security.jwt.nimbus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.sinlo.security.jwt.nimbus.spec.ProcessorBuilder;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.BadJwtException;
import com.sinlo.security.jwt.spec.exception.DecodingFailedException;
import com.sinlo.security.jwt.spec.exception.JwtException;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.LinkedHashMap;

public class Decoder implements Jwt.Dec {

    private final JWTProcessor<SecurityContext> processor;

    private Decoder(JWTProcessor<SecurityContext> processor) {
        this.processor = processor;
    }

    public static Decoder of(ProcessorBuilder builder) {
        return new Decoder(builder.processor());
    }

    public static Decoder of(RSAPublicKey key, JWSAlgorithm alg) {
        return of(new ProcessorBuilder.SingleKey<>(key).alg(alg));
    }

    public static Decoder of(PrivateKey key, JWSAlgorithm alg) {
        return of(new ProcessorBuilder.SingleKey<>(key).alg(alg));
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        JWT jwt = parse(token);
        if (jwt instanceof PlainJWT) {
            throw BadJwtException.unsupported(jwt.getHeader().getAlgorithm());
        }
        try {
            return new Jwt(token,
                    new LinkedHashMap<>(jwt.getHeader().toJSONObject()),
                    this.processor.process(jwt, null).getClaims());
        } catch (RemoteKeySourceException e) {
            throw ifParse(e, "JwkSet");
        } catch (JOSEException e) {
            throw DecodingFailedException.other(e);
        } catch (Exception e) {
            throw ifParse(e, "Payload");
        }
    }

    private DecodingFailedException ifParse(Exception e, String then) {
        if (e.getCause() instanceof ParseException) {
            return DecodingFailedException.malformed(then, e);
        }
        return DecodingFailedException.other(e);
    }

    private JWT parse(String token) {
        try {
            return JWTParser.parse(token);
        } catch (Exception ex) {
            throw BadJwtException.whileParsing(ex);
        }
    }
}
