package com.tencentcloudapi.cls.plugin.network_diagnosis;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Base64;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.ClsDataAPI;
import com.tencentcloudapi.cls.android.scheme.Span;
import com.tencentcloudapi.cls.android.scheme.SpanBuilder;
import com.tencentcloudapi.cls.android.utils.AppUtils;
import com.tencentcloudapi.cls.android.utils.DeviceUtils;
import com.tencentcloudapi.cls.android.scheme.ISpanProvider;
import com.tencentcloudapi.cls.android.scheme.Resource;
import com.tencentcloudapi.cls.android.scheme.Attribute;
import com.tencentcloudapi.cls.android.utils.TimeUtils;
import com.tencentcloudapi.cls.android.utils.Utdid;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.DetectCallback;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.Diagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.DnsConfig;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.HttpConfig;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.MtrConfig;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.PingConfig;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.TcpPingConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NetworkDiagnosis implements INetworkDiagnosis {
    private static final String TAG = "CLSNetworkDiagnosis";
    private ISpanProvider spanProvider;
    private ClsConfigOptions mConfig;
    private Context mContext;
    private Map<String, String> extensions = new LinkedHashMap<>();

    private String mToken;

    private String mTopicId;

    private String mNetworkAppId;
    private String mAppKey;
    private String mUin;

    protected void onPreInit(Context context, ClsConfigOptions mConfig, Map<String, String> ext, String token, String topicId) {
        this.mContext = context;
        this.mConfig = mConfig;
        if (null != ext && !ext.isEmpty()) {
            try {
                Map<String, String> o = new LinkedHashMap<>();
                for(String key : ext.keySet()) {
                    o.put(key, ext.get(key));
                }
                this.extensions = o;
            } catch (Exception e) {
                CLSLog.printStackTrace(e);
            }
        }
        if (null != token) {
            this.mToken = token;
            String t = new String(Base64.decode(token, Base64.DEFAULT));
            try {
                JSONObject tokenJson = new JSONObject(t);
                if (tokenJson.has("topic_id")) {
                    this.mTopicId = tokenJson.getString("topic_id");
                }
                if (tokenJson.has("n_a_id")) {
                    this.mNetworkAppId = tokenJson.getString("n_a_id");
                }
                if (tokenJson.has("key")) {
                    this.mAppKey = tokenJson.getString("key");
                }
                if (tokenJson.has("uin")) {
                    this.mUin = tokenJson.getString("uin");
                }
            } catch (JSONException e) {
                CLSLog.printStackTrace(e);
            }

        } else {
            if (null != topicId) {
                this.mTopicId = topicId;
            }
        }


        initializeDefaultSpanProvider(this.mContext);
        Diagnosis.init(context, mNetworkAppId, mAppKey, mUin, this.mConfig);
        CLSNetworkDiagnosis.getInstance().setNetworkDiagnosis(this);
    }

    protected void initializeDefaultSpanProvider(Context context) {
        this.spanProvider = new ISpanProvider() {
            @Override
            public Resource provideResource() {
                return Resource.of(
                        Pair.create("device.id", Utdid.getInstance().getUtdid(context)),
                        Pair.create("app.version", AppUtils.getAppVersion(context)),
                        Pair.create("app.versionCode", AppUtils.getAppVersionCode(context)),
                        Pair.create("app.name", AppUtils.getAppName(context)),
                        Pair.create("device.resolution", DeviceUtils.getResolution(context)),
                        Pair.create("net.access", DeviceUtils.getAccessName(context)),
                        Pair.create("net.access_subtype",DeviceUtils.getAccessSubTypeName(context)),
                        Pair.create("carrier", DeviceUtils.getCarrier(context)),
                        Pair.create("os.root", DeviceUtils.isRoot())
                );
            }

            @Override
            public List<Attribute> provideAttribute() {
               return Attribute.of(
                        Pair.create("page.name", AppUtils.getTopActivity())
                );
            }
        };
    }

    private void report(Callback callback, JSONObject msg, Map<String, String> ext, String method,
                        String src, String taskId, long startTime) {
        if (null == msg) {
            CLSLog.w(TAG, "msg is empty.");
            return;
        }
        try {
            msg.put("method", method);
            msg.put("task_id", taskId);
            msg.put("network_app_id", this.mNetworkAppId);
            msg.put("src", src);
            msg.put("userEx", new JSONObject(this.extensions));
            if (ext != null) {
                msg.put("detectEx", new JSONObject(ext));
            }
        } catch (JSONException e) {
           CLSLog.printStackTrace(e);
        }
        Span span = createSpanBuilder(msg, method).build();
        span.setStart(startTime);
        span.end();
        try {
            if (null != this.mTopicId && !this.mTopicId.isEmpty()) {
                ClsDataAPI.sharedInstance(this.mContext).trackLog(this.mTopicId, span.toLogItem());
            } else {
                ClsDataAPI.sharedInstance(this.mContext).trackLog(span.toLogItem());
            }
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        handleCallback(callback, msg, method);
    }

    public void handleCallback(Callback callback, JSONObject msg, String method) {
        if (null == callback || null == msg) {
            return;
        }
        callback.onComplete(Response.response(Type.of(method), msg.toString()));
    }

    private SpanBuilder createSpanBuilder(JSONObject msg, String method) {
        CLSLog.v(TAG, "network diagnosis result: method=" + method + ", result: " + msg);
        SpanBuilder builder = new SpanBuilder("network_diagnosis", this.spanProvider);
        builder.addAttribute(
                Attribute.of(
                        Pair.create("net.type", method),
                        Pair.create("net.origin", msg)
                )
        );
        return builder;
    }

    @Override
    public void updateExtensions(Map<String, String> extension) {
        try {
            Map<String, String> o = new LinkedHashMap<>();
            for(String key : extension.keySet()) {
                o.put(key, extension.get(key));
            }
            this.extensions = o;
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }


    @Override
    public void ping(PingRequest request) {
       ping(request, null);
    }

    @Override
    public void ping(PingRequest request, Callback callback) {
        if (null == request || TextUtils.isEmpty(request.domain)) {
            if (null != callback) {
                callback.onComplete(Response.error("PingRequest is null or domain is empty.", Type.PING));
            }
            return;
        }
        String taskId = UUID.randomUUID().toString();
        long startTime = TimeUtils.instance.now();
        final PingConfig config = new PingConfig(
                taskId,
                request.domain,
                request.size,
                request.maxTimes,
                request.timeout,
                new DetectCallback() {
                    @Override
                    public void onComplete(JSONObject result) {
                        report(callback, result, request.extension, "ping", "app", taskId, startTime);
                    }
                }
        );
        config.multiplePortsDetect = request.multiplePortsDetect;
        Diagnosis.startPing(config);
    }

    @Override
    public void http(HttpRequest request) {
        http(request, null);
    }

    @Override
    public void http(HttpRequest request, Callback callback) {
        if (null == request || TextUtils.isEmpty(request.domain)) {
            if (null != callback) {
                callback.onComplete(Response.error("HttpRequest is null or domain is empty.", Type.HTTP));
            }
            return;
        }
        String taskId = UUID.randomUUID().toString();
        long startTime = TimeUtils.instance.now();
        final HttpConfig config = new HttpConfig(
                taskId,
                request.domain,
                request.ip,
                request.timeout,
                request.downloadBytesLimit,
                request.headerOnly,
                null != request.credential ? new com.tencentcloudapi.cls.plugin.network_diagnosis.network.HttpCredential(
                        request.credential.getSslContext(),
                        request.credential.getTrustManager()
                ) : null,
                new DetectCallback() {
                    @Override
                    public void onComplete(JSONObject result) {
                        report(callback, result, request.extension, "http", "app", taskId, startTime);
                    }
                }
        );
        config.multiplePortsDetect = request.multiplePortsDetect;
        Diagnosis.startHttpPing(config);
    }

    @Override
    public void tcpPing(TcpPingRequest request) {
        tcpPing(request, null);
    }

    @Override
    public void tcpPing(TcpPingRequest request, Callback callback) {
        if (null == request || TextUtils.isEmpty(request.domain)) {
            if (null != callback) {
                callback.onComplete(Response.error("TcpRequest is null or domain is empty.", Type.PING));
            }
            return;
        }
        String taskId = UUID.randomUUID().toString();
        long startTime = TimeUtils.instance.now();
        final TcpPingConfig config = new TcpPingConfig(
                taskId,
                request.domain,
                request.port <= 0 ? 80 : request.port,
                request.maxTimes <= 0 ? 1 : request.maxTimes,
                request.timeout <= 0 ? 3000 : request.timeout,
                new DetectCallback() {
                    @Override
                    public void onComplete(JSONObject result) {
                        try {
                            if (null != result) {
                                result.put("userEx", extensions);
                            }
                        } catch(Exception e) {
                            CLSLog.printStackTrace(e);
                        }
                        report(callback, result, request.extension, "tcpping", "app", taskId, startTime);
                    }
                }
        );

        if (null != request.payload && !request.payload.isEmpty()) {
            config.setPayload(request.payload);
        }

        config.multiplePortsDetect = request.multiplePortsDetect;
        Diagnosis.startTcpPing(config);
    }

    @Override
    public void dns(DnsRequest request) {
        dns(request, null);
    }

    @Override
    public void dns(DnsRequest request, Callback callback) {
        if (null == request || TextUtils.isEmpty(request.domain)) {
            if (null != callback) {
                callback.onComplete(Response.error("DnsRequest is null or domain is empty.", Type.PING));
            }
            return;
        }
        String taskId = UUID.randomUUID().toString();
        long startTime = TimeUtils.instance.now();
        final DnsConfig config = new DnsConfig(
                taskId,
                request.nameServer,
                request.domain,
                request.type,
                request.timeout <= 0 ? 3000 : request.timeout,
                new DetectCallback() {
                    @Override
                    public void onComplete(JSONObject result) {
                        try {
                            if (null != result) {
                                result.put("userEx", extensions);
                            }
                        } catch(Exception e) {
                            CLSLog.printStackTrace(e);
                        }
                        report(callback, result, request.extension, "dns", "app", taskId, startTime);
                    }
                },
                ""
        );
        config.multiplePortsDetect = request.multiplePortsDetect;
        Diagnosis.startDns(config);
    }

    @Override
    public void mtr(MtrRequest request) {
        mtr(request, null);
    }

    @Override
    public void mtr(MtrRequest request, Callback callback) {
        if (null == request || TextUtils.isEmpty(request.domain)) {
            if (null != callback) {
                callback.onComplete(Response.error("MtrRequest is null or domain is empty.", Type.MTR));
            }
            return;
        }
        String taskId = UUID.randomUUID().toString();
        long startTime = TimeUtils.instance.now();
        final MtrConfig config = new MtrConfig(
                taskId,
                request.domain,
                request.maxTTL,
                request.maxPaths,
                request.maxTimes,
                request.timeout,
                new DetectCallback() {
                    @Override
                    public void onComplete(JSONObject result) {
                        try {
                            if (null != result) {
                                result.put("userEx", extensions);
                            }
                        } catch(Exception e) {
                            CLSLog.printStackTrace(e);
                        }
                        report(callback, result, request.extension, "mtr", "app", taskId, startTime);
                    }
                },
                ""
        );
        config.protocol = request.protocol.protocol;
        config.multiplePortsDetect = request.multiplePortsDetect;
        Diagnosis.startMtr(config);
    }
}