package com.sinlo.core.http.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sinlo.core.common.util.Jason;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The response entity
 *
 * @author sinlo
 */
public class Response {

    public final HttpURLConnection conn;
    private Charset charset = StandardCharsets.UTF_8;

    public Response(HttpURLConnection conn) {
        this.conn = conn;
    }

    /**
     * With the given {@link Charset}
     */
    public Response withCharset(Charset charset) {
        this.charset = charset == null ? StandardCharsets.UTF_8 : charset;
        return this;
    }

    /**
     * Get the {@link InputStream} of the {@link #conn}
     */
    public InputStream inputStream() {
        try {
            return conn.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the text content of the {@link #conn}
     */
    public String text() {
        final StringBuilder out = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), charset.toString()))) {
            String line;
            while ((line = in.readLine()) != null) {
                out.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }
        return out.toString();
    }

    /**
     * Parse the text content of the {@link #conn} to {@link T}
     */
    public <T> T json(Class<T> clz) {
        return Jason.parse(text(), clz);
    }

    /**
     * Parse the text content of the {@link #conn} to {@link T} using {@link TypeReference}
     */
    public <T> T json(TypeReference<T> typeReference) {
        return Jason.parse(text(), typeReference);
    }

}
