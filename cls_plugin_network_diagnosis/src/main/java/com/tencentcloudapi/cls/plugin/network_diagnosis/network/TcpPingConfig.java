package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

public class TcpPingConfig extends DetectConfig {
    public int port;
    public String payload;

    public TcpPingConfig(String taskId, String domain, int port, int maxTimes, int timeout, DetectCallback callback) {
        this.taskId = taskId;
        this.domain = domain;
        this.port = port;
        this.maxTimes = maxTimes;
        this.timeout = timeout;
        this.callback = callback;
        this.multiplePortsDetect = false;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
