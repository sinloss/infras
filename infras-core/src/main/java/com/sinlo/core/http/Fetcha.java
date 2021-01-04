package com.sinlo.core.http;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.wraparound.Ordered;
import com.sinlo.core.http.spec.Method;
import com.sinlo.core.http.spec.Next;
import com.sinlo.core.http.spec.Response;
import com.sinlo.core.http.spec.Timeout;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The http/https fetching utility
 *
 * @author sinlo
 */
public class Fetcha<T> {

    private final URL url;
    private final Method method;
    private final Map<String, String> headers = new HashMap<>();
    private final Course<T> course;

    private boolean followRedirects = true;
    private Consumer<OutputStream> bodyWriter;

    private Fetcha(URL url, Method method, Course<T> course) {
        if (!ifProtocolSupported(this.url = url))
            throw new IllegalArgumentException(
                    String.format("Unsupported protocol %s", url.getProtocol()));
        this.method = method;
        this.course = Objects.requireNonNull(course);
    }

    /**
     * From the given {@link String} fetch via {@link Method} using the {@link Course#RAW}
     */
    public static Fetcha<Response> from(String url, Method method) {
        return Course.RAW.from(url, method);
    }

    /**
     * @see #from(String, Method)
     * @see Method#GET
     */
    public static Fetcha<Response> get(String url) {
        return from(url, Method.GET);
    }

    /**
     * @see #from(String, Method)
     * @see Method#POST
     */
    public static Fetcha<Response> post(String url) {
        return from(url, Method.POST);
    }

    /**
     * @see #from(String, Method)
     * @see Method#PUT
     */
    public static Fetcha<Response> put(String url) {
        return from(url, Method.PUT);
    }

    /**
     * @see #from(String, Method)
     * @see Method#DELETE
     */
    public static Fetcha<Response> delete(String url) {
        return from(url, Method.DELETE);
    }

