package com.sinlo.core.common.util;

import com.sinlo.core.common.wraparound.It;
import com.sinlo.core.common.wraparound.Lazy;
import com.sinlo.core.common.wraparound.Two;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Filia the file util
 * <br/><strike>Filia wants to manipulate fire, so that she can burn files!</strike>
 *
 * @author sinlo
 */
public class Filia {

    /**
     * the root location
     */
    private final Path p;

    /**
     * Of the given path, ensuring its existing
     */
    public static Filia of(String path) {
        return of(path, true);
    }

    /**
     * Of the given path, ensuring its existing if the given {@code ensure} is true
     */
    public static Filia of(String path, boolean ensure) {
        return new Filia(Paths.get(path), ensure);
    }

    /**
     * Create a temp directory in the default temporary-file directory
     *
     * @see Files#createTempDirectory(String, FileAttribute[])
     */
    public static Filia temp(String prefix, FileAttribute<?>... attrs) {
        return Try.panic(() -> new Filia(Files.createTempDirectory(prefix, attrs)));
    }

    private Filia(Path path) {
        this(path, false);
    }

    private Filia(Path path, boolean ensure) {
        this.p = Objects.requireNonNull(path);
        if (ensure && !Files.exists(this.p))
            Try.tolerate(() -> Files.createDirectories(this.p));
    }

    /**
     * Resolve a given path relative to the {@link #p}, the path can target at a sub-
     * directory or a file
     *
     * @see Path#resolve(Path)
     */
    public Filia resolve(String path) {
        return new Filia(p.resolve(path));
    }

    /**
     * Resolve a sibling file identified by the given {@code fileName}
     *
     * @see Path#resolveSibling(String)
     */
    public Filia sibling(String fileName) {
        return new Filia(p.resolveSibling(fileName));
    }

    /**
     * Create a symlink of the current {@link #p}
     *
     * @return the created symlink
     * @see Files#createSymbolicLink(Path, Path, FileAttribute[])
     */
    public Filia ln(Path link, FileAttribute<?>... attrs) {
        return Try.panic(() ->
                new Filia(Files.createSymbolicLink(link, this.p, attrs)));
    }

    /**
     * Get the parent directory of the current {@link Filia#p}
     */
    public Filia parent() {
        return new Filia(p.getParent());
    }

    /**
     * Get the {@link #p}
     */
    public Path path() {
        return this.p;
    }

    /**
     * Get the absolute path of the {@link #p}
     */
    public Path absolute() {
        return this.p.toAbsolutePath();
    }

    /**
     * @see Sequence#of(Filia)
     */
    public Sequence sequence() {
        return Sequence.of(this);
    }

    /**
     * Map the current {@link Filia#p} to a {@link T}
     */
    public <T> Optional<T> map(Function<Path, T> mapper) {
        return Optional.ofNullable(mapper.apply(p));
    }

    /**
     * Apply {@link #ensure(Path)} on {@link #p}
     */
    public Filia ensure() {
        ensure(p);
        return this;
    }

    /**
     * Apply {@link #rm(Path)} on {@link #p}
     */
    public Filia rm() {
        rm(p);
        return this;
    }

    /**
     * Apply {@link #rm(Path, Predicate)} on {@link #p}
     */
    public Filia rm(Predicate<Path> predicate) {
        rm(p, predicate);
        return this;
    }

    /**
     * Apply {@link #touch(Path)} on {@link #p}
     */
    public Filia touch() {
        touch(p);
        return this;
    }

    /**
     * Apply the {@link #printer(Path, boolean)} onto the {@link #p} with the second
     * parameter assigned to false
     */
    public PrintWriter printer() {
        return printer(p, false);
    }

    /**
     * Apply the {@link #printer(Path, boolean)} onto the {@link #p}
     */
    public PrintWriter printer(boolean append) {
        return printer(p, append);
    }

    /**
     * Get a {@link FileOutputStream} targeting to the {@link #p} with the parameter
     * {@code append} assigned to false
     */
    public FileOutputStream fos() {
        return fos(false);
    }

    /**
     * Get a {@link FileOutputStream} targeting to the {@link #p}
     */
    public FileOutputStream fos(boolean append) {
        return Try.panic(() ->
                new FileOutputStream(this.p.toFile(), append));
    }

