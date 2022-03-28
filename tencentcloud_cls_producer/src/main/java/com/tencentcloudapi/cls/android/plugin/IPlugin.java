package com.tencentcloudapi.cls.android.plugin;

import com.tencentcloudapi.cls.android.CLSConfig;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public interface IPlugin {

    String name();

    String version();

    void init(CLSConfig config);

    void setDebuggable(boolean debuggable);

    void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken);

    void resetTopicID(String endpoint, String topicId);

    void updateConfig(CLSConfig config);
}
