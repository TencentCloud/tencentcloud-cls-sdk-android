package com.tencentcloudapi.cls.plugin;

import com.tencentcloudapi.cls.CLSConfig;

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

    void resetProject(String endpoint, String topicId);

    void updateConfig(CLSConfig config);
}
