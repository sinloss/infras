package com.sinlo.security.jwt;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.spec.CompileAware;

import javax.tools.FileObject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
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
@Sponte(compiling = MakeKey.Maker.class, inheritable = false)
@CompileAware.Neglect
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
            MakeKey mk = cs.current.getAnnotation(MakeKey.class);
            // the resources folder
            Path res = cs.ctx.root().resolve(mk.value());
            if (Files.notExists(res))
                cs.error("Could not find the specified resources folder [ "
                        .concat(mk.value()).concat(" ]"));
            // the home jwt keys
            String pkg = Jwter.class.getPackage().getName();
            res = res.resolve(pkg.replace('.', '/'));
            if (Files.notExists(res)) {
                try {
                    Files.createDirectories(res);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            Path pri = res.resolve("key");
            Path pub = res.resolve("key.pub");
            // do not generate when any one of the keys exists
            if (Files.notExists(pri) && Files.notExists(pub)) {
                // initialize
                this.kpg.initialize(mk.size());
                // generate
                final KeyPair kp = kpg.genKeyPair();
                // output private
                gen(cs.ctx.res(pkg, "key"), pri, kp.getPrivate());
                // output public
                gen(cs.ctx.res(pkg, "key.pub"), pub, kp.getPublic());
            }

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
