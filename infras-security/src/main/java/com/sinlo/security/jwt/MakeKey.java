package com.sinlo.security.jwt;

import com.sinlo.security.jwt.spec.Keys;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.spec.CompileAware;

import javax.tools.FileObject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * The maker of rsa keys
 *
 * @author sinlo
 */
@Retention(RetentionPolicy.RUNTIME)
@Sponte(compiling = MakeKey.Maker.class, inheritable = false)
@Sponte.CompilingNeglect
public @interface MakeKey {

    /**
     * The resources folder's location relative to the folder where the {@code classes}
     * folder resides
     */
    String value() default "../src/main/resources";

    /**
     * The key size
     *
     * @see KeyPairGenerator#initialize(int)
     */
    int size() default 2048;

    Keys keys() default @Keys(pri = Jwter.DEFAULT_PRI, pub = Jwter.DEFAULT_PUB);

    class Maker implements CompileAware {

        private final KeyPairGenerator kpg;
        private final Base64.Encoder enc = Base64.getEncoder();

        public Maker() {
            try {
                this.kpg = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCompile(Context.Subject cs) {
            final MakeKey mk = cs.current.getAnnotation(MakeKey.class);
            // the resources folder
            Path res = cs.ctx.root().resolve(mk.value());
            if (Files.notExists(res))
                cs.error("Could not find the specified resources folder [ "
                        .concat(mk.value()).concat(" ]"));
            // the home of jwt keys
            final String pkg = Jwter.class.getPackage().getName();
            res = res.resolve(pkg.replace('.', '/'));
            if (Files.notExists(res)) {
                try {
                    Files.createDirectories(res);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            final Keys keys = mk.keys();
            final Path pri = ifNotExist(res, keys.pri());
            final Path pub = ifNotExist(res, keys.pub());
            // only generate when none of the keys exists
            if (pri != null && pub != null) {
                // initialize
                this.kpg.initialize(mk.size());
                // generate
                final KeyPair kp = kpg.genKeyPair();
                // output private
                gen(cs.ctx.res(pkg, keys.pri()), pri, kp.getPrivate());
                // output public
                gen(cs.ctx.res(pkg, keys.pub()), pub, kp.getPublic());
            }

        }

        private static Path ifNotExist(Path folder, String name) {
            try {
                // not present
                if (!Jwter.singleResource(name).isPresent()) {
                    Path res = folder.resolve(name);
                    // not exists
                    if (Files.notExists(res)) return res;
                }
            } catch (Jwter.TooManyKeyFilesException ignored) {
            }
            return null;
        }

        private void gen(FileObject fo, Path file, Key key) {
            String value = new String(enc.encode(key.getEncoded()), StandardCharsets.UTF_8);
            try {
                print(new FileOutputStream(file.toFile(), false), value);
                if (fo != null) print(fo.openOutputStream(), value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void print(OutputStream os, String value) {
            try (PrintWriter w = new PrintWriter(os)) {
                w.print(value);
                w.flush();
            }
        }
    }
}
