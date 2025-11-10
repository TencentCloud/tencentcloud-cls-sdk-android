package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

public class HttpCredential {
    private SSLContext sslContext;
    private X509TrustManager trustManager;

    public HttpCredential(SSLContext sslContext, X509TrustManager trustManager) {
        this.sslContext = sslContext;
        this.trustManager = trustManager;
    }

    public SSLContext getSslContext() {
        return this.sslContext;
    }

    public X509TrustManager getTrustManager() {
        return this.trustManager;
    }
}
