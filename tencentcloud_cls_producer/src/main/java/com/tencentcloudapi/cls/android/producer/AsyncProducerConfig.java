package com.tencentcloudapi.cls.android.producer;

import android.content.Context;

import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.util.Args;
import com.tencentcloudapi.cls.android.producer.util.NetworkUtils;

import javax.annotation.Nonnull;


/**
 * @author farmerx
 */
public class AsyncProducerConfig {

    private String httpType;

    private String hostName;

    private String secretId;

    private String secretKey;

    private String secretToken;

    private String sourceIp;

    private String topicId;

    private final Context context;

    private int totalSizeInBytes = Constants.DEFAULT_TOTAL_SIZE_IN_BYTES;

    private long maxBlockMs = Constants.DEFAULT_MAX_BLOCK_MS;

    private int sendThreadCount = Constants.DEFAULT_SEND_THREAD_COUNT;

    private int batchSizeThresholdInBytes = Constants.DEFAULT_BATCH_SIZE_THRESHOLD_IN_BYTES;

    private int batchCountThreshold = Constants.DEFAULT_BATCH_COUNT_THRESHOLD;

    private int lingerMs = Constants.DEFAULT_LINGER_MS;

    private int retries = Constants.DEFAULT_RETRIES;

    private int maxReservedAttempts = Constants.DEFAULT_RETRIES + 1;

    private long baseRetryBackoffMs = Constants.DEFAULT_BASE_RETRY_BACKOFF_MS;

    private long maxRetryBackoffMs = Constants.DEFAULT_MAX_RETRY_BACKOFF_MS;

    /**
     * New Async Client Config
     * @param endpoint tencent cloud cls endpoint
     * @param secretId tencent cloud secretId
     * @param secretKey tencent cloud secretKey
     * @param sourceIp 本机ip，
     */
    public AsyncProducerConfig(Context context, @Nonnull String endpoint, @Nonnull String secretId, @Nonnull String secretKey, String secretToken, String sourceIp) {
        Args.notNullOrEmpty(endpoint, "endpoint");
        Args.notNullOrEmpty(secretId, "secretId");
        Args.notNullOrEmpty(secretKey, "secretKey");
        if (endpoint.startsWith("http://")) {
            this.hostName = endpoint.substring(7);
            this.httpType = "http://";
        } else if (endpoint.startsWith("https://")) {
            this.hostName = endpoint.substring(8);
            this.httpType = "https://";
        } else {
            this.hostName = endpoint;
            this.httpType = "http://";
        }
        while (this.hostName.endsWith("/")) {
            this.hostName = this.hostName.substring(0, this.hostName.length() - 1);
        }
        if (NetworkUtils.isIPAddr(this.hostName)) {
            throw new IllegalArgumentException("EndpointInvalid", new Exception("The ip address is not supported"));
        }

        this.secretId = secretId;
        this.secretKey = secretKey;
        this.sourceIp = sourceIp;
        this.context = context;
//        if (null == sourceIp  || sourceIp.isEmpty()) {
//            this.sourceIp = NetworkUtils.getLocalMachineIP();
//        }

        this.secretToken = secretToken;
        if (null == this.secretToken || this.secretToken.isEmpty()) {
            this.secretToken = "";
        }
    }


    /**
     * 获取Http Type
     * @return
     */
    public String getHttpType() {
        return this.httpType;
    }

    /**
     * 获取Host Name
     * @return
     */
    public String getHostName() {
        return this.hostName;
    }

    /**
     * 获取Tencent Cloud Secret Id
     * @return
     */
    public String getSecretId() {
        return this.secretId;
    }

    /**
     * 获取Tencent Cloud Secret Key
     * @return
     */
    public String getSecretKey() {
        return this.secretKey;
    }

    /**
     * 获取Tencent Cloud Secret Token
     * @return
     */
    public String getSecretToken() {
        return this.secretToken;
    }
    /**
     * 获取本机Ip， 可以是自己设置的
     * @return
     */
    public String getSourceIp() {
        return this.sourceIp;
    }

    public String getTopicId() {
        return this.topicId;
    }


    /**
     * 获取服务接收的最大报文大小
     *
     * @return int
     */
    public int getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    /**
     * 设置生产者可用于缓冲等待发送到服务器的日志的内存总字节数
     *
     * @param totalSizeInBytes
     */
    public void setTotalSizeInBytes(int totalSizeInBytes) {
        if (totalSizeInBytes <= 0) {
            throw new IllegalArgumentException("totalSizeInBytes must be greater than 0, got " + totalSizeInBytes);
        }
        this.totalSizeInBytes = totalSizeInBytes;
    }

