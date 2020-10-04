package com.sinlo.sponte.util;

import com.sinlo.sponte.Sponte;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Sponte files management
 *
 * @author sinlo
 */
public class SponteFiler {

    private SponteFiler() {
    }

    private static Path root;

    /**
     * Get the path of {@link Sponte#SPONTE_ROOT sponte root}
     */
    public static Path root() throws URISyntaxException, IOException {
        if (root != null) return root;
        root = Paths.get(Sponte.class.getResource("/").toURI())
                .resolve(Sponte.SPONTE_ROOT);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        return root;
    }

    /**
     * Lock by creating a {@link Sponte#LOCK .lock} file, or unlock by deleting an
     * existing {@link Sponte#LOCK .lock} file
     */
    public static boolean lock() throws IOException, URISyntaxException {
        Path lock = root().resolve(Sponte.LOCK);
        if (!Files.deleteIfExists(lock)) {
            Files.createFile(lock);
            return true;
        }
        return false;
    }

    /**
     * Query if should lock or not
     */
    public static boolean locked() throws IOException, URISyntaxException {
        return Files.exists(root().resolve(Sponte.LOCK));
    }

    /**
     * Get the {@link PrintWriter writer} of {@link Sponte#SPONTED .sponted} file.
     */
    public static PrintWriter writerSponted() throws IOException, URISyntaxException {
        return writer(root().resolve(Sponte.SPONTED), true);
    }

    /**
     * Get all qualified names defined in all {@link Sponte#SPONTED sponted} files
     * around the classpath
     */
    public static Set<String> spontedNames() {
        return asSet(Sponte.SPONTED, line -> line);
    }

    /**
     * Get a stream of all the lines of the resources of the given name
     */
    public static Stream<String> lines(String name) {
        try {
            final Enumeration<URL> resources = Sponte.class.getClassLoader()
                    .getResources(Sponte.SPONTE_ROOT
                            .concat("/").concat(name));

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                    new Iterator<URL>() {
                        @Override
                        public boolean hasNext() {
                            return resources.hasMoreElements();
                        }

                        @Override
                        public URL next() {
                            return resources.nextElement();
                        }
                    }, Spliterator.ORDERED), false)
                    .flatMap(url -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(url.openStream()))) {
                            return reader.lines()
                                    .collect(Collectors.toList())
                                    .stream();
                        } catch (IOException ignored) {
                            return null;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    /**
     * Map every line of the resources who have the given name
     */
    public static <T> Set<T> asSet(String name, Function<String, T> mapper) {
        return lines(name).map(mapper).collect(Collectors.toSet());
    }

    public static PrintWriter writer(Path path, boolean append) throws FileNotFoundException {
        return new PrintWriter(new FileOutputStream(path.toFile(), append));
    }
}
