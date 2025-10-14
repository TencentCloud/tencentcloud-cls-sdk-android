package com.tencentcloudapi.cls.plugin.network_diagnosis;

import android.content.Context;

import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.plugin.AbstractPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class CLSNetDiagnosisPlugin extends AbstractPlugin {
    private static final String TAG = "CLSNetDiagnosisPlugin";

    @Override
    public String name() {
        return "network_diagnosis";
    }

    @Override
    public String version() {
        return "2.0.0";
    }

    Map<String, String> ext = new LinkedHashMap<>();
    Map<String, String> getExt() {
        return ext;
    }
    private String reportTopicId = "";
    @Override
    public void setReportTopicId(String reportTopicId) {
        this.reportTopicId = reportTopicId;
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
    public void init(Context context, ClsConfigOptions config) {
        CLSNetDiagnosis.getInstance().init(context, config, getExt(), reportTopicId);
    }
}

