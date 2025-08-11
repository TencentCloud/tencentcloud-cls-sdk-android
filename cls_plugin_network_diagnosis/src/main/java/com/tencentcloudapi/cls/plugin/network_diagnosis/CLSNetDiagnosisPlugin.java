package com.tencentcloudapi.cls.plugin.network_diagnosis;


import com.tencentcloudapi.cls.android.CLSConfig;
import com.tencentcloudapi.cls.android.plugin.AbstractPlugin;
import com.tencentcloudapi.cls.android.plugin.ISender;
import com.tencentcloudapi.cls.plugin.network_diagnosis.sender.CLSNetDataSender;

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
    public void init(CLSConfig config) {
        ISender sender = new CLSNetDataSender();
        sender.init(config);
        CLSNetDiagnosis.getInstance().init(config, sender);
    }

    @Override
    public void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken) {
        CLSNetDiagnosis.getInstance().resetSecurityToken(accessKeyId, accessKeySecret, securityToken);
    }

    @Override
    public void resetTopicID(String endpoint, String topicId) {
        CLSNetDiagnosis.getInstance().resetTopicID(endpoint, topicId);
    }

}