    /**
     * Set a header
     */
    public Fetcha<T> header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    /**
     * Set headers
     */
    public Fetcha<T> header(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Precisely do the http action without following redirects
     */
    public Fetcha<T> precisely() {
        this.followRedirects = false;
        return this;
    }

    /**
     * Set the {@link #bodyWriter} to a consumer that accepts the output stream of the request
     * and writes the body
     */
    public Fetcha<T> body(Consumer<OutputStream> bodyWriter) {
        this.bodyWriter = bodyWriter;
        return this;
    }

    /**
     * Set the {@link #bodyWriter} to write the given bytes
     */
    public Fetcha<T> body(byte[] bytes) {
        this.bodyWriter = w -> {
            try {
                w.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return this;
    }

    /**
     * Set the {@link #bodyWriter} to write the given text by decoding it using
     * {@link StandardCharsets#UTF_8}
     */
    public Fetcha<T> body(String text) {
        return this.body(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Set the {@link #bodyWriter} to write the given parameter map by joining them in the form
     * of post data
     */
    public Fetcha<T> body(Map<String, String> params) {
        return this.body(params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")));
    }

    /**
     * Do the fetch
     */
    public CompletableFuture<T> fetch() {
        return this.build().thenApply(course.transformer);
    }

    /**
     * Build the {@link HttpURLConnection} and connect
     *
     * @return {@link Response}
     */
    public CompletableFuture<Response> build() {
        CompletableFuture<Response> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method.toString());
                conn.setInstanceFollowRedirects(followRedirects);
                // set timeout if any
                if (course.timeout != null) course.timeout.set(conn);
                headers.forEach(conn::setRequestProperty);
                conn = precept(conn);
                if (bodyWriter == null) {
                    // connect without body
                    conn.connect();
                } else try {
                    // try to connect and write the body
                    conn.setDoOutput(true);
                    conn.connect();
                    try (OutputStream os = conn.getOutputStream()) {
                        this.bodyWriter.accept(os);
                        os.flush();
                    }
                } catch (IllegalStateException ignored) {
                    // ignore the illegal state as the connecting and writing is allowed to happen
                    // in preceptors
                }
                if (Next.RETRY.equals(intercept(conn))) {
                    // retry by build another conn and wait for its response
                    future.complete(this.build().join());
                } else {
                    future.complete(new Response(conn));
                }
            } catch (IOException | Halted e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private HttpURLConnection precept(HttpURLConnection conn) {
        for (Ordered<Function<HttpURLConnection, HttpURLConnection>> preceptor : course.preceptors) {
            conn = preceptor.t.apply(conn);
        }
        return conn;
    }

    private Next intercept(HttpURLConnection conn) {
        for (Ordered<Function<HttpURLConnection, Next>> interceptor : course.interceptors) {
            Next next = interceptor.t.apply(conn);
            if (next != null) {
                switch (next) {
                    case RETRY:
                        return next;
                    case HALT:
                        throw new Halted(interceptor);
                }
            }
        }
        return Next.CONTINUE;
    }

    /**
     * Check if the protocol of the given {@link URL} is supported
     */
    public static boolean ifProtocolSupported(URL url) {
        switch (url.getProtocol()) {
            case "http":
            case "https":
                return true;
            default:
                return false;
        }
    }

    /**
     * A factory that creates {@link Fetcha} instance with specific {@link #interceptors} and {@link #preceptors}
     * and the {@link #transformer}. Meaning that all {@link Fetcha} instance created would follow the same course
     * to finally produce the specified {@link T}
     *
     * @author sinlo
     */
    public static class Course<T> {

        private static final Course<Response> RAW = identity();

        private final List<Ordered<Function<HttpURLConnection, Next>>> interceptors = new LinkedList<>();
        private final List<Ordered<Function<HttpURLConnection, HttpURLConnection>>> preceptors = new LinkedList<>();
        private Timeout timeout;

        public final Function<Response, T> transformer;

        private Course(Function<Response, T> transformer) {
            this.transformer = Objects.requireNonNull(transformer);
        }

        /**
         * The course that transforms the final {@link Response} to a specific type of
         * object
         */
        public static <T> Course<T> of(Function<Response, T> transformer) {
            return new Course<>(transformer);
        }

        /**
         * The course that returns the final {@link Response} as is
         */
        public static Course<Response> identity() {
            return new Course<>(Funny::identity);
        }

        /**
         * Create a {@link Fetcha} instance that would fetch things from the given {@link URL} via the given
         * {@link Method} following the current course
         */
        public Fetcha<T> from(String url, Method method) {
            try {
                return new Fetcha<>(new URL(url), method, this);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @see #from(String, Method)
         * @see Method#GET
         */
        public Fetcha<T> get(String url) {
            return from(url, Method.GET);
        }

        /**
         * @see #from(String, Method)
         * @see Method#POST
         */
        public Fetcha<T> post(String url) {
            return from(url, Method.POST);
        }

        /**
         * @see #from(String, Method)
         * @see Method#PUT
         */
        public Fetcha<T> put(String url) {
            return from(url, Method.PUT);
        }

        /**
         * @see #from(String, Method)
         * @see Method#DELETE
         */
        public Fetcha<T> delete(String url) {
            return from(url, Method.DELETE);
        }

        /**
         * Set the timeout
         *
         * @see Timeout
         */
        public Course<T> timeout(Timeout timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Add an interceptor to the {@link #interceptors}
         */
        public Course<T> intercept(Function<HttpURLConnection, Next> interceptor) {
            Ordered.last(interceptors, interceptor);
            return this;
        }

        /**
         * Add an interceptor to the {@link #interceptors} ordered by the given order
         */
        public Course<T> intercept(Function<HttpURLConnection, Next> interceptor, int order) {
            Ordered.add(interceptors, interceptor, order);
            return this;
        }

        /**
         * Add an preceptor to the {@link #preceptors}
         */
        public Course<T> precept(Function<HttpURLConnection, HttpURLConnection> preceptor) {
            Ordered.last(preceptors, preceptor);
            return this;
        }

        /**
         * Add an preceptor to the {@link #preceptors} ordered by the given order
         */
        public Course<T> precept(Function<HttpURLConnection, HttpURLConnection> preceptor, int order) {
            Ordered.add(preceptors, preceptor, order);
            return this;
        }

    }

    /**
     * @see #fetch()
     */
    public static class Halted extends RuntimeException {

        public Halted(Ordered<Function<HttpURLConnection, Next>> interceptor) {
            super(String.format(
                    "Fetching halted by the interceptor [ %s ] of order [ %s ]",
                    interceptor, interceptor.order));
        }
    }

}
