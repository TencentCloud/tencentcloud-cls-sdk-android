package com.tencentcloudapi.cls.plugin.network_diagnosis;

import android.content.Context;

import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.plugin.AbstractPlugin;

import java.util.Map;

public class CLSNetDiagnosisPlugin extends AbstractPlugin {
    private static final String TAG = "CLSNetDiagnosisPlugin";

    @Override
    public String name() {
        return "network_diagnosis";
    }

    @Override
    public String version() {
        return "1.0.15";
    }

    @Override
    public void init(Context context, ClsConfigOptions config, Map<String, String> ext) {
        CLSNetDiagnosis.getInstance().init(context, config, ext);
    }
}

