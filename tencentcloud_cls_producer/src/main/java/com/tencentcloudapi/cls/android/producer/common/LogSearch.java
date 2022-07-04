package com.tencentcloudapi.cls.android.producer.common;

import com.tencentcloudapi.cls.android.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.android.producer.http.client.Sender;
import com.tencentcloudapi.cls.android.producer.http.comm.HttpMethod;
import com.tencentcloudapi.cls.android.producer.request.SearchLogRequest;
import com.tencentcloudapi.cls.android.producer.response.SearchLogResponse;
import com.tencentcloudapi.cls.android.producer.util.QcloudClsSignature;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;


public class LogSearch {
    public static SearchLogResponse searchLogs(AsyncProducerConfig producerConfig, SearchLogRequest request) throws LogException {
        HashMap<String, String> headParameter = new HashMap<>(2);
        headParameter.put(Constants.CONST_CONTENT_TYPE, Constants.CONST_JSON);
        headParameter.put(Constants.CONST_HOST, producerConfig.getHostName());

        String signature;
        try {
            signature = QcloudClsSignature.buildSignature(producerConfig.getSecretId(), producerConfig.getSecretKey(), HttpMethod.GET.toString(), Constants.SEARCH_LOG_RESOURCE_URI, new HashMap<>(0), headParameter, 300000);
        } catch (UnsupportedEncodingException e) {
            throw new LogException(ErrorCodes.ENCODING_EXCEPTION, e.getMessage());
        }
        headParameter.put(Constants.CONST_AUTHORIZATION, signature);

        if (!producerConfig.getSecretToken().isEmpty()) {
            headParameter.put("X-Cls-Token", producerConfig.getSecretToken());
        }

        headParameter.put("android-sdk-version", "1.0.5");

        String endPointUrl = producerConfig.getHttpType() + producerConfig.getHostName()+ Constants.SEARCH_LOG_RESOURCE_URI;
        SearchLogResponse response;
        try {
            response = Sender.doGet(request, endPointUrl, headParameter);
        } catch (Exception e) {
            e.printStackTrace();
            throw new LogException(ErrorCodes.SendFailed, e.getMessage());
        }
        return response;
    }
}
