package com.sinlo.core.http.spec;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * The content aware body writer
 *
 * @author sinlo
 */
public class ContentAwareBodyWriter implements Consumer<OutputStream> {

    private final byte[] content;
    private final Charset charset;

    public ContentAwareBodyWriter(byte[] content, Charset charset) {
        this.content = content;
        this.charset = charset;
    }

    @Override
    public void accept(OutputStream os) {
        try {
            os.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] content() {
        return this.content;
    }

    @Override
    public String toString() {
        return new String(content, charset);
    }
}
