package com.tencentcloudapi.cls.plugin.network_diagnosis;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import org.json.JSONObject;
import com.tencentcloudapi.cls.android.CLSConfig;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.JsonUtil;
import com.tencentcloudapi.cls.android.plugin.ISender;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.CommandRunner;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net.traceroute.Traceroute;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.Diagnosis;
import com.tencentcloudapi.cls.android.scheme.Scheme;

public class CLSNetDiagnosis {
    private static final String TAG = "CLSNetwork";
    /**
     * default timeout: 1 second
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    private static final int DEFAULT_TIMEOUT = 1 * 1000;

    private static final int DEFAULT_PING_BYTES = 64;

    private enum Type {
        PING,
        TCPPING,
        MTR,
        HTTP,
        TRACEROUTE,
    }

    private final CommandRunner CommandRunner = new CommandRunner();

    public void CommandRunner() {
        CommandRunner.start();
    }

    private ISender sender;
    private CLSConfig config;
    private final TaskIdGenerator taskIdGenerator = new TaskIdGenerator();
    private final Handler handler;

    private static class Holder {
        private final static CLSNetDiagnosis INSTANCE = new CLSNetDiagnosis();
    }

    public interface Callback {
        void onComplete(String result);
    }

    public interface Output {
        void write(String line);
    }

    private static class TaskIdGenerator {
        private final String prefix = String.valueOf(System.nanoTime());
        private long index = 0;

        @SuppressLint("DefaultLocale")
        synchronized String generate() {
            index += 1;
            return String.format("%s_%d", prefix, index);
        }
    }

    public static CLSNetDiagnosis getInstance() {
        return Holder.INSTANCE;
    }

    void init(CLSConfig config, ISender sender) {
        this.config = config;
        this.sender = sender;
        CommandRunner();
    }

    /**
     * @param accessKeyId
     * @param accessKeySecret
     * @param securityToken
     */
    void resetSecurityToken(String accessKeyId, String accessKeySecret, String securityToken) {
        this.sender.resetSecurityToken(accessKeyId, accessKeySecret, securityToken);
    }

    /**
     * @param endpoint
     * @param topicId
     */
     void resetTopicID(String endpoint, String topicId) {
         this.sender.resetTopicID(endpoint, topicId);
    }


    private CLSNetDiagnosis() {
        handler = new Handler(Looper.getMainLooper());
    }

    private void report(Type type, String result, Callback callback) {
        if (config.debuggable) {
            CLSLog.v(TAG, "diagnosis, result: " + result);
        }
        Scheme scheme = Scheme.createDefaultScheme(config);
        if (!TextUtils.isEmpty(scheme.app_id) && scheme.app_id.contains("@")) {
            scheme.app_id = scheme.app_id.substring(0, scheme.app_id.indexOf("@"));
        }
        scheme.reserve6 = result;

        JSONObject reserves = new JSONObject();
        if (type == Type.PING) {
            JsonUtil.putOpt(reserves, "method", "PING");
        } else if (type == Type.TCPPING) {
            JsonUtil.putOpt(reserves, "method", "TCPPING");
        } else if (type == Type.MTR) {
            JsonUtil.putOpt(reserves, "method", "MTR");
        } else if (type == Type.HTTP) {
            JsonUtil.putOpt(reserves, "method", "HTTP");
        } else if (type == Type.TRACEROUTE) {
            JsonUtil.putOpt(reserves, "method", "TRACEROUTE");
        }else {
            JsonUtil.putOpt(reserves, "method", "UNKNOWN");
        }
        scheme.reserves = reserves.toString();
        sender.send(scheme);

        if (null != callback) {
            handler.post(() -> callback.onComplete(result));
        }
    }

    /**
     * @param domain   ?????? host?????? cloud.tencent.com
     * @param output   ?????? callback
     * @param callback ?????? callback
     */
    public void ping(String domain, Output output, Callback callback) {
        this.ping(domain, 10, DEFAULT_PING_BYTES, output, callback);
    }

