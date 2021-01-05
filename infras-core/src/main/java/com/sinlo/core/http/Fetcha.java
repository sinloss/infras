package com.sinlo.core.http;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Jason;
import com.sinlo.core.common.util.Strine;
import com.sinlo.core.common.wraparound.Ordered;
import com.sinlo.core.http.spec.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The http/https fetching utility
 *
 * @author sinlo
 */
public class Fetcha<T> {

    private URL url;
    private final URI uri;
    private final Method method;
    private final Map<String, String> headers = new HashMap<>();
    private BodyType bodyType = BodyType.FORM;
    private final Course<T> course;
    private final CookieManager cookieManager;

    private boolean followRedirects = true;
    private Consumer<OutputStream> bodyWriter;

    private Fetcha(URL url, Method method, Course<T> course, CookieManager cookieManager) {
        if (!ifProtocolSupported(this.url = url))
            throw new IllegalArgumentException(
                    String.format("Unsupported protocol %s", url.getProtocol()));
        this.uri = strip(this.url);
        this.method = method;
        this.course = Objects.requireNonNull(course);
        this.cookieManager = cookieManager;
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
     * Set a cookie
     */
    public Fetcha<T> cookie(HttpCookie cookie) {
        if (cookieManager != null) {
            cookieManager.getCookieStore().add(uri, cookie);
        }
        return this;
    }

    /**
     * Set cookies
     */
    public Fetcha<T> cookie(List<HttpCookie> cookies) {
        if (cookieManager != null && cookies != null) {
            cookies.forEach(this::cookie);
        }
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
    public Fetcha<T> body(Map<String, ?> params) {
        bodyType = BodyType.FORM;
        return this.body(queryString(params));
    }

    /**
     * Set the {@link #bodyWriter} to write the given {@link Jason.Thingamabob} by deserialize it
     * as json
     *
     * @see Jason.Thingamabob#toString()
     */
    public Fetcha<T> body(Jason.Thingamabob thingamabob) {
        bodyType = BodyType.JSON;
        return this.body(thingamabob.toString());
    }

    public Fetcha<T> query(Map<String, ?> params) {
        try {
            this.url = new URL(this.url.toString()
                    .concat(Strine.isEmpty(this.url.getQuery()) ? "?" : "&")
                    .concat(queryString(params)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Fetcha<T> type(BodyType bodyType) {
        this.bodyType = bodyType;
        return this;
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
                HttpURLConnection conn = precept((HttpURLConnection) url.openConnection());
                conn.setRequestMethod(method.toString());
                conn.setInstanceFollowRedirects(followRedirects);
                // set timeout if any
                if (course.timeout != null) course.timeout.set(conn);
                conn.setRequestProperty("Content-Type", (bodyType == null ? BodyType.FORM : bodyType).value);
                headers.forEach(conn::setRequestProperty);
                // take the cookies
                carryCookies(conn);
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
                Response response = Next.RETRY.equals(intercept(conn))
                        // retry by build another conn and wait for its response
                        ? this.build().join()
                        : new Response(conn);
                storeCookies(response);
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Get all cookies of the current {@link Fetcha}
     */
    public List<HttpCookie> cookies() {
        CookieStore store;
        if (cookieManager == null
                || (store = cookieManager.getCookieStore()) == null) {
            return Collections.emptyList();
        }
        return store.get(uri);
    }

    /**
     * Get the cookie of the given name
     */
    public HttpCookie cookie(String name) {
        return this.cookies().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void storeCookies(Response response) {
        // no storing cookies when the cookie manager is system wide
        if (cookieManager == null) return;
        try {
            cookieManager.put(uri, response.conn.getHeaderFields());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void carryCookies(HttpURLConnection conn) {
        // no carrying cookies when the cookie manager is system wide
        if (cookieManager == null) return;
        try {
            cookieManager.get(uri, conn.getRequestProperties()).forEach(
                    (k, list) -> list.forEach(v -> conn.addRequestProperty(k, v)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HttpURLConnection precept(HttpURLConnection conn) {
        for (Ordered<BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection>> preceptor : course.preceptors) {
            conn = preceptor.t.apply(conn, this);
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
     * Strip the given {@link URL} to a mere simple {@link URI} that is mainly used by the
     * {@link CookieManager}
     */
    public static URI strip(URL url) {
        try {
            return new URI(url.getProtocol(), url.getAuthority(),
                    null, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Derive the query string from the given parameter map
     */
    public static String queryString(Map<String, ?> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
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

        /**
         * The cookie center where all cookies are managed globally if not locally
         */
        public static final CookieManager NATIONAL_COOKIE_CENTER = new CookieManager();

        private static final Course<Response> RAW = identity("");

        private final List<Ordered<Function<HttpURLConnection, Next>>> interceptors = new LinkedList<>();
        private final List<Ordered<BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection>>> preceptors = new LinkedList<>();
        private Timeout timeout;
        private CookieManager cookieManager = NATIONAL_COOKIE_CENTER;
        private final String root;
        private final String basic;

        public final Function<Response, T> transformer;

        private Course(String root, Function<Response, T> transformer) {
            if (!Strine.isEmpty(root)) {
                this.root = !root.endsWith("/") ? root.concat("/") : root;
                try {
                    this.basic = strip(new URL(this.root)).toString();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                this.root = this.basic = "";
            }
            this.transformer = Objects.requireNonNull(transformer);
        }

        /**
         * The course that transforms the final {@link Response} to a specific type of
         * object
         */
        public static <T> Course<T> of(String root, Function<Response, T> transformer) {
            return new Course<>(root, transformer);
        }

        /**
         * The course that returns the final {@link Response} as is
         */
        public static Course<Response> identity(String root) {
            return new Course<>(root, Funny::identity);
        }

        /**
         * Create a {@link Fetcha} instance that would fetch things from the given {@link URL} via the given
         * {@link Method} following the current course
         */
        public Fetcha<T> from(String url, Method method) {
            try {
                return new Fetcha<>(
                        new URL((url.startsWith("/") ? basic : root).concat(url)),
                        method, this, cookieManager);
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
         * Use a local cookie manager instead of the {@link #NATIONAL_COOKIE_CENTER}
         */
        public Course<T> cookieLocally() {
            this.cookieManager = new CookieManager();
            return this;
        }

        /**
         * Use none cookie manager at all
         */
        public Course<T> cookieNone() {
            this.cookieManager = null;
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
        public Course<T> precept(BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection> preceptor) {
            Ordered.last(preceptors, preceptor);
            return this;
        }

        /**
         * Add an preceptor to the {@link #preceptors} ordered by the given order
         */
        public Course<T> precept(BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection> preceptor, int order) {
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
