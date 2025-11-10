package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.annotation.SuppressLint;
import android.net.Network;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.ClsDataAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class HttpEventListener extends EventListener {
    private static final String TAG = HttpEventListener.class.getCanonicalName();
    static final AtomicLong nextCallId = new AtomicLong(1L);
    HttpConfig config;
    JSONObject result;
    final long callId;
    final long callStartMillis;
    long lastMillis;
    JSONObject desc;
    String taskId;
    String key;

    public HttpEventListener(HttpConfig config, JSONObject netInfo) {
        this.config = config;
        this.callId = nextCallId.getAndIncrement();
        this.callStartMillis = System.currentTimeMillis();
        this.result = new JSONObject();
        this.desc = new JSONObject();
        try {
            this.result.put("url", this.config.url);
            this.result.put("startDate", this.callStartMillis);
            this.result.put("netInfo", netInfo);
            this.taskId = config.taskId;
            this.key = config.key;
            this.result.put("src", "app");
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }
    }

    @SuppressLint("SimpleDateFormat")
    private String timestampToString(long ts) {
        return (new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS")).format(new Date(ts));
    }

    public void callStart(Call call) {
        super.callStart(call);
        long now = System.currentTimeMillis();
        try {
            this.desc.put("callStart", this.timestampToString(now));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }
        CLSLog.d(TAG, "callStart: waitStart " + (now - this.callStartMillis));
        this.lastMillis = now;
    }

    public void dnsStart(Call call, String domainName) {
        super.dnsStart(call, domainName);
        long now = System.currentTimeMillis();
        try {
            CLSLog.d(TAG, "dnsStart: waitDns " + (now - this.lastMillis) + " for: " + this.result.getString("url"));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }
        try {
            this.result.put("waitDnsTime", now - this.lastMillis);
            this.desc.put("dnsStart", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }
        this.lastMillis = now;
    }

    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        super.dnsEnd(call, domainName, inetAddressList);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "dnsEnd: dns " + (now - this.lastMillis));
        try {
            this.result.put("dnsTime", now - this.lastMillis);
            this.desc.put("dnsEnd", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }
        this.lastMillis = now;
    }

    @SuppressLint({"NewApi"})
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        super.connectStart(call, inetSocketAddress, proxy);
        long now = System.currentTimeMillis();
        String domain = inetSocketAddress.getHostName();
        String address = inetSocketAddress.getAddress().getHostAddress();
        try {
            this.result.put("domain", domain);
            this.result.put("remoteAddr", address);
            this.desc.put("connectStart", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG,  e.getMessage());
        }

        CLSLog.d(TAG, "connectStart: address " + address + ", waitConnect " + (now - this.lastMillis));
        this.lastMillis = now;
    }

    public void secureConnectStart(Call call) {
        super.secureConnectStart(call);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "secureConnectStart: tcpTime " + (now - this.lastMillis));

        try {
            this.result.put("tcpTime", now - this.lastMillis);
            this.desc.put("secureConnectStart", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }

        this.lastMillis = now;
    }

    public void secureConnectEnd(Call call, Handshake handshake) {
        super.secureConnectEnd(call, handshake);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "secureConnectEnd: sslTime " + (now - this.lastMillis));

        try {
            this.result.put("sslTime", now - this.lastMillis);
            this.desc.put("secureConnectEnd", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG,  e.getMessage());
        }

        this.lastMillis = now;
    }

    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        super.connectEnd(call, inetSocketAddress, proxy, protocol);
        long now = System.currentTimeMillis();

        try {
            this.desc.put("connectEnd", this.timestampToString(now));
            if (!this.result.has("sslTime") || this.result.getLong("sslTime") <= 0L) {
                this.result.put("tcpTime", now - this.lastMillis);
                CLSLog.d(TAG, "connectEnd: tcpTime " + (now - this.lastMillis));
            }
        } catch (Throwable e) {
            CLSLog.e(TAG, "connectEnd exception: " + e.getMessage());
        }

        this.lastMillis = now;
    }

    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "connectFailed: " + now);
        try {
            this.result.put("errCode", -10002);
            this.result.put("errMessage", ioe.getMessage());
            this.desc.put("connectFailed", this.timestampToString(now));
            this.result.put("desc", this.desc);
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }
        config.callback.onComplete(this.result);
    }

    public void connectionAcquired(Call call, Connection connection) {
        super.connectionAcquired(call, connection);
        long now = System.currentTimeMillis();

        try {
            this.desc.put("connectionAcquired", this.timestampToString(now));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }

        this.lastMillis = now;
    }

    public void connectionReleased(Call call, Connection connection) {
        super.connectionReleased(call, connection);
        long now = System.currentTimeMillis();

        try {
            this.desc.put("connectionReleased", this.timestampToString(now));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }
    }

    public void requestHeadersStart(Call call) {
        super.requestHeadersStart(call);
        long now = System.currentTimeMillis();
        this.lastMillis = now;

        try {
            this.desc.put("requestHeaderStart", this.timestampToString(now));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }

    }

    public void requestHeadersEnd(Call call, Request request) {
        super.requestHeadersEnd(call, request);
        long now = System.currentTimeMillis();
        this.lastMillis = now;

        try {
            this.result.put("sendBytes", request.headers().byteCount());
            this.desc.put("requestHeaderEnd", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }

    }


    public void requestBodyStart(Call call) {
        super.requestBodyStart(call);
        long now = System.currentTimeMillis();

        try {
            this.desc.put("requestBodyStart", this.timestampToString(now));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }

    }

    public void requestBodyEnd(Call call, long byteCount) {
        super.requestBodyEnd(call, byteCount);
        long now = System.currentTimeMillis();
        try {
            int count = this.result.getInt("sendBytes");
            this.result.put("sendBytes", byteCount + (long)count);
            this.desc.put("requestBodyEnd", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG,  e.getMessage());
        }
    }

    public void responseHeadersStart(Call call) {
        super.responseHeadersStart(call);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "responseHeadersStart: firstByteTime " + (now - this.lastMillis));

        try {
            this.desc.put("responseHeadersStart", this.timestampToString(now));
            this.result.put("firstByteTime", now - this.lastMillis);
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }

    }

    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
        long now = System.currentTimeMillis();

        try {
            this.result.put("httpCode", response.code());
            this.result.put("httpProtocol", response.protocol());
            this.desc.put("responseHeaderEnd", this.timestampToString(now));
            this.result.put("receiveBytes", 0);

            try {
                JSONObject headers = new JSONObject();
                Headers h = response.headers();

                for(String name : h.names()) {
                    headers.put(name, h.get(name));
                }

                this.result.put("headers", headers);
            } catch (Throwable e) {
                CLSLog.e(TAG,  e.getMessage());
            }

            if (response.isRedirect()) {
                CLSLog.d(TAG, "responseHeadersEnd: redirect: " + response.headers().toString());
                this.result.put("rdr-location", response.header("location"));
                this.result.put("desc", this.desc);
                config.callback.onComplete(this.result);
            }
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }

    }

    public void responseBodyStart(Call call) {
        super.responseBodyStart(call);
        long now = System.currentTimeMillis();

        try {
            this.desc.put("responseBodyStart", this.timestampToString(now));
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }

    }

    public void responseBodyEnd(Call call, long byteCount) {
        super.responseBodyEnd(call, byteCount);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "responseBodyEnd: byteCount " + byteCount + ", allDownloadTime " + (now - this.lastMillis));

        try {
            this.result.put("allByteTime", now - this.lastMillis);
            this.result.put("receiveBytes", byteCount);
            this.result.put("bandwidth", (double)((float)byteCount * 1000.0F / (float)(now - this.lastMillis)));
            this.desc.put("responseBodyEnd", this.timestampToString(now));
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
        }

        this.lastMillis = now;
    }

    public void callEnd(Call call) {
        super.callEnd(call);
        long now = System.currentTimeMillis();
        CLSLog.d(TAG, "callEnd: allTime " + (now - this.callStartMillis));

        try {
            this.result.put("requestTime", now - this.callStartMillis);
            this.desc.put("callEnd", this.timestampToString(now));
            this.result.put("desc", this.desc);
        } catch (Throwable e) {
            CLSLog.e(TAG,  e.getMessage());
        }
        config.callback.onComplete(this.result);
        this.lastMillis = now;
    }

    public void callFailed(Call call, IOException ioe) {
        super.callFailed(call, ioe);
        long now = System.currentTimeMillis();
        try {
            this.result.put("errCode", -10001);
            this.result.put("errMessage", ioe.getMessage());
            this.desc.put("callFailed", this.timestampToString(now));
            this.result.put("desc", this.desc);
        } catch (Throwable e) {
            CLSLog.e(TAG,  e.getMessage());
        }
        config.callback.onComplete(this.result);
        CLSLog.d(TAG, "callFailed: " + ioe.getMessage());
    }
}
