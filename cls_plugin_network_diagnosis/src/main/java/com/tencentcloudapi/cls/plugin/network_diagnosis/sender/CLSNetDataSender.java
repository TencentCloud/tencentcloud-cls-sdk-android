package com.tencentcloudapi.cls.plugin.network_diagnosis.sender;

import android.text.TextUtils;
import android.util.TimeUtils;

import com.tencentcloudapi.cls.android.CLSConfig;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.plugin.ISender;
import com.tencentcloudapi.cls.android.producer.AsyncProducerClient;
import com.tencentcloudapi.cls.android.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.android.producer.common.LogContent;
import com.tencentcloudapi.cls.android.producer.common.LogItem;
import com.tencentcloudapi.cls.android.producer.errors.ProducerException;
import com.tencentcloudapi.cls.android.producer.util.NetworkUtils;
import com.tencentcloudapi.cls.android.scheme.Scheme;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CLSNetDataSender implements ISender {
    private static final String TAG = "CLSNetDataSender";

    private AsyncProducerConfig producerConfig;
    private AsyncProducerClient producerClient;
    private CLSConfig clsConfig;

    @Override
    public void init(CLSConfig config) {
        this.clsConfig = config;

        if (config.debuggable) {
            CLSLog.v(TAG, CLSLog.format("init, topic_id: %s", this.clsConfig.topicId));
        }

        producerConfig = new AsyncProducerConfig(config.context
                    , config.endpoint
                    , config.accessKeyId
                    , config.accessKeySecret
                    , config.securityToken
                    , NetworkUtils.getLocalMachineIP()
        );

        // 发送线程数，默认为1
        producerConfig.setSendThreadCount(1);
        producerConfig.setRetries(0);

        final File rootPath = new File(new File(config.context.getFilesDir(), "cls_network_monitor"), "cls_logs");
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }

        producerClient = new AsyncProducerClient(producerConfig);
        if (config.debuggable) {
            CLSLog.v(TAG, "init success.");
        }
    }

    @Override
    public boolean send(Scheme data) {
        if (null == producerClient) {
            CLSLog.e(TAG, "LogProducerClient is not init or exception caused.");
            return false;
        }

        if(null == data) {
            CLSLog.e(TAG, "TCData must not be null.");
            return false;
        }
        List<LogItem> logItems = new ArrayList<>();
        int ts = (int) (System.currentTimeMillis() / 1000);
        LogItem logItem = new LogItem(ts);
        for (Map.Entry<String,String> entry : data.toMap().entrySet()) {
            logItem.PushBack(new LogContent(entry.getKey(), entry.getValue()));
        }
        logItems.add(logItem);
        try {
            producerClient.putLogs(this.clsConfig.topicId, logItems, result -> System.out.println(result.toString()));
            if (clsConfig.debuggable) {
                CLSLog.v(TAG, "add log success.");
            }
        } catch (ProducerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken) {

    }

    @Override
    public void resetProject(String endpoint, String project, String logstore) {

    }
}
