package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

public class MtrConfig extends DetectConfig {
    public static final String MTR_PROTOCOL_ALL = "all";
    public static final String MTR_PROTOCOL_ICMP = "icmp";
    public static final String MTR_PROTOCOL_UDP = "udp";
    public int maxTtl;
    public int maxPaths;
    public String protocol;

    public MtrConfig(String taskId, String domain, int maxTtl, int maxPaths, int maxTimes, int timeout, DetectCallback callback, Object context) {
        this.taskId = taskId;
        this.domain = domain;
        this.maxTtl = maxTtl;
        this.maxPaths = maxPaths;
        this.maxTimes = maxTimes;
        this.timeout = timeout;
        this.callback = callback;
        this.multiplePortsDetect = false;
        this.protocol = "all";
    }
}
