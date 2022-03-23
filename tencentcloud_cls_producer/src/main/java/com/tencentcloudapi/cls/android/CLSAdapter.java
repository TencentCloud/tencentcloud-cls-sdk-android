package com.tencentcloudapi.cls.android;

import java.util.ArrayList;
import java.util.List;

import com.tencentcloudapi.cls.android.plugin.IPlugin;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public class CLSAdapter {
    private static final String TAG = "CLSAdapter";

    private String channel;
    private String channelName;
    private String userNick;
    private String longLoginNick;
    private String loginType;

    private List<IPlugin> plugins = new ArrayList<>();

    private CLSAdapter() {
        //no instance
    }

    public static CLSAdapter getInstance() {
        return Holder.INSTANCE;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public void setUserNick(String userNick) {
        this.userNick = userNick;
    }

    public void setLongLoginNick(String longLoginNick) {
        this.longLoginNick = longLoginNick;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public void init(final CLSConfig config) {
        if (config.debuggable) {
            CLSLog.v(TAG, "init, start.");
        }

        if (!checkConfig(config)) {
            return;
        }

        for (IPlugin plugin : plugins) {
            if (config.debuggable) {
                CLSLog.v(TAG, CLSLog.format("init plugin %s start. plugin: ", plugin.name()));
            }

            plugin.init(config);
            // add plugin version to user-agent
//            HttpConfigProxy.addPluginUserAgent(plugin.name(), plugin.version());

            if (config.debuggable) {
                CLSLog.v(TAG, CLSLog.format("init plugin %s end. plugin: ", plugin.name()));
            }
        }

        if (config.debuggable) {
            CLSLog.v(TAG, "init, end.");
        }
    }

    public void updateConfig(CLSConfig config) {
        for (IPlugin plugin : plugins) {
            plugin.updateConfig(config);
        }
    }

    public void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken) {
        for (IPlugin plugin : plugins) {
            plugin.resetSecurityToken(accessKeyId, accessKeySecret, securityToken);
        }
    }

    public void resetTopicID(String endpoint, String topicId) {
        for (IPlugin plugin : plugins) {
            plugin.resetTopicID(endpoint, topicId);
        }
    }

    public CLSAdapter addPlugin(IPlugin plugin) {
        if (null == plugin) {
            throw new IllegalArgumentException("plugin must not be null");
        }

        this.plugins.add(plugin);
        return this;
    }

    private boolean checkConfig(CLSConfig config) {
        if (null == config) {
            throw new IllegalArgumentException("CLSConfig must not be null.");
        }

        if (null == config.context) {
            throw new IllegalArgumentException("CLSConfig.context must not be null.");
        }

        return true;
    }

    private static class Holder {
        private final static CLSAdapter INSTANCE = new CLSAdapter();
    }
}
