package com.sinlo.sponte.util;

import com.sinlo.sponte.Sponte;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Sponte files management
 *
 * @author sinlo
 */
public class SponteFiler {

    private SponteFiler() {
    }

    /**
     * Ensure the directory of the given name
     */
    public static Path ensure(String dir) {
        if (!VISITABLE) return null;
        try {
            Path f = Paths.get(Objects.requireNonNull(Sponte.class.getResource("/")).toURI())
                    .resolve(dir);
            if (!Files.exists(f)) {
                Files.createDirectories(f);
            }
            return f;
        } catch (FileSystemNotFoundException fse) {
            return null;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean visitable() {
        try {
            Paths.get(Objects.requireNonNull(Sponte.class.getResource("/")).toURI());
        } catch (FileSystemNotFoundException fse) {
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * If there's an available file system for the files inside the project
     */
    public static final boolean VISITABLE = visitable();

    /**
     * Throw the {@link NotVisitable} exception if not {@link #VISITABLE}
     */
    public static void mustVisitable() {
        if (!VISITABLE)
            throw new NotVisitable();
    }

    /**
     * Create an ephemeral file that disappears the next time this function is applied onto
     * it
     */
    public static boolean ephemeral(Path f) {
        mustVisitable();
        try {
            if (!Files.deleteIfExists(f)) {
                Files.createFile(f);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Get all qualified names defined in all files around the classpath of the given name
     */
    public static Set<String> names(String fn) {
        return asSet(fn, line -> line);
    }

    /**
     * Get a list of all the items mapped from all lines of the resources of the
     * given filename
     */
    public static <C extends Collection<T>, T> C lines(String fn, Supplier<C> supplier,
                                                       Function<String, T> mapper) {
        final C collection = supplier.get();
        try {
            final Enumeration<URL> resources =
                    Sponte.class.getClassLoader().getResources(fn);
            while (resources.hasMoreElements()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        resources.nextElement().openStream()))) {

                    reader.lines().map(mapper)
                            .forEach(collection::add);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return collection;
    }

    /**
     * Map every line of the resources who have the given name
     */
    public static <T> Set<T> asSet(String name, Function<String, T> mapper) {
        return lines(name, HashSet::new, mapper);
    }

    /**
     * Get the corresponding {@link PrintWriter}
     */
    public static PrintWriter writer(Path path, boolean append) {
        mustVisitable();
        try {
            return new PrintWriter(new FileOutputStream(path.toFile(), append));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The current file system is not visitable
     */
    public static class NotVisitable extends RuntimeException {
        public NotVisitable() {
            super("There's none visitable file system right now, " +
                    "please try this operation in a normal file system " +
                    "instead of a jar or what not");
        }
    }
}
