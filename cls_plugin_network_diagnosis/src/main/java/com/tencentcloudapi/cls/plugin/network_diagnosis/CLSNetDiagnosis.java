package com.tencentcloudapi.cls.plugin.network_diagnosis;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.ClsDataAPI;
import com.tencentcloudapi.cls.android.producer.common.LogContent;
import com.tencentcloudapi.cls.android.producer.common.LogItem;
import com.tencentcloudapi.cls.android.producer.errors.ProducerException;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.CommandRunner;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net.traceroute.Traceroute;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.Diagnosis;
import com.tencentcloudapi.cls.android.scheme.Scheme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private ClsConfigOptions mConfig;
    private Context mContext;

    private Map<String, String> ext = new LinkedHashMap<>();
    public Map<String, String> getExt() {
        return ext;
    }
    private final TaskIdGenerator taskIdGenerator = new TaskIdGenerator();
    private final Handler handler;

    private String reportTopicId = "";

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

    void init(Context context, ClsConfigOptions config, Map<String, String> ext, String reportTopicId) {
        this.mConfig = config;
        this.mContext = context;
        this.reportTopicId = reportTopicId;
        if (null != ext && !ext.isEmpty()) {
            for (Map.Entry<String, String> entry : ext.entrySet()) {
                this.ext.put( entry.getKey(), entry.getValue());
            }
        }
        CommandRunner();
    }

    private CLSNetDiagnosis() {
        handler = new Handler(Looper.getMainLooper());
    }

    private void report(Type type, String result, Callback callback, Map<String, String> customField) {
        CLSLog.v(TAG, "diagnosis, result: " + result);
        Scheme scheme = Scheme.createDefaultScheme(mContext, mConfig, ext);
        if (!TextUtils.isEmpty(scheme.app_id) && scheme.app_id.contains("@")) {
            scheme.app_id = scheme.app_id.substring(0, scheme.app_id.indexOf("@"));
        }
        if (!customField.isEmpty()) {
            for (Map.Entry<String, String> entry : customField.entrySet()) {
                scheme.ext.put( entry.getKey(), entry.getValue());
            }
        }
        scheme.result = result;
        if (type == Type.PING) {
            scheme.method = "PING";
        } else if (type == Type.TCPPING) {
            scheme.method = "TCPPING";
        } else if (type == Type.MTR) {
            scheme.method = "MTR";
        } else if (type == Type.HTTP) {
            scheme.method = "HTTP";
        } else if (type == Type.TRACEROUTE) {
            scheme.method = "TRACEROUTE";
        }else {
            scheme.method = "UNKNOWN";
        }
        int ts = (int) (System.currentTimeMillis() / 1000);
        LogItem logItem = new LogItem(ts);
        for (Map.Entry<String,String> entry : scheme.toMap().entrySet()) {
            logItem.PushBack(new LogContent(entry.getKey(), entry.getValue()));
        }
        try {
            if (TextUtils.isEmpty(reportTopicId)) {
                ClsDataAPI.sharedInstance(this.mContext).trackLog(logItem);
            } else {
                ClsDataAPI.sharedInstance(this.mContext).trackLog(reportTopicId, logItem);
            }
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        if (null != callback) {
            handler.post(() -> callback.onComplete(result));
        }
    }

    /**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void ping(String domain, Output output, Callback callback) {
        this.ping(domain, 10, DEFAULT_PING_BYTES, output, callback);
    }

    /**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param output   输出 callback
     * @param callback 回调 callback
     * @param customField 自定义字段
     */
    public void ping(String domain, Output output, Callback callback, Map<String, String> customField) {
        this.ping(domain, 10, DEFAULT_PING_BYTES, output, callback);
    }

    /**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param maxTimes 探测的次数
     * @param size     探测包体积
     * @param output   输出 callback
     * @param callback 回调 callback
     * @param customField 自定义字段
     */
    public void ping(String domain, int maxTimes, int size, Output output, Callback callback, Map<String, String> customField) {
        Diagnosis.ping(domain, maxTimes, size, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.PING, result, callback, customField);
            }
        });
    }

    /**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param maxTimes 探测的次数
     * @param size     探测包体积
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void ping(String domain, int maxTimes, int size, Output output, Callback callback) {
        Diagnosis.ping(domain, maxTimes, size, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.PING, result, callback, new LinkedHashMap<>());
            }
        });
    }

    /**
     * @param url   目标 host，如：cloud.tencent.com
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void httpPing(String url, Output output, Callback callback) {
        Diagnosis.httpPing(url, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.HTTP, result, callback, new LinkedHashMap<>());
            }
        });
    }

    /**
     * @param url   目标 host，如：cloud.tencent.com
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void httpPing(String url, Output output, Callback callback, Map<String, String> customField) {
        Diagnosis.httpPing(url, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.HTTP, result, callback, customField);
            }
        });
    }

    /**
     * @param domain   目标 host，如：cloud.tencent.com
     * @param port     目标端口，如：80
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void tcpPing(String domain, int port, Output output, Callback callback, Map<String, String> customField) {
        this.tcpPing(domain, port, 10, DEFAULT_TIMEOUT, output, callback, customField);
    }

    /**
     * @param domain   目标 host，如：cloud.tencent.com
     * @param port     目标端口，如：80
     * @param maxTimes 探测的次数
     * @param timeout  单次探测的超时时间
     * @param callback 回调 callback
     */
    public void tcpPing(String domain, int port, int maxTimes, int timeout, Output output, Callback callback, Map<String, String> customField) {
        Diagnosis.tcpPing(domain, port, maxTimes, timeout, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TCPPING, result, callback, customField);
            }
        });
    }

    /**
     * @param domain   目标 host，如：cloud.tencent.com
     * @param port     目标端口，如：80
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void tcpPing(String domain, int port, Output output, Callback callback) {
        this.tcpPing(domain, port, 10, DEFAULT_TIMEOUT, output, callback);
    }

    /**
     * @param domain   目标 host，如：cloud.tencent.com
     * @param port     目标端口，如：80
     * @param maxTimes 探测的次数
     * @param timeout  单次探测的超时时间
     * @param callback 回调 callback
     */
    public void tcpPing(String domain, int port, int maxTimes, int timeout, Output output, Callback callback) {
        Diagnosis.tcpPing(domain, port, maxTimes, timeout, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TCPPING, result, callback, new LinkedHashMap<>());
            }
        });
    }

    /**
     * @param domain 目标 host，如：cloud.tencent.com
     * @param output 输出 callback
     * @param callback 回调 callback
     */
    public void traceroute(String domain, Output output, Callback callback) {
        Traceroute traceroute = new Traceroute(new Traceroute.Config(domain), new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TRACEROUTE, result, callback, new LinkedHashMap<>());
            }
        }, output);
        traceroute(traceroute);
    }

    /**
     * @param domain 目标 host，如：cloud.tencent.com
     * @param output 输出 callback
     * @param callback 回调 callback
     */
    public void traceroute(String domain, Output output, Callback callback, Map<String, String> customField) {
        Traceroute traceroute = new Traceroute(new Traceroute.Config(domain), new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TRACEROUTE, result, callback, customField);
            }
        }, output);
        traceroute(traceroute);
    }

    /**
     *
     * @param domain 目标 host，如：cloud.tencent.com
     * @param maxHop
     * @param countPerRoute
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void traceroute(String domain, int maxHop, int countPerRoute, Output output, Callback callback, Map<String, String> customField) {
        Traceroute.Config config = new Traceroute.Config(domain);
        config.setMaxHop(maxHop);
        config.setCountPerRoute(countPerRoute);
        Traceroute traceroute = new Traceroute(config, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TRACEROUTE, result, callback, customField);
            }
        }, output);
        traceroute(traceroute);
    }

    /**
     *
     * @param domain 目标 host，如：cloud.tencent.com
     * @param maxHop
     * @param countPerRoute
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void traceroute(String domain, int maxHop, int countPerRoute, Output output, Callback callback) {
        Traceroute.Config config =  new Traceroute.Config(domain);
        config.setMaxHop(maxHop);
        config.setCountPerRoute(countPerRoute);
        Traceroute traceroute = new Traceroute(config, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TRACEROUTE, result, callback, new LinkedHashMap<>());
            }
        }, output);
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
     * @param domain 目标 host，如：cloud.tencent.com
     */
    public void mtr(String domain) {
        this.mtr(domain, null);
    }

    /**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param callback 回调 callback
     */
    public void mtr(String domain, Callback callback) {
        this.mtr(domain, 30, 1, 10, DEFAULT_TIMEOUT, callback);
    }

    /**
     * @param domain   目标 host，如：cloud.tencent.com
     * @param maxTtl   最大生存时间
     * @param maxPath  探测路径数量
     * @param maxTimes 探测的次数
     * @param timeout  单次探测的超时时间
     * @param callback 回调 callback
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
