package com.tencentcloudapi.cls.plugin.network_diagnosis;

import android.content.Context;

import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.plugin.AbstractPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class NetworkDiagnosisPlugin extends AbstractPlugin {
    private static final String TAG = "CLSNetDiagnosisPlugin";

    @Override
    public String name() {
        return "network_diagnosis";
    }

    @Override
    public String version() {
        return "3.0.0";
    }

    Map<String, String> ext = new LinkedHashMap<>();
    Map<String, String> getExt() {
        return ext;
    }

    @Override
    public void addCustomField(String key, String value) {
        if (null == key) {
            key = "null";
        }
        if(null == value) {
            value = "null";
        }
        ext.put(key, value);
    }

    @Override
    public void setReportTopicId(String reportTopicId) {

    }

    @Override
    public void init(Context context, ClsConfigOptions config) {
        NetworkDiagnosis networkDiagnosis = new NetworkDiagnosis();
        networkDiagnosis.onPreInit(context, config, getExt());
    }
}
