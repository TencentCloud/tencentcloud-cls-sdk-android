package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net.traceroute;

import android.os.SystemClock;
import android.text.TextUtils;

//import androidx.annotation.NonNull;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.CommandPerformer;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.bean.CommandStatus;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.IPUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Traceroute implements CommandPerformer {
    protected final String TAG = getClass().getSimpleName();
    private Config config;

    private CLSNetDiagnosis.Callback callback;
    private CLSNetDiagnosis.Output output;

    private boolean isUserStop = false;
    private TracerouteTask task;

    public Traceroute(Config config, CLSNetDiagnosis.Callback callback, CLSNetDiagnosis.Output output) {
        this.config = config == null ? new Config("") : config;
        this.callback = callback;
        this.output = output;
    }

    public Traceroute(String targetHost, CLSNetDiagnosis.Callback callback, CLSNetDiagnosis.Output output) {
        this(new Config(targetHost), callback, output);
    }

    @Override
    public void run() {
        CLSLog.d(TAG,  "run thread:" + Thread.currentThread().getId() + " name:" + Thread.currentThread().getName());
        isUserStop = false;
        InetAddress inetAddress;
        try {
            inetAddress = config.parseTargetAddress();
        } catch (UnknownHostException e) {
            CLSLog.e(TAG, String.format("traceroute parse %s occur error:%s ", config.targetHost, e.getMessage()));
            if (callback != null) {
                long timestamp = System.currentTimeMillis() / 1000;
                TracerouteResult result = new TracerouteResult(
                        "",
                        timestamp,
                        CommandStatus.CMD_STATUS_ERROR_UNKNOW_HOST,
                        config.getTargetHost()
                );
                callback.onComplete(result.toJson().toString());
            }
            return;
        }

        List<TracerouteNodeResult> nodeResults = new ArrayList<>();
        int countUnreachable = 0;
        long timestamp = System.currentTimeMillis() / 1000;
        long start = SystemClock.elapsedRealtime();
        for (int i = 1; i <= config.maxHop && !isUserStop; i++) {
            task = new TracerouteTask(inetAddress, i, config.countPerRoute, output);
            TracerouteNodeResult node = task.run();
            CLSLog.d(TAG, String.format("[thread]:%d, [trace node]:%s",
                    Thread.currentThread().getId(),
                    (node == null ? "null" : node.toString()))
            );
            if (node == null) {
                continue;
            }
            nodeResults.add(node);
            if (node.isFinalRoute())
                break;

            if (TextUtils.equals("*", node.getRouteIp())) {
                countUnreachable++;
            } else {
                countUnreachable = 0;
            }

            if (countUnreachable == 5) {
                break;
            }
        }

        TracerouteResult result = new TracerouteResult(
                config.getTargetAddress().getHostAddress(),
                timestamp,
                isUserStop ? CommandStatus.CMD_STATUS_USER_STOP : CommandStatus.CMD_STATUS_SUCCESSFUL,
                config.getTargetHost()
        );
        result.getTracerouteNodeResults().addAll(nodeResults);
        if (callback != null)
            callback.onComplete(result.toJson().toString());
    }

    @Override
    public void stop() {
        isUserStop = true;
        if (task != null)
            task.stop();
    }

    public Config getConfig() {
        return config;
    }

    public static class Config {
        private InetAddress targetAddress;
        private String targetHost;
        private int maxHop;
        private int countPerRoute;

        public Config(String targetHost) {
            this.targetHost = targetHost;
            this.maxHop = 32;
            this.countPerRoute = 3;
        }

        InetAddress getTargetAddress() {
            return targetAddress;
        }

        InetAddress parseTargetAddress() throws UnknownHostException {
            targetAddress = IPUtil.parseIPv4Address(targetHost);
            return targetAddress;
        }

        public String getTargetHost() {
            return targetHost;
        }

        public Config setTargetHost(String targetHost) {
            this.targetHost = targetHost;
            return this;
        }

        public int getMaxHop() {
            return maxHop;
        }

        public Config setMaxHop(int maxHop) {
            this.maxHop = Math.max(1, Math.min(maxHop, 128));
            return this;
        }

        public int getCountPerRoute() {
            return countPerRoute;
        }

        public Config setCountPerRoute(int countPerRoute) {
            this.countPerRoute = Math.max(1, Math.min(countPerRoute, 3));
            return this;
        }
    }
}