    /**
     * @param accessKeyId
     * @param accessKeySecret
     * @param securityToken
     */
    public void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken) {
        Args.notNullOrEmpty(accessKeyId, "secretId");
        Args.notNullOrEmpty(accessKeySecret, "secretKey");
        this.secretId = accessKeyId;
        this.secretKey = accessKeySecret;
        this.secretToken = securityToken;
        if (null == this.secretToken || this.secretToken.isEmpty()) {
            this.secretToken = "";
        }
    }

    public void resetTopicID(String endpoint, String topicId) {
        Args.notNullOrEmpty(endpoint, "endpoint");
        Args.notNullOrEmpty(topicId, "topicId");
        if (endpoint.startsWith("http://")) {
            this.hostName = endpoint.substring(7);
            this.httpType = "http://";
        } else if (endpoint.startsWith("https://")) {
            this.hostName = endpoint.substring(8);
            this.httpType = "https://";
        } else {
            this.hostName = endpoint;
            this.httpType = "http://";
        }
        while (this.hostName.endsWith("/")) {
            this.hostName = this.hostName.substring(0, this.hostName.length() - 1);
        }
        if (NetworkUtils.isIPAddr(this.hostName)) {
            throw new IllegalArgumentException("EndpointInvalid", new Exception("The ip address is not supported"));
        }
        this.topicId = topicId;
    }


    /**
     * 获取日志合并最大等待时间
     *
     * @return
     */
    public long getMaxBlockMs() {
        return maxBlockMs;
    }

    /**
     * @param maxBlockMs
     */
    public void setMaxBlockMs(long maxBlockMs) {
        this.maxBlockMs = maxBlockMs;
    }

    /**
     * 获取最大发送线程数
     *
     * @return sendThreadCount
     */
    public int getSendThreadCount() {
        return sendThreadCount;
    }

    /**
     * 设置最大发送并发线程数
     * @param sendThreadCount
     */
    public void setSendThreadCount(int sendThreadCount) {
        if (sendThreadCount <= 0) {
            throw new IllegalArgumentException("sendThreadCount must be greater than 0, got " + sendThreadCount);
        }
        this.sendThreadCount = sendThreadCount;
    }

    public int getBatchSizeThresholdInBytes() {
        return batchSizeThresholdInBytes;
    }

    /**
     * 设置每次大小的阀值
     * @param batchSizeThresholdInBytes
     */
    public void setBatchSizeThresholdInBytes(int batchSizeThresholdInBytes) {
        if (batchSizeThresholdInBytes < 1 || batchSizeThresholdInBytes > Constants.MAX_BATCH_SIZE_IN_BYTES) {
            throw new IllegalArgumentException(
                    String.format(
                            "batchSizeThresholdInBytes must be between 1 and %d, got %d",
                            Constants.MAX_BATCH_SIZE_IN_BYTES, batchSizeThresholdInBytes));
        }
        this.batchSizeThresholdInBytes = batchSizeThresholdInBytes;
    }

    public int getBatchCountThreshold() {
        return batchCountThreshold;
    }

    public void setBatchCountThreshold(int batchCountThreshold) {
        if (batchCountThreshold < 1 || batchCountThreshold > Constants.MAX_BATCH_COUNT) {
            throw new IllegalArgumentException(
                    String.format(
                            "batchCountThreshold must be between 1 and %d, got %d",
                            Constants.MAX_BATCH_COUNT, batchCountThreshold));
        }
        this.batchCountThreshold = batchCountThreshold;
    }

    public int getLingerMs() {
        return lingerMs;
    }

    public void setLingerMs(int lingerMs) {
        if (lingerMs < Constants.LINGER_MS_LOWER_LIMIT) {
            throw new IllegalArgumentException(
                    String.format(
                            "lingerMs must be greater than or equal to %d, got %d",
                            Constants.LINGER_MS_LOWER_LIMIT, lingerMs));
        }
        this.lingerMs = lingerMs;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getMaxReservedAttempts() {
        return maxReservedAttempts;
    }

    public void setMaxReservedAttempts(int maxReservedAttempts) {
        if (maxReservedAttempts <= 0) {
            throw new IllegalArgumentException(
                    "maxReservedAttempts must be greater than 0, got " + maxReservedAttempts);
        }
        this.maxReservedAttempts = maxReservedAttempts;
    }

    public long getBaseRetryBackoffMs() {
        return baseRetryBackoffMs;
    }

    public void setBaseRetryBackoffMs(long baseRetryBackoffMs) {
        if (baseRetryBackoffMs <= 0) {
            throw new IllegalArgumentException(
                    "baseRetryBackoffMs must be greater than 0, got " + baseRetryBackoffMs);
        }
        this.baseRetryBackoffMs = baseRetryBackoffMs;
    }

    public long getMaxRetryBackoffMs() {
        return maxRetryBackoffMs;
    }

    public void setMaxRetryBackoffMs(long maxRetryBackoffMs) {
        if (maxRetryBackoffMs <= 0) {
            throw new IllegalArgumentException(
                    "maxRetryBackoffMs must be greater than 0, got " + maxRetryBackoffMs);
        }
        this.maxRetryBackoffMs = maxRetryBackoffMs;
    }
}
