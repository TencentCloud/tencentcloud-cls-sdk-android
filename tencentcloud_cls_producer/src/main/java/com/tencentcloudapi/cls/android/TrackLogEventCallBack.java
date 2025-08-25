package com.tencentcloudapi.cls.android;

public interface TrackLogEventCallBack {
    public static final int SUCCESS = 0;
    public static final int FAIL = 1;
    void onCompletion(Integer status, String eventMessage);
}
