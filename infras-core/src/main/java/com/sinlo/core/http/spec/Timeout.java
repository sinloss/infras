package com.sinlo.core.http.spec;

import java.net.HttpURLConnection;

/**
 * The timeout setting utility
 *
 * @author sinlo
 */
public class Timeout {

    private final Integer connect;

    private final Integer read;

    private Timeout(Integer connect, Integer read) {
        this.connect = connect;
        this.read = read;
    }

    public static Timeout connect(int timeout) {
        return new Timeout(timeout, null);
    }

    public static Timeout read(int timeout) {
        return new Timeout(null, timeout);
    }

    public static Timeout both(int connect, int read) {
        return new Timeout(connect, read);
    }

    public void set(HttpURLConnection conn) {
        if (connect != null)
            conn.setConnectTimeout(connect);
        if (read != null)
            conn.setReadTimeout(read);
    }
}