    /**
     * @param domain   ?????? host?????? cloud.tencent.com
     * @param maxTimes ???????????????
     * @param size     ???????????????
     * @param output   ?????? callback
     * @param callback ?????? callback
     */
    public void ping(String domain, int maxTimes, int size, Output output, Callback callback) {
        Diagnosis.ping(domain, maxTimes, size, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.PING, result, callback);
            }
        });
    }

    /**
     * @param domain   ?????? host?????????cloud.tencent.com
     * @param port     ?????????????????????80
     * @param output   ?????? callback
     * @param callback ?????? callback
     */
    public void tcpPing(String domain, int port, Output output, Callback callback) {
        this.tcpPing(domain, port, 10, DEFAULT_TIMEOUT, output, callback);
    }

    /**
     * @param domain   ?????? host?????????cloud.tencent.com
     * @param port     ?????????????????????80
     * @param maxTimes ???????????????
     * @param timeout  ???????????????????????????
     * @param callback ?????? callback
     */
    public void tcpPing(String domain, int port, int maxTimes, int timeout, Output output, Callback callback) {
        Diagnosis.tcpPing(domain, port, maxTimes, timeout, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TCPPING, result, callback);
            }
        });
    }

    /**
     * @param domain ?????? host?????????cloud.tencent.com
     * @param output ?????? callback
     * @param callback ?????? callback
     */
    public void traceroute(String domain, Output output, Callback callback) {
        Traceroute traceroute = new Traceroute(new Traceroute.Config(domain), new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TRACEROUTE, result, callback);
            }
        }, output);
        traceroute(traceroute);
    }

    /**
     *
     * @param domain ?????? host?????????cloud.tencent.com
     * @param maxHop
     * @param countPerRoute
     * @param output   ?????? callback
     * @param callback ?????? callback
     */
    public void traceroute(String domain, int maxHop, int countPerRoute, Output output, Callback callback) {
        Traceroute.Config config =  new Traceroute.Config(domain);
        config.setMaxHop(maxHop);
        config.setCountPerRoute(countPerRoute);
        Traceroute traceroute = new Traceroute(new Traceroute.Config(domain), callback, output);
        traceroute(traceroute);
    }

    private void traceroute(Traceroute traceroute) {
        if (traceroute == null)
            throw new NullPointerException("The parameter (traceroute) is null !");

        if (CommandRunner != null && CommandRunner.isAlive()) {
            boolean res = CommandRunner.addCommand(traceroute);
            CLSLog.d(TAG, "[add task traceroute]: " + traceroute.getConfig().getTargetHost() + " res:" + res);
        }
    }

    /**
     * @param domain ?????? host?????????cloud.tencent.com
     */
    public void mtr(String domain) {
        this.mtr(domain, null);
    }

    /**
     * @param domain   ?????? host?????? cloud.tencent.com
     * @param callback ?????? callback
     */
    public void mtr(String domain, Callback callback) {
        this.mtr(domain, 30, 1, 10, DEFAULT_TIMEOUT, callback);
    }

    /**
     * @param domain   ?????? host?????????cloud.tencent.com
     * @param maxTtl   ??????????????????
     * @param maxPath  ??????????????????
     * @param maxTimes ???????????????
     * @param timeout  ???????????????????????????
     * @param callback ?????? callback
     */
    public void mtr(String domain, int maxTtl, int maxPath, int maxTimes, int timeout, Callback callback) {
//
    }

    public void http(String httpUrl, Callback callback) {
//        Diagnosis.startHttpPing(new HttpConfig(taskIdGenerator.generate(), httpUrl, (context, result) -> {
//            report(Type.HTTP, result, callback);
//            return 0;
//        }, this));
    }

    public void http(String httpUrl) {
        this.http(httpUrl, null);
    }

}
