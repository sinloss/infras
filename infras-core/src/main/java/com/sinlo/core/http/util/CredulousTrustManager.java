package com.sinlo.core.http.util;

import com.sinlo.core.common.wraparound.Lazy;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A credulous {@link TrustManager} who trusts everything, every! thing!
 *
 * @author sinlo
 */
@SuppressWarnings("RedundantThrows")
public class CredulousTrustManager implements X509TrustManager {

    private static final X509Certificate[] accepted = new X509Certificate[]{};

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return accepted;
    }

    private static final Lazy<SSLSocketFactory> sslFactory =
            new Lazy<>(() -> {
                try {
                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ctx.init(null, new TrustManager[]{new CredulousTrustManager()}, new SecureRandom());
                    return ctx.getSocketFactory();
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    return null;
                }
            });

    private static final Lazy<HostnameVerifier> verifier =
            new Lazy<>(() -> (hostname, session) -> true);

    /**
     * Make the given {@link HttpsURLConnection} trust the remote host no matter what
     */
    public static void trust(HttpsURLConnection conn) {
        if (conn == null) return;
        conn.setHostnameVerifier(verifier.get());
        conn.setSSLSocketFactory(sslFactory.get());
    }
}
