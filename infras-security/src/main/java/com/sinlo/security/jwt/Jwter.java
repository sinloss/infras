package com.sinlo.security.jwt;

import com.sinlo.core.common.functional.ImpatientFunction;
import com.sinlo.core.common.util.Arria;
import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Try;
import com.sinlo.security.jwt.spec.exception.JwtException;
import com.sinlo.security.jwt.spec.Jwt;
import com.sinlo.security.jwt.spec.exception.ValidationFailedException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import java.util.*;
import java.util.function.Function;

/**
 * Jwter the jwt encoder and decoder
 * <p/>
 * Only support base64 encoded RSA key that is at least 2048-bit long, the key could be
 * generated using the following command lines or, better, using the {@link MakeKey}
 * annotation which can generate valid key files in the {@code resources} folder of the
 * working tree if there are neither {@code key} file nor {@code key.pub} file exists. And
 * also create copies of those generated key files in their corresponding build paths
 * <p>
 * <p/>
 * <pre>
 * # Generate a 2048-bit RSA private key
 * <b>openssl</b> genrsa <i>-out</i> <u>pk.pem</u> 2048
 *
 * # Convert to PKCS#8 format
 * <b>openssl</b> pkcs8 <i>-topk8</i> <i>-inform</i> PEM <i>-outform</i> DER <i>-in</i>
 *      <u>pk.pem</u> <i>-out</i> <u>key.raw</u> <i>-nocrypt</i>
 *
 * # Output public key
 * <b>openssl</b> rsa <i>-in</i> <u>pk.pem</u> <i>-pubout</i> <i>-outform</i> DER <i>-out</i>
 *      <u>key.pub.raw</u>
 *
 * # Output base64 encoded file
 * <b>openssl</b> base64 <i>-in</i> <u>key.raw</u> <i>-out</i> <u>key</u>
 * <b>openssl</b> base64 <i>-in</i> <u>key.pub.raw</u> <i>-out</i> <u>key.pub</u>
 * </pre>
 * <p/>
 *
 * @author sinlo
 */
public class Jwter<J> {

    public static final String DEFAULT_PRI = "key";
    public static final String DEFAULT_PUB = "key.pub";

    private final Scheme<J> scheme;

    private final Jwt.Signer<J> enc;
    private final Jwt.Dec dec;
    private final List<ImpatientFunction<Jwt, Boolean, JwtException>> validators = new LinkedList<>();

    public Jwter(Scheme<J> scheme) {
        this(scheme, DEFAULT_PRI, DEFAULT_PUB);
    }

    public Jwter(Scheme<J> scheme, String pri, String pub) {
        this.scheme = scheme;
        this.enc = enc(pri);
        this.dec = dec(pub);
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
        Jwt j = dec.decode(jwt);
        Set<Throwable> errs = j.validate(this.validators);
        if (!Arria.isEmpty(errs))
            throw new ValidationFailedException(errs);
        return j;
    }

    public J encode(J jwt) {
        if (enc == null) throw new UnsupportedOperationException("Encode is not supported");
        try {
            return enc.sign(jwt);
        } catch (JwtException e) {
            e.printStackTrace();
        }
        return jwt;
    }

    /**
     * Add validators
     *
     * @see Jwt#validate(List)
     */
    @SafeVarargs
    public final Jwter<J> ensure(ImpatientFunction<Jwt, Boolean, JwtException>... validators) {
        Collections.addAll(this.validators, validators);
        return this;
    }

    /**
     * Add validators
     *
     * @see Jwt#validate(List)
     */
    public final Jwter<J> ensure(List<ImpatientFunction<Jwt, Boolean, JwtException>> validators) {
        this.validators.addAll(validators);
        return this;
    }

    /**
     * Get a JwtDecoder
     *
     * @param pub the public key file path
     */
    public Jwt.Dec dec(String pub) {
        if (scheme.pub()) {
            RSAPublicKey key = load(pub, Jwter::pub);
            if (key != null) {
                return scheme.dec(key);
            }
            return null;
        }
        return scheme.dec(null);
    }

    /**
     * Get a RSASSASigner
     *
     * @param pri the private key file path
     */
    public Jwt.Signer<J> enc(String pri) {
        if (scheme.pri()) {
            PrivateKey key = load(pri, Jwter::pri);
            if (key != null) {
                return scheme.signer(key);
            }
            return null;
        }
        return scheme.signer(null);
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

    /**
     * Expecting only one single resource from the given resource name
     */
    public static Optional<URL> singleResource(String name) {
        try {
            Enumeration<URL> resources = Jwter.class.getClassLoader()
                    .getResources(Jwter.class.getPackage().getName()
                            .replace('.', '/') + '/' + name);
            URL res = resources.hasMoreElements() ? resources.nextElement() : null;
            if (res != null && resources.hasMoreElements())
                throw new TooManyKeyFilesException(name);
            return Optional.ofNullable(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Load a file or a resource
     */
    public static <K extends Key> K load(String name, Function<byte[], K> loader) {
        try (InputStream is = Files.exists(Paths.get(name))
                ? new FileInputStream(name)
                : singleResource(name).map(url -> Try.tolerate(url::openStream)).orElse(null);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // the 'is' might be null
            if (is == null) return null;

            byte[] buf = new byte[4096]; // 4K
            int len;
            while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
            return loader.apply(Base64.getDecoder().decode(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * The scheme of a specific jwt typed token implementation
     *
     * @param <J> the jwt token type of the implementation
     */
    public interface Scheme<J> {

        /**
         * Indicating if the loading of the public key is required or not
         */
        default boolean pub() {
            return true;
        }

        /**
         * Indicating if the loading of the private key is required or not
         */
        default boolean pri() {
            return true;
        }

        /**
         * To create a {@link Jwt.Dec}
         *
         * @param key a {@link RSAPublicKey} which will be null in case of
         *            not {@link #pub()}
         */
        Jwt.Dec dec(RSAPublicKey key);

        /**
         * To create a {@link Jwt.Signer}
         *
         * @param key a {@link PrivateKey} which will be null in case of
         *            not {@link #pri()}
         */
        Jwt.Signer<J> signer(PrivateKey key);

        /**
         * Issue a {@link J} typed jwt token
         *
         * @param iss {@link Jwt.Claim#ISS}
         * @param sub {@link Jwt.Claim#SUB}
         * @param jti {@link Jwt.Claim#JTI}
         * @param iat {@link Jwt.Claim#IAT}
         * @param nbf {@link Jwt.Claim#NBF}
         * @param exp {@link Jwt.Claim#EXP}
         * @param aud {@link Jwt.Claim#AUD}
         */
        J issue(String iss, String sub, String jti, Date iat, Date nbf, Date exp, List<String> aud);
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
        @SuppressWarnings("SpellCheckingInspection")
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
        public J issue(String jti, T sub, long lifespan) {
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
        @SuppressWarnings("SpellCheckingInspection")
        public J issue(String jti, T sub, long lifespan, List<String> audiences) {
            return Jwter.this.encode(scheme.issue(
                    iss,
                    converter.apply(sub),
                    jti,
                    new Date(),
                    new Date(System.currentTimeMillis() - leeway),
                    new Date(System.currentTimeMillis() + lifespan),
                    Funny.nvl(audiences, Collections.emptyList())));
        }

    }

    public static class TooManyKeyFilesException extends RuntimeException {

        public TooManyKeyFilesException(String name) {
            super(String.format(
                    "There are too many key files named [ %s ] in %s",
                    name, Jwter.class.getPackage().getName()));
        }
    }

}
