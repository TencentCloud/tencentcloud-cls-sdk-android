package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import java.util.Map;

public class HttpConfig extends DetectConfig {
    private static final int DEFAULT_TIMEOUT_MILLISECONDS = 30000;
    private static final int DEFAULT_DOWNLOAD_SIZE = 65536;
    public String url;
    public String ip;
    public String key;
    public String connectionType;
    public HttpCredential httpCredential;
    public int timeout;
    public int downloadBytesLimit;
    public boolean downloadHeaderOnly;
    public Map<String, String> headers;

    public HttpConfig(String taskId, String url, DetectCallback callback) {
        this.taskId = taskId;
        this.url = url;
        this.callback = callback;
        this.multiplePortsDetect = false;
        this.timeout = 30000;
        this.downloadBytesLimit = 65536;
        this.downloadHeaderOnly = false;
    }

    public HttpConfig(String taskId, String url, String ip, DetectCallback callback) {
        this.taskId = taskId;
        this.url = url;
        this.ip = ip;
        this.callback = callback;
        this.multiplePortsDetect = false;
        this.timeout = 30000;
        this.downloadBytesLimit = 65536;
        this.downloadHeaderOnly = false;
    }

    public HttpConfig(String taskId, String url, String ip, HttpCredential httpCredential, DetectCallback callback) {
        this.taskId = taskId;
        this.url = url;
        this.ip = ip;
        this.httpCredential = httpCredential;
        this.callback = callback;
        this.multiplePortsDetect = false;
        this.timeout = 30000;
        this.downloadBytesLimit = 65536;
        this.downloadHeaderOnly = false;
    }

    public HttpConfig(String taskId, String url, String ip, int timeout, int downloadBytesLimit, boolean headerOnly, HttpCredential httpCredential, DetectCallback callback) {
        this.taskId = taskId;
        this.url = url;
        this.ip = ip;
        this.httpCredential = httpCredential;
        this.callback = callback;
        this.multiplePortsDetect = false;
        if (timeout > 0) {
            this.timeout = timeout;
        } else {
            this.timeout = 30000;
        }
        if (downloadBytesLimit > 0) {
            this.downloadBytesLimit = downloadBytesLimit;
        } else {
            this.downloadBytesLimit = 65536;
        }
        this.downloadHeaderOnly = headerOnly;
    }
}