    /**
     * Equivalent to <pre>{@code
     *      Filia.read(this.p, start, bufSize, consumer);
     * }</pre>
     *
     * @see #read(Path, int, int, Consumer)
     */
    public CompletableFuture<Integer> read(int start, int bufSize, Consumer<byte[]> consumer) {
        return read(this.p, start, bufSize, consumer);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.read(0, 4096, consumer);
     * }</pre>
     *
     * @see #read(int, int, Consumer)
     * @see #read(Path, int, int, Consumer)
     */
    public CompletableFuture<Integer> read(Consumer<byte[]> consumer) {
        return this.read(0, 4096, consumer);
    }

    /**
     * Equivalent to <pre>{@code
     *      Filia.write(this.p, start, supplier, overwrite);
     * }</pre>
     *
     * @see #write(Path, int, Function, boolean)
     */
    public CompletableFuture<Integer> write(int start, Function<Integer, byte[]> supplier, boolean overwrite) {
        return write(this.p, start, supplier, overwrite);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.write(0, supplier, overwrite);
     * }</pre>
     *
     * @see #write(Path, int, Function, boolean)
     * @see #write(int, Function, boolean)
     */
    public CompletableFuture<Integer> write(Function<Integer, byte[]> supplier, boolean overwrite) {
        return this.write(0, supplier, overwrite);
    }

    /**
     * Apply {@link AsynchronousFileChannel#open(Path, OpenOption...)} on the {@link #p}
     *
     * @see AsynchronousFileChannel#open(Path, OpenOption...)
     */
    public AsynchronousFileChannel channel(OpenOption... options) {
        try {
            return AsynchronousFileChannel.open(this.p, options);
        } catch (IOException e) {
            return Try.tolerate(e);
        }
    }

