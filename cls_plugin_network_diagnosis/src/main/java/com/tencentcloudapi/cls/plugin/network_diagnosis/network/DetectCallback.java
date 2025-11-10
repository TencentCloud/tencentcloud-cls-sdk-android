package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import org.json.JSONObject;

public interface DetectCallback {
    void onComplete(JSONObject result);
}
