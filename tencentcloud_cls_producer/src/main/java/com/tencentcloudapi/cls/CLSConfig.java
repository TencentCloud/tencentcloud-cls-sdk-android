package com.tencentcloudapi.cls;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import com.tencentcloudapi.cls.scheme.AppUtils;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public final class CLSConfig {

    public boolean debuggable = false;
    public String appVersion = "--";
    public String appName = "--";

    public Context context;

    public String endpoint;
    public String accessKeyId;
    public String accessKeySecret;
    public String securityToken;
    public String pluginAppId;
    public String topicId;

    public String channel;
    public String channelName;
    public String userNick;
    public String longLoginNick;
    public String userId;
    public String longLoginUserId;
    public String loginType;

    private Map<String, String> ext = new LinkedHashMap<>();

    public CLSConfig(Context context) {
        this.context = context;
        this.appVersion = AppUtils.getAppVersion(context);
        this.appName = AppUtils.getAppName(context);
    }

    public void addCustom(String key, String value) {
        if (null == key) {
            key = "null";
        }
        if(null == value) {
            value = "null";
        }

        ext.put(key, value);
    }

    public Map<String, String> getExt() {
        return ext;
    }
}
