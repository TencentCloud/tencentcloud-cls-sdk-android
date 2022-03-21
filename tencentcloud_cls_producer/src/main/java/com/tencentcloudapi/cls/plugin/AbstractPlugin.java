package com.tencentcloudapi.cls.plugin;

import com.tencentcloudapi.cls.CLSConfig;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public abstract class AbstractPlugin implements IPlugin {
    protected boolean debuggable = false;

    @Override
    public void setDebuggable(boolean debuggable) {
        this.debuggable = debuggable;
    }

    @Override
    public void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken) {

    }

    @Override
    public void resetProject(String endpoint, String topicId) {

    }

    @Override
    public void updateConfig(CLSConfig config) {

    }
}
