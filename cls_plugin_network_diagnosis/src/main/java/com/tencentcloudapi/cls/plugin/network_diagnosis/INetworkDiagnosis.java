package com.tencentcloudapi.cls.plugin.network_diagnosis;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

public interface INetworkDiagnosis {
    int DEFAULT_PING_SIZE = 64;
    int DEFAULT_TIMEOUT = 2 * 1000;
    int DEFAULT_MAX_TIMES = 10;

    boolean DEFAULT_HTTP_HEADER_ONLY = false;
    int DEFAULT_HTTP_DOWNLOAD_BYTES_LIMIT = 64 * 1024; // 64KB

    int INVALID = -1;

    String DNS_TYPE_IPv4 = "A";
    String DNS_TYPE_IPv6 = "AAAA";

    int DEFAULT_MTR_MAX_TTL = 30;
    int DEFAULT_MTR_MAX_PATH = 1;


    enum Type {
        HTTP("http"),
        PING("ping"),
        TCPPING("tcpping"),
        UDP("udp"),
        MTR("mtr"),
        DNS("dns"),
        UNKNOWN("unknown");

        private static final Map<String, Type> ENUM_MAP = new HashMap<String, Type>(5) {
            {
                put(HTTP.value, HTTP);
                put(PING.value, PING);
                put(TCPPING.value, TCPPING);
                put(UDP.value, UDP);
                put(MTR.value, MTR);
                put(DNS.value, DNS);
            }
        };

        public final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type of(String value) {
            return ENUM_MAP.containsKey(value.toLowerCase()) ? ENUM_MAP.get(value) : UNKNOWN;
        }
    }

    class Response {
        public Type type;
        public String content;
        public String error;

        public static Response response(Type type, String content) {
            Response response = new Response();
            response.type = type;
            response.content = content;
            return response;
        }

        public static Response error(String error, Type type) {
            Response response = new Response();
            response.error = error;
            response.type = type;

            return response;
        }
    }

    interface Callback {
        void onComplete(Response response);
    }


    class HttpCredential {
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

    void updateExtensions(Map<String, String> extension);

    class Request {
        public String domain;
        public boolean multiplePortsDetect = true;
        public Map<String, String> extension;
    }

    class PingRequest extends Request {
        public int size = DEFAULT_PING_SIZE;
        public int maxTimes = DEFAULT_MAX_TIMES;
        public int timeout = DEFAULT_TIMEOUT;
    }
    void ping(PingRequest request);

    void ping(PingRequest request, Callback callback);

    class HttpRequest extends PingRequest {
        public String ip;
        public HttpCredential credential;
        public boolean headerOnly = DEFAULT_HTTP_HEADER_ONLY;
        public int downloadBytesLimit = DEFAULT_HTTP_DOWNLOAD_BYTES_LIMIT;

        public HttpRequest() {
            timeout = 15 * DEFAULT_TIMEOUT; // timeout 30s
        }
    }

    void http(HttpRequest request);

    void http(HttpRequest request, Callback callback);


    class TcpPingRequest extends PingRequest {
        public int port = INVALID;
        public String payload;
    }

    void tcpPing(TcpPingRequest request);

    void tcpPing(TcpPingRequest request, Callback callback);

    class DnsRequest extends PingRequest {
        public String type = DNS_TYPE_IPv4;
        public String nameServer;
    }

    void dns(DnsRequest request);

    void dns(DnsRequest request, Callback callback);


    class MtrRequest extends PingRequest {
        public enum Protocol {
            ALL("all"),
            ICMP("icmp"),
            UDP("udp");

            public final String protocol;

            Protocol(String protocol) {
                this.protocol = protocol;
            }
        }

        public int maxTTL = DEFAULT_MTR_MAX_TTL;
        public int maxPaths = DEFAULT_MTR_MAX_PATH;
        public Protocol protocol = Protocol.ALL;
    }

    void mtr(MtrRequest request);

    void mtr(MtrRequest request, Callback callback);
}
