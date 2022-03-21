package com.tencentcloudapi.cls.plugin.network_diagnosis;


import com.tencentcloudapi.cls.CLSConfig;
import com.tencentcloudapi.cls.plugin.AbstractPlugin;
import com.tencentcloudapi.cls.plugin.ISender;
import com.tencentcloudapi.cls.plugin.network_diagnosis.sender.CLSNetDataSender;

public class CLSNetDiagnosisPlugin extends AbstractPlugin {
    private static final String TAG = "CLSNetDiagnosisPlugin";

    @Override
    public String name() {
        return "network_diagnosis";
    }

    @Override
    public String version() {
        return BuildConfig.LIBRARY_PACKAGE_NAME;
    }

    @Override
    public void init(CLSConfig config) {
        ISender sender = new CLSNetDataSender();
        sender.init(config);
        CLSNetDiagnosis.getInstance().init(config, sender);
    }
}

