package com.tencentcloudapi.cls.plugin.network_diagnosis.network;


public class DetectConfig {
    public String taskId;
    public String domain;
    public int maxTimes;
    public int timeout;
    public int interval;
    public DetectCallback callback;
    public boolean multiplePortsDetect;
    public void setMultiplePortsDetect(boolean on) {
        this.multiplePortsDetect = on;
    }
}


