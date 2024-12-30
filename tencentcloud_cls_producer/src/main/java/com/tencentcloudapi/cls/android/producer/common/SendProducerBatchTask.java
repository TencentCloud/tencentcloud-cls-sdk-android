package com.tencentcloudapi.cls.android.producer.common;

import com.google.common.math.LongMath;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.cls.Cls;
import com.tencentcloudapi.cls.android.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.android.producer.http.client.Sender;
import com.tencentcloudapi.cls.android.producer.http.comm.HttpMethod;
import com.tencentcloudapi.cls.android.producer.http.comm.RequestMessage;
import com.tencentcloudapi.cls.android.producer.request.PutLogsRequest;
import com.tencentcloudapi.cls.android.producer.response.PutLogsResponse;
import com.tencentcloudapi.cls.android.producer.util.LZ4Encoder;
import com.tencentcloudapi.cls.android.producer.util.QcloudClsSignature;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SendProducerBatchTask implements Runnable {

    private final ProducerBatch batch;

    private final AsyncProducerConfig producerConfig;

    private final RetryQueue retryQueue;

    private final BlockingQueue<ProducerBatch> successQueue;

    private final BlockingQueue<ProducerBatch> failureQueue;

    private final AtomicInteger batchCount;

    public SendProducerBatchTask(
            ProducerBatch batch,
            AsyncProducerConfig producerConfig,
            RetryQueue retryQueue,
            BlockingQueue<ProducerBatch> successQueue,
            BlockingQueue<ProducerBatch> failureQueue,
            AtomicInteger batchCount) {
        this.batch = batch;
        this.producerConfig = producerConfig;
        this.retryQueue = retryQueue;
        this.successQueue = successQueue;
        this.failureQueue = failureQueue;
        this.batchCount = batchCount;
    }

    @Override
    public void run() {
        try {
            sendProducerBatch(System.currentTimeMillis());
        } catch (Throwable t) {

            CLSLog.e("producer", CLSLog.format("Uncaught error in send producer batch task, topic_id="
                            + batch.getTopicId()
                            + ", e=%s",
                    t.getMessage()));
        }
    }

    private Map<String, String> getCommonHeadPara() {
        HashMap<String, String> headParameter = new HashMap<>(3);
        headParameter.put(Constants.CONST_CONTENT_LENGTH, "0");
        headParameter.put(Constants.CONST_CONTENT_TYPE, Constants.CONST_PROTO_BUF);
        headParameter.put(Constants.CONST_HOST, producerConfig.getHostName());
        return headParameter;
    }




    /**
     * buildPutLogsRequest
     * @param batch ProducerBatch
     * @return  PutLogsRequest
     */
    private PutLogsRequest buildPutLogsRequest(ProducerBatch batch) {
        List<LogItem> list = batch.getLogItems();
        Cls.LogGroup.Builder logGroup = Cls.LogGroup.newBuilder();
        for(LogItem tmp:list){
            logGroup.addLogs(tmp.mContents);
        }
        return new PutLogsRequest(batch.getTopicId(), producerConfig.getSourceIp(), "", logGroup);
    }

    /**
     * 构造报文，发送日志
     *
     * @param urlParameter  url param
     * @param headParameter headers
     * @param body          cls pb serialize body
     * @throws LogException  LogException
     */
    private PutLogsResponse sendLogs(Map<String, String> urlParameter, Map<String, String> headParameter, byte[] body) throws LogException {
        headParameter.put(Constants.CONST_CONTENT_LENGTH, String.valueOf(body.length));
        String signature;
        try {
            signature = QcloudClsSignature.buildSignature(producerConfig.getSecretId(), producerConfig.getSecretKey(), HttpMethod.POST.toString(), Constants.UPLOAD_LOG_RESOURCE_URI, urlParameter, headParameter, 300000);
        } catch (UnsupportedEncodingException e) {
            throw new LogException(ErrorCodes.ENCODING_EXCEPTION, e.getMessage());
        }
        headParameter.put(Constants.CONST_AUTHORIZATION, signature);
        headParameter.put("x-cls-compress-type", "lz4");
        headParameter.put("x-cls-add-source", "1");

        if (!producerConfig.getSecretToken().isEmpty()) {
            headParameter.put("X-Cls-Token", producerConfig.getSecretToken());
        }
        headParameter.put("User-Agent", "cls-android-sdk-1.0.3");
        URI uri = getHostURI();
        byte[] compressedData = LZ4Encoder.compressToLhLz4Chunk(body);
        RequestMessage requestMessage = buildRequest(uri, urlParameter, headParameter, compressedData, compressedData.length);
        headParameter.put(Constants.CONST_CONTENT_LENGTH, String.valueOf(compressedData.length));
        PutLogsResponse response;
        try {
            response = Sender.doPost(requestMessage);
        } catch (Exception e) {
            throw new LogException(ErrorCodes.SendFailed, e.getMessage());
        }
        switch (response.GetHttpStatusCode()) {
            case 200: return response;
            case 500: throw new LogException(ErrorCodes.INTERNAL_SERVER_ERROR, "internal server error");
            case 429: throw new LogException(ErrorCodes.SpeedQuotaExceed, "speed quota exceed");
            default: throw new LogException(ErrorCodes.BAD_RESPONSE, response.GetAllHeaders().toString());
        }

    }

    /**
     * 获取host uri
     * @return URI
     */
    private URI getHostURI() {
        String endPointUrl = producerConfig.getHttpType() + producerConfig.getHostName();
        try {
            return new URI(endPointUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(ErrorCodes.ENDPOINT_INVALID, e);
        }
    }

    private static RequestMessage buildRequest(URI endpoint,
                                               Map<String, String> parameters,
                                               Map<String, String> headers,
                                               byte[] content,
                                               long size)
    {
        RequestMessage request = new RequestMessage();
        request.setMethod(HttpMethod.POST);
        request.setEndpoint(endpoint);
        request.setResourcePath(Constants.UPLOAD_LOG_RESOURCE_URI);
        request.setParameters(parameters);
        request.setHeaders(headers);
        request.setContent(content);
        request.setContentLength(size);
        return request;
    }

    private void sendProducerBatch(long nowMs) throws InterruptedException {
        PutLogsResponse response = null;
        try {
            PutLogsRequest request = buildPutLogsRequest(batch);
            Map<String, String> headParameter = getCommonHeadPara();
            request.SetParam(Constants.TOPIC_ID, request.GetTopic());
            Map<String, String> urlParameter = request.GetAllParams();
            byte[] logBytes = request.GetLogGroupBytes(producerConfig.getSourceIp(), batch.getPackageId());
            response = sendLogs(urlParameter, headParameter, logBytes);
            Attempt attempt = new Attempt(true, response.GetRequestId(), "", "", nowMs);
            batch.appendAttempt(attempt);
            successQueue.put(batch);
        } catch (Exception e) {
            String requestId = "";
            if (response !=null) {
                requestId = response.GetRequestId();
            }
            Attempt attempt = buildAttempt(e, nowMs, requestId);
            batch.appendAttempt(attempt);
            if (meetFailureCondition(e)) {
                failureQueue.put(batch);
            } else {
                long retryBackoffMs = calculateRetryBackoffMs();
                batch.setNextRetryMs(System.currentTimeMillis() + retryBackoffMs);
                try {
                    retryQueue.put(batch);
                } catch (IllegalStateException e1) {
                    if (retryQueue.isClosed()) {
                        failureQueue.put(batch);
                    }
                }
            }
        }

    }

    private Attempt buildAttempt(Exception e, long nowMs, String requestId) {
        if (e instanceof LogException) {
            LogException logException = (LogException) e;
            return new Attempt(
                    false,
                    requestId,
                    logException.GetErrorCode(),
                    logException.GetErrorMessage(),
                    nowMs);
        } else {
            return new Attempt(false, "", ErrorCodes.BAD_RESPONSE, e.getMessage(), nowMs);
        }
    }

    private boolean meetFailureCondition(Exception e) {
        if (!isRetrievableException(e)) {
            return true;
        }
        if (retryQueue.isClosed()) {
            return true;
        }
        return (batch.getRetries() >= producerConfig.getRetries()
                && failureQueue.size() <= batchCount.get() / 2);
    }

    private boolean isRetrievableException(Exception e) {
        if (e instanceof LogException) {
            LogException logException = (LogException) e;
            return (logException.GetErrorCode().equals(ErrorCodes.SendFailed) ||
                    logException.GetErrorCode().equals(ErrorCodes.SpeedQuotaExceed)
            );
        }
        return false;
    }

    private long calculateRetryBackoffMs() {
        int retry = batch.getRetries();
        long retryBackoffMs = producerConfig.getBaseRetryBackoffMs() * LongMath.pow(2, retry);
        if (retryBackoffMs <= 0) {
            retryBackoffMs = producerConfig.getMaxRetryBackoffMs();
        }
        return Math.min(retryBackoffMs, producerConfig.getMaxRetryBackoffMs());
    }

}
