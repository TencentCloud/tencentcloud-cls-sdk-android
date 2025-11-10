package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

public class DnsConfig extends DetectConfig {
    public String server;
    public String type;

    public DnsConfig(String taskId, String nameServer, String domain, String type, int timeout, DetectCallback callback, Object context) {
        this.taskId = taskId;
        this.domain = domain;
        this.timeout = timeout;
        this.callback = callback;
        this.server = nameServer;
        this.type = type;
        this.multiplePortsDetect = false;
    }
}

