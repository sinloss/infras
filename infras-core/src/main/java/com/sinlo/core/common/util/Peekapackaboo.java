package com.sinlo.core.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Peek into a package for classes
 * <br/><strike>Do you want to play peekaboo inside the package?</strike>
 *
 * @author sinlo
 */
public class Peekapackaboo {

    /**
     * Get the path to the given package name
     *
     * @param packName the given package name
     */
    public static Path where(String packName, BiConsumer<Path, Boolean> then) {
        if (packName == null) return null;

        final URL url = ClassLoader.getSystemClassLoader().getResource(
                packName = packName.replace('.', '/'));

        if (url == null) return null;

        try {
            URI uri = url.toURI();

            Path path;
            if (!"jar".equals(uri.getScheme())) {
                path = Paths.get(uri);
                if (then != null) then.accept(path, false);
                return path;
            }
            // in a jar file
            try {
                path = FileSystems.getFileSystem(uri).getPath(packName);
            } catch (final FileSystemNotFoundException ignored) {
                path = FileSystems.newFileSystem(uri, Collections.emptyMap())
                        .getPath(packName);
            }
            then.accept(path, true);
            return path;
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Peek into where the given package name represents for all classes and apply
     * the given {@link Consumer action} on them
     */
    public static void peek(final String packName, int depth, Consumer<Class<?>> action) {
        where(packName, (root, inJar) -> {
            try (final Stream<Path> paths = Files.walk(root, depth)) {
                paths.filter(Files::isRegularFile)
                        .map(Path::toString)
                        .filter(path -> path.endsWith(".class"))
                        .map(path -> path.replace(inJar ? '/' : File.separatorChar, '.'))
                        .map(qname -> qname.substring(
                                qname.indexOf(packName), qname.length() - 6))
                        .map(name -> {
                            try {
                                return Class.forName(name);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .forEach(action);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * {@link #peek(String, int, Consumer) Peek} with max depth
     */
    public static void peek(final String packName, Consumer<Class<?>> action) {
        peek(packName, Integer.MAX_VALUE, action);
    }

}
