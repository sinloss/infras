package com.sinlo.core.common.util;

import com.sinlo.sponte.util.Pool;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * Filia the file util
 *
 * @author sinlo
 */
public class Filia {

    private static final Pool.Simple<Filia> pool = new Pool.Simple<>();

    public static Filia of(String folder) {
        return pool.get(folder, () -> new Filia(folder));
    }

    /**
     * the root location
     */
    private final Path root;

    private Filia(String module) {
        if (!Files.exists(this.root = Paths.get(module))) try {
            // create if not exist
            Files.createDirectories(this.root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path resolve(String fn) {
        return this.root.resolve(stamp(fn));
    }

    /**
     * save the given InputStream with the specific name
     *
     * @param in the InputStream whose underlying content could be a file most of the time
     * @param fn a given file path
     * @return the number of bytes read or written
     */
    public long save(InputStream in, String fn) {
        try {
            Files.copy(in, this.resolve(fn));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * load the given file where the given path [ fn ] indicated
     *
     * @param fn the given path
     */
    public Item load(String fn) {
        return new Item(this.resolve(fn));
    }

    public Stream<Item> load(String... fn) {
        if (fn != null) {
            return Arrays.stream(fn).map(this::load);
        }
        return null;
    }

    private static String stamp(String fn) {
        if (fn == null) return "";
        int sp = fn.substring(0, fn.length() - 1).lastIndexOf(File.separator);
        if (sp == -1) {
            return System.currentTimeMillis() + "-" + fn;
        } else {
            return fn.substring(0, sp) + System.currentTimeMillis() + "-" + fn.substring(sp + 1);
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
    public static boolean clear(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path).map(Path::toFile).forEach(File::delete);
                return true;
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Create a locker for the given path
     */
    public static Lock locker(Path path) {
        return new Lock(path);
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

    public static class Item {
        public final Path path;

        public Item(Path path) {
            this.path = path;
        }

        public void to(OutputStream out) {
            retrieve(out, path);
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
}
