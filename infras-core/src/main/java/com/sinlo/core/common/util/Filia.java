package com.sinlo.core.common.util;

import com.sinlo.core.common.wraparound.Lazy;

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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Filia the file util
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
        try {
            return new Filia(Files.createTempDirectory(prefix, attrs));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Filia(Path path) {
        this(path, false);
    }

    private Filia(Path path, boolean ensure) {
        this.p = Objects.requireNonNull(path);
        if (ensure && !Files.exists(this.p)) try {
            // create if not exist
            Files.createDirectories(this.p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolve a given path relative to the {@link #p}, the path can target at a sub
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
        try {
            return new Filia(Files.createSymbolicLink(link, this.p, attrs));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
     * Save the given InputStream to the current {@link Filia#p}
     *
     * @param in the InputStream whose underlying content could be a file most of the time
     * @return the number of bytes read or written
     */
    public long save(InputStream in) {
        try (InputStream is = in) {
            Files.copy(in, this.p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * Load the current {@link Filia#p}
     */
    public Item load() {
        return new Item(this.p);
    }

    /**
     * Load many files inside the current {@link Filia#p}, in this case the
     * {@link Filia#p} must be a folder
     *
     * @see #load()
     */
    public Stream<Item> load(String... fn) {
        if (fn != null) {
            return Arrays.stream(fn)
                    .map(this::resolve)
                    .map(Filia::load);
        }
        return null;
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
        try {
            return new FileOutputStream(this.p.toFile(), append);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return Files.lines(p, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Apply the {@link Files#walk(Path, int, FileVisitOption...)} onto the {@link #p} and map
     * the paths to {@link Filia} instances
     */
    public Stream<Filia> walk(int depth, FileVisitOption... options) {
        try {
            return Files.walk(p, depth, options).map(Filia::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Apply the {@link Files#list(Path)} onto the {@link #p} and map the paths to {@link Filia}
     * instances
     */
    public Stream<Filia> list() {
        try {
            return Files.list(p).map(Filia::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        mustRegular(p);
        return Parts.from(p);
    }

    /**
     * @see Files#getLastModifiedTime(Path, LinkOption...)
     */
    public FileTime getLastModifiedTime(LinkOption... options) {
        try {
            return Files.getLastModifiedTime(p, options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see Files#probeContentType(Path)
     */
    public String probeContentType() {
        try {
            return Files.probeContentType(this.p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return Files.isHidden(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        try {
            return Files.isSameFile(p, other);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public String toString() {
        return this.p.toString();
    }

    /**
     * Get the corresponding {@link PrintWriter} of the given path
     */
    public static PrintWriter printer(Path path, boolean append) {
        try {
            return new PrintWriter(new FileOutputStream(path.toFile(), append));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
            if (panic) throw new RuntimeException(e);
            e.printStackTrace();
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
     * Clear the given directory or file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean rm(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path).map(Path::toFile).forEach(File::delete);
                return true;
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
        IllegalPathException.check(Files::isDirectory, path, "directory");
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
     * retrieve the content of the file pointed by the given path and output it
     * to the given OutputStream
     */
    public static void retrieve(OutputStream os, Path path) {
        if (os != null && path != null) {
            CountDownLatch latch = new CountDownLatch(1);
            try (OutputStream out = os) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                AsynchronousFileChannel channel = AsynchronousFileChannel
                        .open(path, StandardOpenOption.READ);
                channel.read(buffer, 0, channel, new CompletionHandler<Integer, AsynchronousFileChannel>() {
                    int pos = 0;

                    @Override
                    public void completed(Integer result, AsynchronousFileChannel attachment) {
                        try {
                            if (result != -1) {
                                out.write(buffer.array()); // output
                                buffer.clear(); // clear
                                attachment.read(buffer, (pos += result), channel, this);
                            } else {
                                out.flush();
                                latch.countDown();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void failed(Throwable exc, AsynchronousFileChannel attachment) {
                        latch.countDown();
                        throw new RuntimeException(exc);
                    }
                });
                latch.await();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The file item that {@link #load()} produces
     */
    public static class Item {
        public final Path path;

        public Item(Path path) {
            this.path = path;
        }

        public void to(OutputStream out) {
            retrieve(out, path);
        }

        public <T> T read(Function<AsynchronousFileChannel, T> reader) {
            try {
                return reader.apply(AsynchronousFileChannel.open(path, StandardOpenOption.READ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
         * Create from the a given {@link Path}
         */
        public static Parts from(Path path) {
            return from(path.getFileName().toString());
        }

        /**
         * Create from the a given file name
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

        private final Filia root;

        private final List<Integer> seq;

        private final Parts parts;

        private Sequence(Filia root, Parts parts, List<Integer> seq) {
            this.root = Objects.requireNonNull(root).mustRegular();
            this.parts = parts;
            this.seq = seq;
        }

        private Sequence(Filia root, Parts parts, int... seq) {
            this(root, parts, IntStream.of(seq).boxed().collect(Collectors.toList()));
        }

        public static Sequence of(Filia filia) {
            try {
                Parts parts = filia.parts();
                return new Sequence(filia, parts,
                        Files.list(filia.path().getParent())
                                .map(Parts::from)
                                .filter(p -> p.basename.startsWith(parts.basename))
                                .filter(p -> p.extension.equals(parts.extension))
                                .map(Parts::getBasename)
                                .map(n -> n.substring(parts.basename.length()))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Create a sequence for the given {@link Path}
         */
        public static Sequence of(Path path) {
            return of(new Filia(path));
        }

        /**
         * Get the name of the i-th member of this sequence
         */
        public String name(int i) {
            if (i == 0)
                return parts.rename(Funny::identity);
            return parts.rename(name -> name.concat(String.valueOf(seq.get(i))));
        }

        /**
         * Get a {@link Filia} instance for the i-th member of this sequence
         *
         * @see #name(int)
         * @see Filia#sibling(String)
         */
        public Filia get(int i) {
            if (i == 0)
                return root;
            return root.sibling(name(i));
        }

        /**
         * Get the first member of this sequence, aka the {@link #root}
         */
        public Filia root() {
            return root;
        }

        /**
         * Get the size of this sequence
         */
        public int size() {
            return seq.size();
        }

        /**
         * Create a new member file of this sequence
         */
        public Filia increase() {
            int len = size();
            seq.add(seq.get(len - 1) + 1);
            return get(len);
        }

        /**
         * For each member of the sequence, consume it using the given {@link Consumer}
         */
        public void forEach(Consumer<Filia> consumer) {
            this.seq.stream().map(this::get).forEach(consumer);
        }
    }

    /**
     * The file system watcher
     */
    public class Watcher {

        private final Lazy<WatchService>.Default service = new Lazy<>(
                () -> Funny.panic(() -> FileSystems.getDefault().newWatchService())).asDefault();
        private final Lazy<ScheduledExecutorService>.Default ex = new Lazy<>(
                () -> Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()))
                .asDefault();
        private WatchEvent.Kind<?>[] events;
        private WatchEvent.Modifier[] modifiers = new WatchEvent.Modifier[0];
        private long interval;

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
         * Poll in every {@code interval} milliseconds
         */
        public Watcher every(long interval) {
            this.interval = interval;
            return this;
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
         * Perform the given {@link Consumer}. This will finally use all provided variables
         * to register a {@link WatchKey} and start polling
         */
        public ScheduledFuture<?> perform(Consumer<WatchEvent<?>> handler) {
            try {
                final WatchKey wk = Filia.this.p.register(service.get(), events, modifiers);
                return this.ex.get().scheduleAtFixedRate(
                        () -> wk.pollEvents().forEach(handler),
                        interval, interval, TimeUnit.MILLISECONDS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class IllegalPathException extends RuntimeException {

        public IllegalPathException(Path path, String expected) {
            super(String.format("The path %s is not a %s", path, expected));
        }

        public static void check(Predicate<Path> predicate, Path path, String expected) {
            if (!predicate.test(path)) {
                throw new IllegalPathException(path, expected);
            }
        }
    }
}
