package com.sinlo.core.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sinlo.core.common.util.*;
import com.sinlo.core.common.wraparound.Lazy;
import com.sinlo.core.http.spec.Status;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The response entity
 *
 * @author sinlo
 */
public class Response {

    private final Lazy<Map<String, List<String>>> headers;
    private final HttpURLConnection conn;
    private Charset charset = StandardCharsets.UTF_8;
    private final Lazy<Status> status;

    /**
     * Whens holds handlers for some specific http statuses. Any handler can abort the
     * following handling processes by returning {@code false}
     */
    private final Map<Status, Function<Response, Boolean>> whens = new HashMap<>();

    /**
     * Otherwise and, if the {@link #status} if not 2xx successful
     */
    private Function<Response, Boolean> otherwise;

    private Response(HttpURLConnection conn) {
        this.headers = new Lazy<>(conn::getHeaderFields);
        this.status = new Lazy<>(() -> Status.resolve(Try.panic(conn::getResponseCode)));
        this.conn = conn;
    }

    /**
     * Create a response out of the given {@link HttpURLConnection}
     */
    public static Response of(HttpURLConnection conn) {
        return new Response(Objects.requireNonNull(conn));
    }

    /**
     * Get the underlying {@link HttpURLConnection}
     */
    public HttpURLConnection connection() {
        return conn;
    }

    /**
     * With the given {@link Charset}
     */
    public Response with(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        return this;
    }

    /**
     * Get the {@link Status} of the current response
     */
    public Status status() {
        return status.get();
    }

    /**
     * Get the header identified by the given key
     */
    public String header(String key) {
        List<String> header = headers().get(key);
        if (Arria.isEmpty(header)) return "";
        return Arria.join(header, ",");
    }

    /**
     * Get a {@link Stream} containing all the header values identified by the given key
     */
    public Stream<String> headers(String key) {
        List<String> header = headers().get(key);
        if (Arria.isEmpty(header)) return Stream.empty();
        return header.stream();
    }

    /**
     * Get the original headers
     */
    public Map<String, List<String>> headers() {
        return headers.get();
    }

    /**
     * Discard contents
     */
    public Response discard() {
        this.conn.disconnect();
        return this;
    }

    /**
     * Create a {@link When} builder to build status code handlers
     */
    public When when(Status... statuses) {
        return new When(statuses);
    }

    /**
     * Otherwise apply the given {@code handle}
     */
    public Response otherwise(Function<Response, Boolean> handle) {
        this.otherwise = handle;
        return this;
    }

    /**
     * Otherwise throw an {@link UnresolvableStatusException}
     */
    public Response otherwiseThrow() {
        return otherwise(UnresolvableStatusException::toss);
    }

    /**
     * Otherwise abort following processes
     */
    public Response otherwiseAbort() {
        return otherwise(r -> false);
    }

    /**
     * Get the content {@link InputStream} and map it to a {@link T}
     */
    public <T> Optional<T> map(Function<InputStream, T> mapper) {
        Status sta = status.get();
        Function<Response, Boolean> h = whens.get(sta);
        // if the handler returns false
        if ((h != null && !Funny.nvl(h.apply(this), false))
                // or "otherwise" returns false when it is not 2xx successful
                || (otherwise != null && !sta.is2xxSuccessful()
                && !Funny.nvl(otherwise.apply(this), false))) {
            // then abort
            return Optional.empty();
        }

        try (InputStream is = this.conn.getInputStream()) {
            return Optional.ofNullable(mapper.apply(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Get the text content
     */
    public Optional<String> text() {
        return map(is -> new String(Filia.drain(is), charset));
    }

    /**
     * Parse the text content to {@link T}
     */
    public <T> Optional<T> json(Class<T> clz) {
        return text().map(t -> Jason.parse(t, clz));
    }

    /**
     * Parse the text content to {@link T} using {@link TypeReference}
     */
    public <T> Optional<T> json(TypeReference<T> typeReference) {
        return text().map(t -> Jason.parse(t, typeReference));
    }

    /**
     * The status code handler builder
     */
    public class When {

        private final Status[] statuses;

        private When(Status[] statuses) {
            this.statuses = statuses;
        }

        /**
         * Then apply the given {@code handle}
         */
        public Response then(Function<Response, Boolean> handle) {
            Arrays.stream(statuses).forEach(s -> Response.this.whens.put(s, handle));
            return Response.this;
        }

        /**
         * Then throw an {@link UnresolvableStatusException}
         */
        public Response thenThrow() {
            return then(UnresolvableStatusException::toss);
        }

        /**
         * Then abort following processes
         */
        public Response thenAbort() {
            return then(r -> false);
        }
    }

    /**
     * The unexpected status code
     */
    public static class UnresolvableStatusException extends RuntimeException {

        private UnresolvableStatusException(Response response) {
            super(String.format(
                    "Got an unresolvable status code %s with message %s",
                    response.status.get(),
                    new String(Filia.drain(Try.tolerate(response.conn::getInputStream)))));
        }

        public static boolean toss(Response response) {
            throw new UnresolvableStatusException(response);
        }
    }
}