    /**
     * Equivalent to <pre>{@code
     *      this.channel(StandardOpenOption.READ);
     * }</pre>
     */
    public AsynchronousFileChannel channelRead() {
        return channel(StandardOpenOption.READ);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.channel(StandardOpenOption.WRITE);
     * }</pre>
     */
    public AsynchronousFileChannel channelWrite() {
        return channel(StandardOpenOption.WRITE);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.channel(StandardOpenOption.WRITE, StandardOpenOption.APPEND);
     * }</pre>
     */
    public AsynchronousFileChannel channelAppend() {
        return channel(StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    /**
     * Copy the given InputStream to the current {@link Filia#p}
     *
     * @param in        the InputStream whose underlying content could be a file most of the time
     * @param overwrite should overwrite the existing file or not
     * @return the number of bytes read or written
     */
    public long copy(InputStream in, boolean overwrite) {
        try (InputStream is = in) {
            return overwrite
                    ? Files.copy(in, this.p, StandardCopyOption.REPLACE_EXISTING)
                    : Files.copy(in, this.p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * Write from the start, overwriting the existing contents
     */
    public Filia write(Consumer<FileOutputStream> consumer) {
        return write(consumer, false);
    }

    /**
     * Write contents to the {@link #p}
     *
     * @param consumer in which the writing happens
     * @param append   should append to the existing contents
     */
    public Filia write(Consumer<FileOutputStream> consumer, boolean append) {
        try (FileOutputStream fos = new FileOutputStream(this.p.toFile(), append)) {
            consumer.accept(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Print from the start, overwriting the existing contents
     */
    public Filia print(Consumer<PrintWriter> consumer) {
        return print(consumer, false);
    }

    /**
     * Print contents to the {@link #p} using {@link PrintWriter}
     *
     * @param consumer in which the printing happens
     * @param append   should append to the existing contents
     */
    public Filia print(Consumer<PrintWriter> consumer, boolean append) {
        try (PrintWriter pw = this.printer(append)) {
            consumer.accept(pw);
        }
        return this;
    }

    /**
     * @see #mustExist(Path)
     */
    public Filia mustExist() {
        mustExist(p);
        return this;
    }

    /**
     * @see #mustRegular(Path)
     */
    public Filia mustRegular() {
        mustRegular(p);
        return this;
    }

    /**
     * @see #mustDirectory(Path)
     */
    public Filia mustDirectory() {
        mustDirectory(p);
        return this;
    }

    /**
     * @see #lines(Charset)
     */
    public Stream<String> lines() {
        return lines(StandardCharsets.UTF_8);
    }

    /**
     * @see Files#lines(Path, Charset)
     */
    public Stream<String> lines(Charset charset) {
        return Try.panic(() -> Files.lines(p, charset));
    }

    /**
     * Apply the {@link Files#walk(Path, int, FileVisitOption...)} onto the {@link #p} and map
     * the paths to {@link Filia} instances
     */
    public Stream<Filia> walk(int depth, FileVisitOption... options) {
        return Try.panic(() -> Files.walk(p, depth, options).map(Filia::new));
    }

    /**
     * Select specific files under the {@link #p}
     */
    public Stream<Filia> select(String... names) {
        return Arrays.stream(names).map(this::resolve);
    }

    /**
     * Apply the {@link Files#list(Path)} onto the {@link #p} and map the paths to {@link Filia}
     * instances
     */
    public Stream<Filia> list() {
        return Try.panic(() -> Files.list(p).map(Filia::new));
    }

    /**
     * Create a {@link Watcher} which will be watching on the {@link #p}
     *
     * @see Watcher
     */
    public Watcher watch() {
        return new Watcher();
    }

    /**
     * Get a {@link Parts} of the current file
     */
    public Parts parts() {
        return Parts.from(p);
    }

    /**
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     */
    public FileTime getLastModifiedTime(LinkOption... options) {
        return Try.panic(() -> Files.getLastModifiedTime(p, options));
    }

    /**
     * @see Files#probeContentType(Path)
     */
    public String probeContentType() {
        return Try.panic(() -> Files.probeContentType(this.p));
    }

    /**
     * @see Files#isDirectory(Path, LinkOption...)
     */
    public boolean isDirectory(LinkOption... options) {
        return Files.isDirectory(p, options);
    }

    /**
     * @see Files#isRegularFile(Path, LinkOption...)
     */
    public boolean isRegularFile(LinkOption... options) {
        return Files.isRegularFile(p, options);
    }

    /**
     * @see Files#isWritable(Path)
     */
    public boolean isWritable() {
        return Files.isWritable(p);
    }

    /**
     * @see Files#isReadable(Path)
     */
    public boolean isReadable() {
        return Files.isReadable(p);
    }

    /**
     * @see Files#isExecutable(Path)
     */
    public boolean isExecutable() {
        return Files.isExecutable(p);
    }

    /**
     * @see Files#isHidden(Path)
     */
    public boolean isHidden() {
        return Try.panic(() -> Files.isHidden(p));
    }

    /**
     * @see Files#isSymbolicLink(Path)
     */
    public boolean isSymbolicLink() {
        return Files.isSymbolicLink(p);
    }

    /**
     * @see Files#isSameFile(Path, Path)
     */
    public boolean isSameWith(Path other) {
        return Try.panic(() -> Files.isSameFile(p, other));
    }

    /**
     * @see Files#exists(Path, LinkOption...)
     */
    public boolean exists(LinkOption... options) {
        return Files.exists(p, options);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.resolve(name).map(Filia::locker)
     * }</pre>
     */
    public Optional<Lock> locker(String name) {
        return resolve(name).map(Filia::locker);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.resolve(name).map(Filia::ensure).orElse(false)
     * }</pre>
     */
    public boolean ensure(String name) {
        return resolve(name).map(Filia::ensure).orElse(false);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.resolve(name).map(Filia::touch).orElse(false)
     * }</pre>
     */
    public boolean touch(String name) {
        return resolve(name).map(Filia::touch).orElse(false);
    }

    /**
     * Equivalent to <pre>{@code
     *      this.resolve(name).map(Filia::rm).orElse(false)
     * }</pre>
     */
    public boolean rm(String name) {
        return resolve(name).map(Filia::rm).orElse(false);
    }

    /**
     * Just rename
     *
     * @param name new filename
     */
    public Filia rename(String name, CopyOption... options) {
        if (Objects.requireNonNull(name).contains(File.pathSeparator)) {
            throw new InvalidPathException(name,
                    String.format("The filename must not contains %s", File.pathSeparator));
        }
        return mv(name, options);
    }

    /**
     * Rename with a function that accepts the current filename represented by
     * a {@link Parts}
     */
    public Filia rename(Function<Parts, String> resolver, CopyOption... options) {
        return rename(resolver.apply(parts()), options);
    }

    /**
     * Move to the target location where the {@code to} is resolved by the {@link #p}
     *
     * @return the {@link Filia} to the target location {@code to}
     */
    public Filia mv(String to, CopyOption... options) {
        return new Filia(Try.panic(() -> Files.move(p, p.resolve(to), options)));
    }

    /**
     * Copy to the target location where the {@code to} is resolved by the {@link #p}
     *
     * @return a {@link Two} whose {@code two.one} is the source {@link Filia} and {@code two.another}
     * the {@link Filia} of the target location {@code to}
     */
    public Two<Filia, Filia> cp(String to, CopyOption... options) {
        return Two.two(this, new Filia(Try.panic(() -> Files.copy(p, p.resolve(to), options))));
    }

    @Override
    public String toString() {
        return this.p.toString();
    }

    /**
     * Get the corresponding {@link PrintWriter} of the given path
     */
    public static PrintWriter printer(Path path, boolean append) {
        return Try.panic(() ->
                new PrintWriter(new FileOutputStream(path.toFile(), append)));
    }

    /**
     * @see #drain(InputStream, boolean)
     */
    public static byte[] drain(InputStream in) {
        return drain(in, false);
    }

    /**
     * Read all the bytes from the given {@link InputStream in}, throws a {@link RuntimeException}
     * enclosing the caught {@link IOException} if {@code panic}
     */
    public static byte[] drain(InputStream in, boolean panic) {
        try (InputStream is = in;
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
            return baos.toByteArray();
        } catch (IOException e) {
            if (panic)
                return Try.toss(e);
            Try.tolerate(e);
            return new byte[0];
        }
    }

    /**
     * Ensure the folder identified by the given path exist
     */
    public static boolean ensure(final Path path) {
        try {
            if (Files.notExists(path))
                Files.createDirectories(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * An equivalent of linux command {@code touch}
     */
    public static boolean touch(final Path path) {
        try {
            if (Files.exists(path)) {
                Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
                return true;
            }
            if (ensure(path.getParent())) {
                Files.createFile(path);
                return true;
            }
            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * <pre>{@code rm(path, Funny::aye)}</pre>
     * A double sequential call on the same folder will remove the directory according to
     * the behavior of the {@link #rm(Path, Predicate)}
     *
     * @see #rm(Path, Predicate)
     */
    public static boolean rm(Path path) {
        return rm(path, Funny::aye);
    }

    /**
     * Clear the given directory or delete the file, and the folder itself if
     * it is empty
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean rm(Path path, Predicate<Path> predicate) {
        if (!Files.exists(path)) return true;
        try {
            if (Files.isDirectory(path)) {
                // indicates if the folder is initially empty before this call
                It<Boolean, Void> empty = It.just(true);
                Files.walk(path).peek(__ -> empty.mutate(false)).filter(predicate)
                        .map(Path::toFile).forEach(File::delete);
                // as long as it is not empty before this call, do not delete the folder
                if (!empty.get()) return true;
            }
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Panic if the given {@link Path} is not a regular file
     */
    public static void mustRegular(Path path) {
        IllegalPathException.check(Files::isRegularFile, path, "a regular file");
    }

    /**
     * Panic if the given {@link Path} is not a directory
     */
    public static void mustDirectory(Path path) {
        IllegalPathException.check(Files::isDirectory, path, "a directory");
    }

    /**
     * Panic if the given {@link Path} is not present
     */
    public static void mustExist(Path path) {
        IllegalPathException.check(Files::exists, path, "present");
    }

    /**
     * Create a locker for the given path
     */
    public static Lock locker(Path path) {
        return new Lock(path);
    }

    /**
     * Create a {@link Watcher} targeting at the given {@link Path}
     *
     * @see Filia#watch()
     * @see Watcher
     */
    public static Watcher watch(Path path) {
        return new Filia(path).watch();
    }

    /**
     * Asynchronously read the all the content of the file identified by the given path
     *
     * @param path     the file path
     * @param start    starting from this given position of the file
     * @param bufSize  allocate a {@link ByteBuffer} of this size
     * @param consumer consumes the read bytes in the buffer
     * @return the final size <b>starting from 0</b>
     */
    public static CompletableFuture<Integer> read(
            Path path, int start, int bufSize, Consumer<byte[]> consumer) {
        mustRegular(path);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(bufSize);
                AsynchronousFileChannel chan =
                        AsynchronousFileChannel.open(path, StandardOpenOption.READ);
                chan.read(buffer, start, buffer, new CompletionHandler<Integer, ByteBuffer>() {

                    private int pos = start;

                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        if (result != -1) {
                            attachment.flip();
                            consumer.accept(attachment.array());
                            attachment.clear();
                            chan.read(buffer, (pos += result), buffer, this);
                        } else {
                            future.complete(pos);
                        }
                    }

                    @Override
                    public void failed(Throwable e, ByteBuffer attachment) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Asynchronously write the content supplied by the given {@code supplier} to the file
     * identified by the given path. The {@code supplier} accepts an Integer parameter with
     * represents the number of wrote bytes of the last writing, and a null represents the
     * first time of writing at which none byte has been written. If the {@code supplier}
     * wants to stop the writing process, it could simply return a null.
     *
     * @param path      the file path
     * @param start     starting from this given position of the file
     * @param supplier  supplies the byte[] to be written
     * @param overwrite should overwrite the existing contents or not
     * @return the final size <b>starting from 0</b>
     */
    public static CompletableFuture<Integer> write(Path path, int start, Function<Integer, byte[]> supplier, boolean overwrite) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        OpenOption[] options = overwrite
                ? new OpenOption[]{StandardOpenOption.WRITE}
                : new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.APPEND};
        CompletableFuture.runAsync(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(supplier.apply(null));
                AsynchronousFileChannel chan = AsynchronousFileChannel.open(path, options);
                chan.write(buffer, start, null, new CompletionHandler<Integer, Void>() {

                    private int pos = start;

                    @Override
                    public void completed(Integer result, Void attachment) {
                        buffer.clear();
                        // if wrote 0 bytes or supplier supplies nothing, then complete the future
                        // with the current pos
                        if (result == 0 ||
                                Funny.maybe(supplier.apply(result), buffer::put) == null) {
                            future.complete(pos);
                            return;
                        }
                        // otherwise, do the writing
                        buffer.put(supplier.apply(result));
                        chan.write(buffer, (pos += result), null, this);
                    }

                    @Override
                    public void failed(Throwable e, Void attachment) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * The two parts of a filename
     * <ul>
     *     <li>basename</li>
     *     <li>extension</li>
     * </ul>
     */
    public static class Parts {

        public final String basename;

        public final String extension;

        private Parts(String basename, String extension) {
            this.basename = basename;
            this.extension = extension;
        }

        /**
         * Rename
         */
        public String rename(Function<String, String> re) {
            return re.apply(this.basename)
                    .concat(this.extension.isEmpty() ? "" : ".".concat(this.extension));
        }

        public String getBasename() {
            return basename;
        }

        public String getExtension() {
            return extension;
        }

        /**
         * Create from the given {@link Path}
         */
        public static Parts from(Path path) {
            return from(path.getFileName().toString());
        }

        /**
         * Create from the given file name
         */
        public static Parts from(String fileName) {
            int delim = fileName.lastIndexOf(".");
            if (delim == -1)
                return new Parts(fileName, "");
            return new Parts(fileName.substring(0, delim), fileName.substring(delim + 1));
        }
    }

    /**
     * The file lock
     */
    public static class Lock {
        private final Path l;

        private Lock(Path l) {
            ensure((this.l =
                    Files.isDirectory(l) ? l.resolve(".lock") : l).getParent());
        }

        public boolean locked() {
            return Files.exists(l);
        }

        public boolean unlock() {
            try {
                Files.deleteIfExists(l);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        public boolean lock() {
            try {
                Files.createFile(l);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * A sequence of files having the same prefix name, and sequential numbers
     * as their suffixes sharing the same extension
     */
    public static class Sequence {

        private final Member root;

        private final NavigableSet<Integer> seq;

        private final Parts parts;

        private Sequence(Filia root, Parts parts, NavigableSet<Integer> seq) {
            this.root = new Member(0, Objects.requireNonNull(root));
            this.parts = parts;
            this.seq = Objects.requireNonNull(seq);
        }

        private Sequence(Filia root, Parts parts, int... seq) {
            this(root, parts, IntStream.of(seq).boxed().collect(Collin.toSkipList()));
        }

        /**
         * Create a sequence for the given {@link Filia}
         */
        public static Sequence of(Filia root) {
            Parts parts = root.parts();
            return Try.panic(() -> new Sequence(root, parts,
                    // Prepend a 0 to the stream as the root must be in the seq no matter the
                    // file exists or not.
                    Stream.concat(Stream.of(0), root.parent().mustExist().list()
                            .map(Filia::parts)
                            .filter(p -> p.basename.startsWith(parts.basename)
                                    && p.extension.equals(parts.extension))
                            .map(Parts::getBasename)
                            .map(n -> n.substring(parts.basename.length()))
                            // Non-empty, meaning the root file is excluded here as a stream of 0
                            // will be prepended at be beginning of the stream. The 0 represents
                            // the root file in the context of sequence
                            .filter(Strine::nonEmpty)
                            // Parse to int, and all the non-numeric values who would throw a
                            // NumberFormatException will be turned into nulls and be filtered
                            // off in the next operation
                            .map(n -> Try.of(() -> Integer.parseInt(n))
                                    .caught(NumberFormatException.class)
                                    .thenNull()
                                    .otherwiseNull()
                                    .exert())
                            .filter(Objects::nonNull)
                            // Only accepts positive values. Since the 0 has already been prepended
                            // ignore the file that has a 0 as it's suffix
                            .filter(i -> i > 0))
                            // ascending
                            .sorted()
                            .collect(Collin.toSkipList())));
        }

        /**
         * Create a sequence for the given {@link Path}
         */
        public static Sequence of(Path root) {
            return of(new Filia(root));
        }

        /**
         * Get the name having the number {@code i} as its suffix in this sequence
         */
        public String name(int i) {
            if (i == 0)
                return parts.rename(Funny::identity);
            return parts.rename(name -> name.concat(String.valueOf(i)));
        }

        /**
         * Get a {@link Filia} instance having the number {@code i} as its suffix in this sequence
         *
         * @see #name(int)
         * @see Filia#sibling(String)
         */
        public Member of(int i) {
            if (i == 0)
                return root;
            return root.sibling(i);
        }

        /**
         * Get the first member of this sequence, aka the {@link #root}
         */
        public Member root() {
            return root;
        }

        /**
         * Get the last member of this sequence
         */
        public Member last() {
            return of(seq.last());
        }

        /**
         * Get the size of this sequence
         */
        public int size() {
            return seq.size();
        }

        /**
         * Equivalent to {@code this.all(true)} which means only get the next when the last one
         * practically exists
         */
        public Member next() {
            return next(true);
        }

        /**
         * Create a new member file of this sequence having a greater serial than the last one
         * which must exist, otherwise return the {@link #last()}. If the {@code practical} is
         * given false, then just produce the new member no matter what
         */
        public Member next(boolean practical) {
            if (seq.isEmpty()) {
                // init the root member if empty
                seq.add(0);
                return root;
            }
            return Funny.maybe(last(),
                    m -> (practical && !m.self.exists()) ? m : m.next());
        }

        /**
         * Equivalent to {@code this.all(true)} which means only get all the practically existing
         * members
         *
         * @see #all(boolean)
         */
        public Stream<Member> all() {
            return all(true);
        }

        /**
         * Get all the files in the sequence
         *
         * @param practical the file should be practically on the disk or not
         */
        public Stream<Member> all(boolean practical) {
            Stream<Member> all = seq.stream().map(this::of);
            if (!practical) {
                return all;
            }
            return all.filter(m -> m.self.exists());
        }

        /**
         * The member of {@link Sequence}
         */
        public class Member {

            /**
             * The serial
             */
            public final int serial;

            /**
             * The underlying {@link Filia}
             */
            public final Filia self;

            public Member(int serial, Filia self) {
                this.serial = serial;
                this.self = self;
            }

            /**
             * The backing {@link Sequence}
             */
            public Sequence sequence() {
                return Sequence.this;
            }

            /**
             * Get a sibling of suffix {@code i}
             */
            public Member sibling(int i) {
                return new Member(i, Sequence.this.root.self.sibling(name(i)));
            }

            public int serial() {
                return serial;
            }

            public Filia self() {
                return self;
            }

            /**
             * Get the next member
             */
            public Member next() {
                int next = serial + 1;
                Sequence.this.seq.add(next);
                return sibling(next);
            }

            /**
             * Get the previous member
             */
            public Member prev() {
                return sibling(serial - 1);
            }

            /**
             * Check if this member is the root member
             */
            public boolean isRoot() {
                return serial == 0;
            }
        }
    }

    /**
     * The file system watcher
     */
    public class Watcher {

        private final Lazy<WatchService>.Default service = new Lazy<>(
                () -> Try.panic(() -> FileSystems.getDefault().newWatchService())).asDefault();
        private final Lazy<ScheduledExecutorService>.Default ex = new Lazy<>(
                () -> Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()))
                .asDefault();
        private WatchEvent.Kind<?>[] events;
        private WatchEvent.Modifier[] modifiers = new WatchEvent.Modifier[0];
        private long interval = 1L;
        private long delay = 0L;

        private Watcher() {
        }

        /**
         * On what events. This would replace the previous set events
         */
        public Watcher on(WatchEvent.Kind<?>... events) {
            this.events = events;
            return this;
        }

        /**
         * And other events. This will <b>not</b> replace the previous set events
         */
        public Watcher and(WatchEvent.Kind<?>... events) {
            this.events = Arria.concat(this.events, events);
            return this;
        }

        /**
         * Mod by what modifiers. This would replace the previous set modifiers
         */
        public Watcher mod(WatchEvent.Modifier... modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        /**
         * With other modifiers. This will <b>not</b> replace the previous set modifiers
         */
        public Watcher with(WatchEvent.Modifier... modifiers) {
            this.modifiers = Arria.concat(this.modifiers, modifiers);
            return this;
        }

        /**
         * Start after {@code delay} milliseconds, it is 0 milliseconds by default
         */
        public Watcher after(long delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Start after {@code delay} {@link TimeUnit}, it is 0 milliseconds by default
         */
        public Watcher after(long delay, TimeUnit tu) {
            return after(tu.toMillis(delay));
        }

        /**
         * Poll in every {@code interval} milliseconds, it is 1 milliseconds by default
         */
        public Watcher every(long interval) {
            this.interval = interval;
            return this;
        }

        /**
         * Poll in every {@code interval} {@link TimeUnit}, it is 1 milliseconds by default
         */
        public Watcher every(long interval, TimeUnit tu) {
            return every(tu.toMillis(interval));
        }

        /**
         * Use the given {@link WatchService}
         */
        public Watcher service(WatchService service) {
            this.service.provide(service);
            return this;
        }

        /**
         * Use the given {@link ScheduledExecutorService} to do the scheduling
         */
        public Watcher use(ScheduledExecutorService ex) {
            this.ex.provide(ex);
            return this;
        }

        /**
         * Perform the given {@link BiConsumer}. This will finally use all provided variables
         * to register a {@link WatchKey} and start polling
         */
        public Scheduled perform(BiConsumer<Filia, List<WatchEvent<?>>> handler) {
            final WatchKey wk = Try.panic(() ->
                    Filia.this.p.register(service.get(), events, modifiers));
            return new Scheduled(this.ex.get().scheduleAtFixedRate(
                    () -> handler.accept(Filia.this, wk.pollEvents()),
                    delay, interval, TimeUnit.MILLISECONDS), wk);
        }

        /**
         * A container consisting of a {@link ScheduledFuture} and a {@link WatchKey}
         */
        public class Scheduled {
            /**
             * The future produced by scheduling
             */
            public final ScheduledFuture<?> future;
            /**
             * The watch key of the current watcher
             */
            public final WatchKey wk;

            private Scheduled(ScheduledFuture<?> future, WatchKey wk) {
                this.future = future;
                this.wk = wk;
            }

            private boolean stop(boolean interrupt) {
                wk.cancel();
                return future.cancel(interrupt);
            }

            /**
             * Cancel the {@link #wk} and {@link #future} leniently
             */
            public boolean cancel() {
                return stop(false);
            }

            /**
             * Kill the {@link #wk} and {@link #future} forcefully
             */
            public boolean kill() {
                return stop(true);
            }
        }
    }

    public static class IllegalPathException extends InvalidPathException {

        public IllegalPathException(Path path, String expected) {
            super(path.toString(), String.format(
                    "The path %s is not %s", path, expected));
        }

        public static void check(Predicate<Path> predicate, Path path, String expected) {
            if (!predicate.test(path)) {
                throw new IllegalPathException(path, expected);
            }
        }
    }
}
