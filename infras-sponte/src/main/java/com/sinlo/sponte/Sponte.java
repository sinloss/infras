package com.sinlo.sponte;

import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.core.Spontaneously;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.Profile;
import com.sinlo.sponte.spec.SponteAware;
import com.sinlo.sponte.util.SponteFiler;
import com.sinlo.sponte.util.Typer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The main annotation who introduces the processing from {@link Spontaneously}
 *
 * @author sinlo
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Sponte {

    String NAME = "com.sinlo.sponte.Sponte";

    /**
     * The {@link SponteAware}
     */
    Class<? extends SponteAware> value() default SponteAware.class;

    /**
     * The key of instances of the {@link #value()} and the {@link #compiling()}. When only
     * one key is supplied, they would share the key, otherwise the {@link #value()} would
     * use the first key and the {@link #compiling()} the second.
     * <p/>
     * Different element annotated with the same {@code key} will share the same instance
     * of the {@link #value()} and the {@link #compiling()} respectively
     */
    String[] key() default Keys.DEFAULT;

    /**
     * Call the {@link CompileAware#onCompile(Context.Subject)} method when compiling. Note
     * that the given {@link CompileAware compiling} itself should be already compiled before
     * the compiling of which on it the {@link Sponte} is annotated
     */
    Class<? extends CompileAware> compiling() default CompileAware.class;

    /**
     * On {@link ElementType#ANNOTATION_TYPE} it represents if the annotation annotated by the
     * sponted annotation (which is annotated by this) could inherit the properties from the
     * sponted annotation. On {@link ElementType#TYPE} it represents if the {@link Sponte}
     * annotation should work on subclasses of the annotated class
     */
    boolean inheritable() default true;

    Agent agent() default @Agent(Agent.Bond.class);

    class Keys {

        public static final String DEFAULT = "";

        public static String get(Sponte sponte, Class<?> which) {
            String[] keys = sponte.key();
            switch (keys.length) {
                case 0:
                    return DEFAULT;
                case 1:
                    return keys[0];
                default:
                    return keys[SponteAware.class.isAssignableFrom(which) ? 0 : 1];
            }
        }
    }

    /**
     * The file objects
     */
    enum Fo {
        INITIALIZED(".initialized"),
        SPONTED(".sponted"),
        INHERITANCE(".inheritance");

        public final String name;

        Fo(String name) {
            this.name = name;
        }

        private static final String rootspec = "META-INF/spontaneously";
        private static final Path root = SponteFiler.ensure(rootspec);

        private PrintWriter pw;

        /**
         * Clear the {@link #root}
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void clear() throws IOException {
            Files.walk(root).map(Path::toFile).forEach(File::delete);
            SponteFiler.ensure(rootspec);
        }

        /**
         * Get the path of the given filename in {@link #root}
         */
        public static Path of(String name) {
            return root.resolve(name);
        }

        /**
         * Same as {@link #lines(String, Function)}, but accept a {@link Class} and use its name
         * as the {@code name}
         */
        public static <T> Set<T> lines(Class<?> c, Function<String, T> mapper) {
            return lines(c.getName(), mapper);
        }

        /**
         * Get a list of all the items mapped from all lines of the resources of the
         * given filename which resides in the {@link #rootspec}
         *
         * @see SponteFiler#lines(String, Supplier, Function)
         */
        public static <T> Set<T> lines(String name, Function<String, T> mapper) {
            return SponteFiler.asSet(rootspec.concat("/").concat(name), mapper);
        }

        /**
         * @see SponteFiler#names(String)
         */
        public static Set<String> names(String name) {
            return lines(name, line -> line);
        }

        /**
         * Get corresponding {@link Profile profiles} of the given subject
         */
        public static Set<Profile> profiles(Class<? extends Annotation> subject) {
            return Sponte.Fo.lines(subject,
                    line -> Profile.of(line, subject, Sponte.Fo.inheritors(subject)));
        }

        /**
         * Get all inheritors of the given {@link Class}
         */
        @SuppressWarnings("unchecked")
        public static Class<? extends Annotation>[] inheritors(Class<?> c) {
            return lines(
                    c.getCanonicalName().concat(Sponte.Fo.INHERITANCE.name),
                    line -> {
                        try {
                            return Typer.forName(line);
                        } catch (ClassNotFoundException e) {
                            return null;
                        }
                    }).stream()
                    .filter(Objects::nonNull)
                    .toArray(Class[]::new);
        }

        /**
         * Close all file objects
         */
        public static void closeAll() {
            for (Fo fo : values()) {
                fo.close();
            }
        }

        /**
         * @see Files#deleteIfExists(Path)
         */
        public static void delete(String name) throws IOException {
            Files.deleteIfExists(of(name));
        }

        /**
         * @see Files#createFile(Path, FileAttribute[])
         */
        public static void create(String name) throws IOException {
            Files.createFile(of(name));
        }

        /**
         * @see Files#exists(Path, LinkOption...)
         */
        public static boolean exists(String name) {
            return Files.exists(of(name));
        }

        /**
         * @see #exists(String)
         */
        public boolean exists() {
            return exists(name);
        }

        /**
         * @see #create(String)
         */
        public void create() throws IOException {
            create(name);
        }

        /**
         * @see #delete(String)
         */
        public void delete() throws IOException {
            delete(name);
        }

        /**
         * @see #names(String)
         */
        public Set<String> names() {
            return names(name);
        }

        /**
         * Println the given text to the underlying file object
         *
         * @see PrintWriter#println(String)
         */
        public void println(String text) {
            if (pw == null) pw = SponteFiler.writer(root.resolve(name), true);
            pw.println(text);
            pw.flush();
        }

        /**
         * Close the underlying {@link PrintWriter} and make it null
         */
        public void close() {
            if (pw != null) {
                pw.close();
                pw = null;
            }
        }
    }
}
