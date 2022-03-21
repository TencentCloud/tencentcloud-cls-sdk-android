package com.tencentcloudapi.cls.plugin;

import com.tencentcloudapi.cls.CLSConfig;
import com.tencentcloudapi.cls.producer.errors.ProducerException;
import com.tencentcloudapi.cls.scheme.Scheme;

public interface ISender {

    void init(CLSConfig config);

    /**
     * send report data to remote server.
     */
    boolean send(Scheme data);

    /**
     * reset security token
     * @param accessKeyId accessKeyId
     * @param accessKeySecret accessKeySecret
     * @param securityToken securityToken
     */
    void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken);

    /**
     * reset project configuration
     * @param endpoint endpoint of this project. should start with 'https://' prefix
     * @param project project name
     * @param logstore logstore name
     */
    void resetProject(String endpoint, String project, String logstore);
}
