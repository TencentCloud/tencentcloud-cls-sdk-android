package com.tencentcloud.cls.producer;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

import com.tencentcloud.cls.producer.common.*;
import com.tencentcloud.cls.producer.http.client.Sender;
import com.tencentcloud.cls.producer.http.comm.HttpMethod;
import com.tencentcloud.cls.producer.http.comm.RequestMessage;
import com.tencentcloud.cls.producer.request.PutLogsRequest;
import com.tencentcloud.cls.producer.response.PutLogsResponse;
import com.tencentcloud.cls.producer.util.Args;
import com.tencentcloud.cls.producer.util.NetworkUtils;
import com.tencentcloud.cls.producer.util.QcloudClsSignature;


/**
 * @author farmerx
 */
public class AsyncClient {

    private String httpType;
    private String hostName;
    private String secretId;
    private String secretKey;
    private String sourceIp;
    private int   retry_times;
    private ExecutorService executor = null;

    public AsyncClient(String endpoint, String secretId, String secretKey, String sourceIp, Integer retry_times, Integer poolSize) {
        configure(endpoint, secretId, secretKey, sourceIp, retry_times, poolSize);
    }

    /**
     * 关闭线程池
     */
    public void Close() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * 配置文件初始化
     * @param endpoint
     * @param secretId
     * @param secretKey
     * @param sourceIp
     * @param retry_times
     */
    private void configure(String endpoint, String secretId, String secretKey, String sourceIp, Integer retry_times, Integer poolSize) {
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
        if (sourceIp == null || sourceIp.isEmpty()) {
            this.sourceIp = NetworkUtils.getLocalMachineIP();
        }
        this.retry_times = retry_times;
        if (retry_times == null || retry_times <=0) {
            this.retry_times = 1;
        }

        if (poolSize == null || poolSize <=0) {
            poolSize = Math.max(Runtime.getRuntime().availableProcessors(), 1);
        }
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * 多线程去发送日志
     * @param request PutLogsRequest
     * @throws LogException
     */
    public Future<PutLogsResponse> PutLogs(PutLogsRequest request) throws LogException {
        byte[] logBytes = request.GetLogGroupBytes(this.sourceIp);
        // 日志上传体积限制检查
        if (logBytes.length > Constants.CONST_MAX_PUT_SIZE) {
            throw new LogException("InvalidLogSize", "logItems' size exceeds maximum limitation : "
                    + Constants.CONST_MAX_PUT_SIZE
                    + " bytes, logBytes=" + logBytes.length + ", topic=" + request.GetTopic());
        }

        Map<String, String> headParameter = GetCommonHeadPara();
        request.SetParam(Constants.TOPIC_ID, request.GetTopic());
        Map<String, String> urlParameter = request.GetAllParams();
        return this.SendLogs(HttpMethod.POST, Constants.UPLOAD_LOG_RESOURCE_URI, urlParameter, headParameter, logBytes);
    }

    /**
     * 构造报文，发送日志
     * @param method Http method
     * @param resourceUri  cls日志上报uri
     * @param urlParameter url param
     * @param headParameter headers
     * @param body cls pb serialize body
     * @throws LogException
     */
    private Future<PutLogsResponse> SendLogs(HttpMethod method, String resourceUri, Map<String, String> urlParameter, Map<String, String> headParameter, byte[] body) throws LogException {
        headParameter.put(Constants.CONST_CONTENT_LENGTH, String.valueOf(body.length));
        String signature = "";
        try {
            signature = QcloudClsSignature.buildSignature(secretId, secretKey, method.toString(), resourceUri, urlParameter, headParameter, 300000);
        } catch (UnsupportedEncodingException e) {
            throw new LogException(ErrorCodes.ENCODING_EXCEPTION, e.getMessage());
        }
        headParameter.put(Constants.CONST_AUTHORIZATION, signature);
        URI uri = GetHostURI();
        RequestMessage requestMessage = BuildRequest(uri, method, resourceUri, urlParameter, headParameter, body, body.length);
        return this.executor.submit(new ReportCallable(requestMessage, this.retry_times));
    }


    /**
     * 获取host uri
     * @return
     */
    private URI GetHostURI() {
        String endPointUrl = this.httpType + this.hostName;
        try {
            return new URI(endPointUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(ErrorCodes.ENDPOINT_INVALID, e);
        }
    }

    private static RequestMessage BuildRequest(URI endpoint,
                                               HttpMethod httpMethod, String resourceUri,
                                               Map<String, String> parameters, Map<String, String> headers,
                                               byte[] content, long size) {
        RequestMessage request = new RequestMessage();
        request.setMethod(httpMethod);
        request.setEndpoint(endpoint);
        request.setResourcePath(resourceUri);
        request.setParameters(parameters);
        request.setHeaders(headers);
        request.setContent(content);
        request.setContentLength(size);
        return request;
    }

    private Map<String, String> GetCommonHeadPara() {
        HashMap<String, String> headParameter = new HashMap<String, String>();
        headParameter.put(Constants.CONST_CONTENT_LENGTH, "0");
        headParameter.put(Constants.CONST_CONTENT_TYPE, Constants.CONST_PROTO_BUF);
        headParameter.put(Constants.CONST_HOST, this.hostName);
        return headParameter;
    }

    private static class ReportCallable implements Callable<PutLogsResponse> {
        private RequestMessage requestMessage;
        private Integer retry_times;

        public ReportCallable(RequestMessage requestMessage, int retry_times) {
            this.requestMessage = requestMessage;
            this.retry_times = retry_times;
        }

        @Override
        public PutLogsResponse call() throws Exception {
            PutLogsResponse resp = null;
            for (int retryTimes = 0; retryTimes < this.retry_times; retryTimes++) {
                try {
                    resp = Sender.doPost( this.requestMessage);
                    if (resp!=null && resp.GetHttpStatusCode() !=null && !(resp.GetHttpStatusCode()==200 || resp.GetHttpStatusCode()==413)) {
                        if (retryTimes+1 >= this.retry_times) {
                            throw new LogException(ErrorCodes.BAD_RESPONSE, "send log failed and exceed retry times");
                        }
                        continue;
                    }
                    return resp;
                } catch (Exception e) {
                    if (retryTimes+1 >= this.retry_times) {
                        throw new LogException(ErrorCodes.BAD_RESPONSE, e.getMessage());
                    }
                }
            }
            return resp;
        }
    }


//    public static void main(String[] args) throws LogException, ExecutionException, InterruptedException {
//        String endpoint = "ap-guangzhou.cls.tencentcs.com";
//        // API密钥 secretId，必填
//        String secretId = "*";
//        // API密钥 secretKey，必填
//        String secretKey = "*";
//        // 日志主题ID，必填
//        String topicId = "*";
//        // 日志源机器IP，可选
//        String source = "test_source";
//        // 日志源文件名，可选
//        String filename = "test_filename";
//
//        // 构建一个客户端实例
//        final AsyncClient client = new AsyncClient(endpoint, secretId, secretKey, NetworkUtils.getLocalMachineIP(), 5, 10);
//
//
//        int ts = (int) (System.currentTimeMillis() / 1000);
//        LogItem logItem = new LogItem(ts);
//        logItem.PushBack(new LogContent("__CONTENT__", "你好，我来自深圳|hello world"));
//        logItem.PushBack(new LogContent("city", "guangzhou"));
//        logItem.PushBack(new LogContent("logNo",
//                String.valueOf(System.currentTimeMillis() + new Random(1000).nextInt())));
//        logItem.PushBack(new LogContent("__PKG_LOGID__", (String.valueOf(System.currentTimeMillis()))));
//        Logs.LogGroup.Builder logGroup = Logs.LogGroup.newBuilder();
//        logGroup.addLogs(logItem.mContents);
//
//        final PutLogsRequest req = new PutLogsRequest(topicId, source, filename, logGroup);
//        Future<PutLogsResponse> resq = client.PutLogs(req);
//        // resq.get() 是阻塞的
//        System.out.println(resq.get().GetAllHeaders());
//        client.executor.shutdown();
//    }
}
