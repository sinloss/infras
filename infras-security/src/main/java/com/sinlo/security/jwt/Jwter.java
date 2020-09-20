package com.sinlo.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Jwter the jwt encoder and decoder
 * <p/>
 * Only support PKCS#8 formatted 2048-bit RSA key
 * <p/>
 * <pre>
 * # Generate a 2048-bit RSA private key
 * <b>openssl</b> genrsa <i>-out</i> <u>pk.pem</u> 2048
 *
 * # Convert to PKCS#8 format
 * <b>openssl</b> pkcs8 <i>-topk8</i> <i>-inform</i> PEM <i>-outform</i> DER <i>-in</i> <u>pk.pem</u> <i>-out</i> <u>key</u> <i>-nocrypt</i>
 *
 * # Output public key
 * <b>openssl</b> rsa <i>-in</i> <u>pk.pem</u> <i>-pubout</i> <i>-outform</i> DER <i>-out</i> <u>key.pub</u>
 * </pre>
 * <p/>
 *
 * @author sinlo
 */
public class Jwter {

    private final JwtDecoder dec;
    private final RSASSASigner enc;

    public Jwter() {
        this("key.pub", "key");
    }

    public Jwter(String pub, String pri) {
        this.dec = dec(pub);
        this.enc = enc(pri);
    }

    /**
     * @see Jwter#issuer(String, Function, Integer)
     */
    public <T> Issuer<T> issuer(String iss, Function<T, String> converter) {
        return issuer(iss, converter, null);
    }

    /**
     * Get the issuer
     *
     * @param iss       the issuer field of claims
     * @param converter the subject converter
     * @param leeway    the leeway for the nbf field
     * @param <T>       the type of subject
     */
    public <T> Issuer<T> issuer(String iss, Function<T, String> converter, Integer leeway) {
        return leeway == null
                ? this.new Issuer<>(iss, converter)
                : this.new Issuer<>(iss, converter, leeway);
    }

    public Jwt decode(String jwt) {
        if (dec == null) throw new UnsupportedOperationException("Decode is not supported");
        return dec.decode(jwt);
    }

    public SignedJWT encode(SignedJWT jwt) {
        if (enc == null) throw new UnsupportedOperationException("Encode is not supported");
        try {
            jwt.sign(enc);
        } catch (JOSEException e) {
            e.printStackTrace();
        }
        return jwt;
    }

    /**
     * Set validators for the {@link NimbusJwtDecoder}
     */
    @SafeVarargs
    public final Jwter ensure(OAuth2TokenValidator<Jwt>... validators) {
        if (validators != null) {
            NimbusJwtDecoder nimbus = (NimbusJwtDecoder) dec;
            switch (validators.length) {
                case 0:
                    break;
                case 1:
                    nimbus.setJwtValidator(validators[0]);
                    break;
                default:
                    nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(Arrays.asList(validators)));
                    break;
            }
        }
        return this;
    }

    /**
     * Get a JwtDecoder
     *
     * @param pub the public key file path
     */
    public static JwtDecoder dec(String pub) {
        RSAPublicKey key = load(pub, Jwter::pub);
        if (key != null) {
            return NimbusJwtDecoder.withPublicKey(key).build();
        }
        return null;
    }

    /**
     * Get a RSASSASigner
     *
     * @param pri the private key file path
     */
    public static RSASSASigner enc(String pri) {
        PrivateKey key = load(pri, Jwter::pri);
        if (key != null) {
            return new RSASSASigner(key);
        }
        return null;
    }

    /**
     * Restore the private key from bytes
     */
    public static PrivateKey pri(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Restore the public key from bytes
     */
    public static RSAPublicKey pub(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(bytes));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <K extends Key> K load(String name, Function<byte[], K> loader) {
        try (InputStream is = Files.exists(Paths.get(name))
                ? new FileInputStream(name) : Jwter.class.getResourceAsStream(name);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // the 'is' might be null
            if (is == null) return null;

            byte[] buf = new byte[4089]; // 4K
            int len;
            while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
            return loader.apply(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * The issuer
     *
     * @param <T> the type of subject
     */
    public class Issuer<T> {

        private final String iss;
        private final Function<T, String> converter;
        private final int leeway;

        /**
         * @see Issuer#Issuer(String, Function, int)
         */
        private Issuer(String iss, Function<T, String> converter) {
            this(iss, converter, 30000);
        }

        /**
         * Instantiate an issuer
         *
         * @param iss       {@link com.nimbusds.jwt.JWTClaimsSet.Builder#issuer(String)}
         * @param converter the [ subject -> string ] converter, which could choose to encrypt a minimum of confidential
         *                  fields in the conversion process
         * @param leeway    the leeway for {@link com.nimbusds.jwt.JWTClaimsSet.Builder#notBeforeTime(Date) nbf}
         */
        private Issuer(String iss, Function<T, String> converter, int leeway) {
            this.iss = iss;
            this.converter = converter == null
                    ? (sub -> sub == null ? "" : sub.toString())
                    : converter;
            this.leeway = leeway;
        }

        /**
         * @see Issuer#issue(String, Object, long, List)
         */
        public SignedJWT issue(String jti, T sub, long lifespan) {
            return issue(jti, sub, lifespan, null);
        }

        /**
         * Issue a signed jwt
         *
         * @param jti       {@link com.nimbusds.jwt.JWTClaimsSet.Builder#jwtID(String)}
         * @param sub       {@link com.nimbusds.jwt.JWTClaimsSet.Builder#subject(String)}
         * @param lifespan  the lifespan of this token, in milliseconds
         * @param audiences {@link com.nimbusds.jwt.JWTClaimsSet.Builder#audience(List)}
         */
        public SignedJWT issue(String jti, T sub, long lifespan, List<String> audiences) {
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .issuer(iss)
                    .subject(converter.apply(sub))
                    .jwtID(jti)
                    .issueTime(new Date())
                    .notBeforeTime(new Date(System.currentTimeMillis() - leeway))
                    .expirationTime(new Date(System.currentTimeMillis() + lifespan));
            if (audiences != null && !audiences.isEmpty()) {
                builder.audience(audiences);
            }
            return Jwter.this.encode(new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(Jwter.this.toString()).build(),
                    builder.build()));
        }

    }

}
