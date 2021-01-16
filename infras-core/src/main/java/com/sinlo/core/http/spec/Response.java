package com.sinlo.core.http.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sinlo.core.common.util.Arria;
import com.sinlo.core.common.util.Filia;
import com.sinlo.core.common.util.Jason;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The response entity
 *
 * @author sinlo
 */
public class Response {

    private final Map<String, List<String>> headers;
    private final byte[] content;
    private Charset charset = StandardCharsets.UTF_8;

    private Response(Map<String, List<String>> headers, byte[] content) {
        this.headers = headers;
        this.content = content;
    }

    /**
     * Create a response out of the given {@link HttpURLConnection}
     */
    public static Response of(HttpURLConnection conn) {
        try {
            return new Response(conn.getHeaderFields(),
                    Filia.drain(conn.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * With the given {@link Charset}
     */
    public Response withCharset(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        return this;
    }

    /**
     * Get the header identified by the given key
     */
    public String header(String key) {
        List<String> header = headers.get(key);
        if (Arria.isEmpty(header)) return "";
        return Arria.join(header, ",");
    }

    /**
     * Get a {@link Stream} containing all the header values identified by the given key
     */
    public Stream<String> headers(String key) {
        List<String> header = headers.get(key);
        if (Arria.isEmpty(header)) return Stream.empty();
        return header.stream();
    }

    /**
     * Get the original headers
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Get the content bytes
     */
    public byte[] content() {
        return content;
    }

    /**
     * Get the text content
     */
    public String text() {
        return new String(content, charset);
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
