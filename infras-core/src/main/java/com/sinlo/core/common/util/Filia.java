package com.sinlo.core.common.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * Filia the file util
 *
 * @author sinlo
 */
public class Filia {

    private static final Map<String, Filia> pool = new HashMap<>();

    public static Filia of(String folder) {
        Filia filia = pool.get(folder);
        if (filia == null) {
            pool.put(folder, filia = new Filia(folder));
        }
        return filia;
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
}
