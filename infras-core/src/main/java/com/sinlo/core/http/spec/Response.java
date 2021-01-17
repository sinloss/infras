package com.sinlo.core.http.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sinlo.core.common.util.Arria;
import com.sinlo.core.common.util.Filia;
import com.sinlo.core.common.util.Jason;
import com.sinlo.core.common.wraparound.Lazy;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private Response(HttpURLConnection conn) {
        this.headers = new Lazy<>(conn::getHeaderFields);
        this.conn = conn;
    }

    /**
     * Create a response out of the given {@link HttpURLConnection}
     */
    public static Response of(HttpURLConnection conn) {
        return new Response(Objects.requireNonNull(conn));
    }

    /**
     * With the given {@link Charset}
     */
    public Response with(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        return this;
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
     * Get the content {@link InputStream} and map it to a {@link T}
     */
    public <T> T map(Function<InputStream, T> mapper) {
        try (InputStream is = this.conn.getInputStream()) {
            return mapper.apply(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Get the text content
     */
    public String text() {
        return map(is -> new String(Filia.drain(is), charset));
    }

    /**
     * Parse the text content to {@link T}
     */
    public <T> T json(Class<T> clz) {
        return Jason.parse(text(), clz);
    }

    /**
     * Parse the text content to {@link T} using {@link TypeReference}
     */
    public <T> T json(TypeReference<T> typeReference) {
        return Jason.parse(text(), typeReference);
    }

}
