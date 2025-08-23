package com.tencentcloudapi.cls.android;

import com.tencentcloudapi.cls.android.producer.util.Args;
import com.tencentcloudapi.cls.android.producer.util.NetworkType;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClsConfigOptions {
    /**
     * 日志服务CLS endpoint 接入域名，如：ap-guangzhou.cls.tencentcs.com
     */
    private final String endpoint;
    private final String host;

    public String getHost() {
        return host;
    }
    private final Credential credential;
    private final String topicId;

    public Credential getCredential() {
        return credential;
    }

    public String getTopicId() {
        return topicId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     */
    private int flushInterval = 5 * 1000;

    /**
     * flush日志的最大条目数
     */
    private int flushBulkSize = 50;

    /**
     * 本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     */
    private long maxCacheSize = 32 * 1024 * 1024L;

    /**
     * 是否开启打印日志
     */
    boolean mLogEnabled;

    /**
     * 网络上传策略
     */
    int mNetworkTypePolicy = NetworkType.TYPE_2G | NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI | NetworkType.TYPE_5G;

    /**
     * 设置数据的网络上传策略
     *
     * @param networkTypePolicy 数据的网络上传策略
     * @return ClsConfigOptions
     */
    public ClsConfigOptions setNetworkTypePolicy(int networkTypePolicy) {
        this.mNetworkTypePolicy = networkTypePolicy;
        return this;
    }

    /**
     * 是否打印日志
     *
     * @param enableLog 是否开启打印日志
     * @return ClsOptionsConfig
     */
    public ClsConfigOptions enableLog(boolean enableLog) {
        this.mLogEnabled = enableLog;
        return this;
    }
    public boolean isLogEnabled() {
        return mLogEnabled;
    }


    public long getMaxCacheSize() {
        return maxCacheSize;
    }
    /**
     *  设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *  Params:
     *  maxCacheSize – 单位 byte
     */
    public ClsConfigOptions setMaxCacheSize(long maxCacheSize) {
        this.maxCacheSize = Math.max(16 * 1024 * 1024, maxCacheSize);
        return this;
    }

    public int getFlushBulkSize() {
        return flushBulkSize;
    }

    public ClsConfigOptions setFlushBulkSize(int flushBulkSize) {
        this.flushBulkSize = Math.max(50, flushBulkSize);
        return this;
    }

    public int getFlushInterval() {
        return flushInterval;
    }

    /**
     * 设置两次数据发送的最小时间间隔，最小值 5 秒
     * Params:
     * flushInterval – 时间间隔，单位毫秒
     */
    public ClsConfigOptions setFlushInterval(int flushInterval) {
        this.flushInterval = Math.max(5 * 1000, flushInterval);
        return this;
    }

    /**
     * ClsConfigOptions 构造函数
     * @param endpoint tencent cloud cls endpoint
     * @param topicId tencent cloud cls 日志主题id
     * @param credential tencent cloud credential
     */
    public ClsConfigOptions(String endpoint, String topicId, Credential credential) {
        Args.notNullOrEmpty(endpoint, "endpoint");
        Args.notNullOrEmpty(topicId, "topicId");
        Args.notNull(credential, "credential");
        this.topicId = topicId;
        this.credential = credential;
        if (endpoint.startsWith("http://")) {
            this.endpoint = endpoint;
            this.host = endpoint.substring(7);
        } else if (endpoint.startsWith("https://")) {
            this.endpoint = endpoint;
            this.host = endpoint.substring(8);
        } else {
            this.host = endpoint;
            this.endpoint = "https://" + endpoint;
        }
    }

    private String appVersion = "--";
    private String appName = "--";
//    private Map<String, String> tag = new LinkedHashMap<>();
//    public void addTag(String key, String value) {
//        if (null == key) {
//            key = "null";
//        }
//        if(null == value) {
//            value = "null";
//        }
//        tag.put(key, value);
//    }
//    public Map<String, String> getTag() {
//        return tag;
//    }

    public String getAppVersion() {
        return appVersion;
    }

    public ClsConfigOptions setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public String getAppName() {
        return appName;
    }

    public ClsConfigOptions setAppName(String appName) {
        this.appName = appName;
        return this;
    }
}
