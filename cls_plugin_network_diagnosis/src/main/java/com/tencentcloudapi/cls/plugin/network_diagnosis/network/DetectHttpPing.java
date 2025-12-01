package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.net.Network;
import android.os.Build;

import com.tencentcloudapi.cls.android.CLSLog;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetectHttpPing {

    private static final String TAG = "DetectHttpPing";

    private static OkHttpClient mOkhttpClient;

    DetectHttpPing() {
    }

    public JSONObject doDetectHttpPing(String taskId, String connectionType, Network network,
                                       JSONObject netInfo, HttpConfig config) {
        try {
            String url = config.url;
            SSLContext ctx = null;
            HttpEventListener listener = new HttpEventListener(config, netInfo);
            OkHttpClient client = null;
            X509TrustManager trustManager = null;
            SSLContext sslContext = null;
            SSLSocketFactory sslSocketFactory = null;
            if (config.httpCredential != null) {
                trustManager = config.httpCredential.getTrustManager();
                sslContext = config.httpCredential.getSslContext();
            }

//            if (sslContext == null && sslSocketFactory == null && gHttpCredentialCallback != null) {
//                HttpCredential credential = gHttpCredentialCallback.getCredential(config.url, config.context);
//                if (credential != null) {
//                    sslContext = credential.getSslContext();
//                    trustManager = credential.getTrustManager();
//                }
//            }

            if (trustManager != null || sslContext != null) {
                if (trustManager == null) {
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init((KeyStore)null);
                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                    if (trustManagers.length == 1 && trustManagers[0] instanceof X509TrustManager) {
                        trustManager = (X509TrustManager)trustManagers[0];
                    } else {
                        CLSLog.e(TAG, "Unexpected default trust managers:" + Arrays.toString(trustManagers));
                    }
                }

                if (sslContext == null) {
                    sslContext = SSLContext.getInstance("TLS");
                    sslContext.init((KeyManager[])null, new TrustManager[]{trustManager}, (SecureRandom)null);
                }

                sslSocketFactory = sslContext.getSocketFactory();
            }

            int maxRequests = 5;
            int maxRequestsPerHost = 1;
            Dispatcher dispatcher = new Dispatcher(new ThreadPoolExecutor(0, maxRequests, 60L, TimeUnit.SECONDS, new SynchronousQueue()));
            dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
            mOkhttpClient = (new OkHttpClient()).newBuilder().dispatcher(dispatcher).build();
            OkHttpClient.Builder builder = mOkhttpClient.newBuilder();
            builder.eventListener(listener).connectTimeout((long)config.timeout, TimeUnit.MILLISECONDS).readTimeout((long)config.timeout, TimeUnit.MILLISECONDS).writeTimeout((long)config.timeout, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).connectionPool(new ConnectionPool(0, 5L, TimeUnit.SECONDS));
            if (network != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.socketFactory(network.getSocketFactory());
            }
            if (sslSocketFactory != null && trustManager != null) {
                builder.sslSocketFactory(sslSocketFactory, trustManager);
            }
            if (config.ip != null && !config.ip.equalsIgnoreCase("")) {
                builder.dns(new DetectionDns(config.ip));
            }

            client = builder.build();
            Request.Builder reqBuilder = new Request.Builder();
            reqBuilder.url(url);
            if (config.headers != null && !config.headers.isEmpty()) {
                Headers.Builder headersBuilder = new Headers.Builder();

                for(String key : config.headers.keySet()) {
                    headersBuilder.add(key, (String) Objects.requireNonNull(config.headers.get(key)));
                }

                reqBuilder.headers(headersBuilder.build());
            }

            Request request = reqBuilder.build();
            client.newCall(request).enqueue(new Callback() {
                public void onFailure(Call call, IOException e) {
                    CLSLog.e(TAG, "startHttpPing onFailure:\n" + e.getMessage() + "\n" + e.toString());
                }
                public void onResponse(Call call, Response response) throws IOException {
                    CLSLog.i(TAG, "startHttpPing code: " + response.code() + " onlyHeader " + config.downloadHeaderOnly);
                    if (config.downloadHeaderOnly) {
                        response.close();
                    } else {
                        response.peekBody((long)config.downloadBytesLimit);
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            CLSLog.e(TAG, "doDetectHttpPing error: " + e.getMessage());
        }
        return null;
    }

    private static class DetectionDns implements Dns {
        private String ip = "";

        public DetectionDns(String ip) {
            this.ip = ip;
        }

        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            InetAddress ia = InetAddress.getByName(this.ip);
            List<InetAddress> list = new ArrayList();
            list.add(ia);
            return list;
        }
    }

}
