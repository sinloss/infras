package com.sinlo.core.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.sinlo.core.common.util.Arria;
import com.sinlo.core.common.util.Funny;
import com.sinlo.core.common.util.Jason;
import com.sinlo.core.common.util.Strine;
import com.sinlo.core.common.wraparound.Ordered;
import com.sinlo.core.http.spec.*;
import com.sinlo.core.http.util.CredulousTrustManager;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
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
    private BodyType bodyType;
    private final Course<T> course;
    private final CookieManager cookieManager;

    private boolean followRedirects = true;
    private boolean credulous;
    private Consumer<OutputStream> bodyWriter;
    private Timeout timeout;

    private Fetcha(URL url, Method method, Course<T> course, CookieManager cookieManager) {
        if (!ifProtocolSupported(this.url = url))
            throw new IllegalArgumentException(
                    String.format("Unsupported protocol %s", url.getProtocol()));
        this.uri = strip(this.url);
        this.method = method;
        this.course = Objects.requireNonNull(course);
        this.credulous = this.course.credulous;
        this.timeout = this.course.timeout;
        this.cookieManager = cookieManager;
        // set the default body type
        type(BodyType.FORM);
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
    public Fetcha<T> header(String key, String... values) {
        this.headers.put(key, Arria.join(",", values));
        return this;
    }

    /**
     * Set the header if absent
     */
    public Fetcha<T> headerIfAbsent(String key, String... values) {
        this.headers.putIfAbsent(key, Arria.join(",", values));
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
        this.bodyWriter = new ContentAwareBodyWriter(bytes, Charset.defaultCharset());
        return this;
    }

    /**
     * Set the {@link #bodyWriter} to write the given text by decoding it using
     * {@link Charset#defaultCharset()}
     */
    public Fetcha<T> body(String text) {
        return this.body(text.getBytes(Charset.defaultCharset()));
    }

    /**
     * Set the {@link #bodyWriter} to write the given form data string
     */
    public Fetcha<T> form(String form) {
        return this.type(BodyType.FORM).body(form);
    }

    /**
     * Set the {@link #bodyWriter} to write the given parameter map by joining them in the form
     * of post data
     */
    public Fetcha<T> form(Map<String, ?> params) {
        return this.form(queryString(params));
    }

    /**
     * Set the {@link #bodyWriter} to write the given json string
     */
    public Fetcha<T> json(String json) {
        return this.type(BodyType.JSON).body(json);
    }

    /**
     * Set the {@link #bodyWriter} to write the given {@link Object} by deserialize it as json
     *
     * @see Jason#stringify(Object)
     */
    public Fetcha<T> json(Object object) {
        return this.json(Jason.stringify(object));
    }

    /**
     * Set the {@link #bodyWriter} to write the given {@link Jason.Thingamabob} by deserialize it
     * as json
     *
     * @see Jason#map()
     * @see Jason.Thingamabob#from(HashMap)
     */
    public Fetcha<T> json(Jason.Thingamabob thingamabob) {
        return this.json(thingamabob.toString());
    }

    /**
     * Set the {@link #bodyWriter} to write the given {@link JsonNode} by deserialize it
     * as json
     */
    public Fetcha<T> json(JsonNode jn) {
        return this.json(Jason.stringify(jn));
    }

    /**
     * Append query string converted from the given parameter map
     */
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

    /**
     * Set the body type
     *
     * @see BodyType
     */
    public Fetcha<T> type(BodyType bodyType) {
        (this.bodyType = (bodyType == null ? BodyType.NONE : bodyType)).set(this);
        return this;
    }

    /**
     * Trust the current underlying request anyway by making the {@link Fetcha}
     * {@link #credulous}
     *
     * @see #build()
     * @see CredulousTrustManager
     */
    public Fetcha<T> trust() {
        this.credulous = true;
        return this;
    }

    /**
     * Set the timeout
     *
     * @see Timeout
     */
    public Fetcha<T> timeout(Timeout timeout) {
        this.timeout = timeout;
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
                HttpURLConnection conn = precept(Stage.OPEN, (HttpURLConnection)
                        (course.proxy == null ? url.openConnection() : url.openConnection(course.proxy)));
                conn.setRequestMethod(method.toString());
                conn.setInstanceFollowRedirects(followRedirects);
                // set timeout if any
                if (timeout != null) timeout.set(conn);
                headers.forEach(conn::setRequestProperty);
                // take the cookies
                carryCookies(conn);
                if (credulous && conn instanceof HttpsURLConnection) {
                    CredulousTrustManager.trust((HttpsURLConnection) conn);
                }
                conn = precept(Stage.ABOUT_TO_CONNECT, conn);
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
                conn = precept(Stage.CONNECTED, conn);
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

    private HttpURLConnection precept(Stage stage, HttpURLConnection conn) {
        for (Ordered<BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection>> preceptor : course.forStage(stage)) {
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
     * Get the {@link #url} of the current underlying request
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Get the {@link #uri} that are composed with schema and authority parts of
     * the {@link #url}
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Get the {@link #method} of the current underlying request
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Get the {@link #headers} of the current underlying request
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Get the {@link #bodyType} of the current underlying request
     */
    public BodyType getBodyType() {
        return bodyType;
    }

    /**
     * Check if the current underlying request follows redirects or not
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * Get the {@link ContentAwareBodyWriter} typed {@link #bodyWriter}. In case that the
     * {@link #bodyWriter} is not instanceof {@link ContentAwareBodyWriter}, this method would
     * read all bytes out of the {@link #bodyWriter} and then replace the {@link #bodyWriter}
     * with a newly created {@link ContentAwareBodyWriter} which contains all the read bytes
     * and finally return it.
     */
    public ContentAwareBodyWriter getBody() {
        if (bodyWriter instanceof ContentAwareBodyWriter) {
            return (ContentAwareBodyWriter) bodyWriter;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bodyWriter.accept(baos);
        return body(baos.toByteArray()).getBody();
    }

    /**
     * The preceptor stages
     */
    public enum Stage {
        /**
         * The {@link HttpURLConnection} has been open
         */
        OPEN,
        /**
         * The {@link HttpURLConnection} is about to connect
         */
        ABOUT_TO_CONNECT,
        /**
         * The {@link HttpURLConnection} has connected, meaning the underlying request has been sent
         */
        CONNECTED
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

        private static final Course<Response> RAW = identity();

        private final List<Ordered<Function<HttpURLConnection, Next>>> interceptors = new LinkedList<>();
        private final Map<Stage, List<Ordered<BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection>>>> preceptors = new HashMap<>();
        private final String root;
        private final String basic;

        private Timeout timeout;
        private CookieManager cookieManager = NATIONAL_COOKIE_CENTER;
        private boolean credulous = false;
        private Proxy proxy;

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
         * A shortcut of {@link #identity(String)}("")
         */
        public static Course<Response> identity() {
            return identity("");
        }

        /**
         * The course that returns the final {@link Response} as is
         */
        public static Course<Response> identity(String root) {
            return new Course<>(root, Funny::identity);
        }

        /**
         * Use a http proxy
         */
        public Course<T> proxy(String host, int port) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            return this;
        }

        /**
         * Use a socks proxy
         */
        public Course<T> socks(String host, int port) {
            proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
            return this;
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
         * Be a credulous {@link Course} that trusts any remote hosts of every spawned
         * {@link Fetcha}
         */
        public Course<T> credulous() {
            this.credulous = true;
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
        public Course<T> precept(Stage stage, BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection> preceptor) {
            Ordered.last(forStage(stage), preceptor);
            return this;
        }

        /**
         * Add an preceptor to the {@link #preceptors} ordered by the given order
         */
        public Course<T> precept(Stage stage, BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection> preceptor, int order) {
            Ordered.add(forStage(stage), preceptor, order);
            return this;
        }

        private List<Ordered<BiFunction<HttpURLConnection, Fetcha<T>, HttpURLConnection>>> forStage(Stage stage) {
            return preceptors.computeIfAbsent(stage, (k) -> new LinkedList<>());
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
