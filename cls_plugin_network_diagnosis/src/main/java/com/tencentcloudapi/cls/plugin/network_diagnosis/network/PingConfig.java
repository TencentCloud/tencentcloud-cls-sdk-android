package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

public class PingConfig extends DetectConfig {
    private int size;

    public int getSize() {
        return this.size;
    }

    public PingConfig(String taskId, String domain, int maxTimes, int timeout, DetectCallback callback) {
        this.taskId = taskId;
        this.domain = domain;
        this.maxTimes = maxTimes;
        this.timeout = timeout;
        this.callback = callback;
        this.multiplePortsDetect = false;
        this.size = 64;
    }

    public PingConfig(String taskId, String domain, int size, int maxTimes, int timeout, DetectCallback callback) {
        this.taskId = taskId;
        this.domain = domain;
        this.maxTimes = maxTimes;
        this.timeout = timeout;
        this.callback = callback;
        this.multiplePortsDetect = false;
        this.size = size;
    }
}

