package com.tencentcloudapi.cls.plugin.network_diagnosis.network;



import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.Network;
import android.os.Build;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.Channel.ConnectionType;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author farmerx
 */
public class Diagnosis {
    private static final String TAG = Diagnosis.class.getCanonicalName();
    private static boolean mInvited;

    private static ConcurrentLinkedQueue<DetectConfig> configQueue = new ConcurrentLinkedQueue<>();

    public Diagnosis() {
    }

    public static void init(Context context) {
        Application application = (Application) context.getApplicationContext();
        Utils.storeApplication(application);
        startTask();
    }

    static boolean isCellularNetwork(Channel.ConnectionType ct) {
        return ct == ConnectionType.CONNECTION_2G || ct == ConnectionType.CONNECTION_3G || ct == ConnectionType.CONNECTION_4G || ct == ConnectionType.CONNECTION_5G || ct == ConnectionType.CONNECTION_UNKNOWN_CELLULAR;
    }

    private static void directCallback(DetectConfig config, int errorCode, String errorMessage) {
        if (config.callback != null) {
            try {
                JSONObject o = new JSONObject();
                o.put("domain", config.domain);
                o.put("errCode", errorCode);
                o.put("errMsg", errorMessage);
                config.callback.onComplete(o);
            } catch (JSONException e) {
                CLSLog.printStackTrace(e);
            }
        }
    }

    private static synchronized void startTask() {
        if (!mInvited) {
            (new Thread(new Runnable() {
                public void run() {
                    while(true) {
                        DetectConfig config = Diagnosis.configQueue.poll();
                        if (config == null) {
                            Diagnosis.sleep(100);
                        } else {
                            try {
                                if (config instanceof TcpPingConfig) {
                                    TcpPingConfig tcpPingConfig = (TcpPingConfig)config;
                                    Diagnosis.startTcpPingInner(tcpPingConfig);
                                } else if (config instanceof PingConfig) {
                                    PingConfig pingConfig = (PingConfig)config;
                                    Diagnosis.statPingInner(pingConfig);
                                } else if (config instanceof DnsConfig) {
                                    DnsConfig dnsConfig = (DnsConfig)config;
//                                    Diagnosis.startDnsInner(dnsConfig);
                                } else if (config instanceof HttpConfig) {
                                    HttpConfig httpConfig = (HttpConfig)config;
                                    Diagnosis.startHttpPingInner(httpConfig);
                                } else if (config instanceof TracerouteConfig) {
                                    TracerouteConfig tracerouteConfig = (TracerouteConfig)config;
//                                    Diagnosis.startMtrInner(tracerouteConfig);
                                } else {
                                    CLSLog.d(Diagnosis.TAG, "detection config mismatch");
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            })).start();
            CLSLog.d(TAG, "start cls diagnosis task");
            mInvited = true;
        }
    }

    private interface DetectionFunc {
        void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object config);
    }

    private static void OverLayDetect(DetectionFunc func, String taskId, String connectionType, Network network, JSONObject netInfo, Object config) {
        func.detection(taskId, connectionType, network, netInfo, config);
    }


    @SuppressLint({"NewApi"})
    private static void startDetect(final DetectionFunc func, final Object config, String taskId) {
            try {
                if (taskId == null || taskId.isEmpty()) {
                    taskId = UUID.randomUUID().toString();
                }
                if (Build.VERSION.SDK_INT < 23) {
                    Channel.ConnectivityManagerDelegate cmd = new Channel.ConnectivityManagerDelegate(Utils.getApplication().getApplicationContext());
                    Channel.NetworkState netState = cmd.getNetworkStateBak();
                    Channel.ConnectionType ctype = Channel.getConnectionType(netState.isConnected(), netState.getNetworkType(), netState.getNetworkSubType());
                    JSONObject netInfo = cmd.getNetInfoBak();
                    OverLayDetect(func, taskId, Channel.stringConnectionType(ctype), null, netInfo, config);
                } else {
                    boolean hasCellular = false;
                    final Channel.ConnectivityManagerDelegate cmd = new Channel.ConnectivityManagerDelegate(Utils.getApplication().getApplicationContext());
                    Channel.NetworkState networkState = cmd.getNetworkState();
                    Channel.ConnectionType connectionType = Channel.getConnectionType(networkState.isConnected(), networkState.getNetworkType(), networkState.getNetworkSubType());
                    String currentNetType = Channel.stringConnectionType(connectionType);
                    CLSLog.d(TAG, "current network " + currentNetType + ", netId: " + cmd.getDefaultNetId());
                    DetectConfig dc = (DetectConfig)config;
                    System.out.println(dc.multiplePortsDetect + " " + isCellularNetwork(connectionType));
                    if (dc.multiplePortsDetect && !isCellularNetwork(connectionType)) {
                        Network[] nets = Channel.getAllNetworks();
                        if (nets.length == 0) {
                            directCallback((DetectConfig)config, -9004, "NONE valid Network");
                        } else {
                            int detectNetworkNum = 0;
                            for(Network n : nets) {
                                Channel.NetworkState ns = cmd.getNetworkState(n);
                                Channel.ConnectionType ct = Channel.getConnectionType(ns.isConnected(), ns.getNetworkType(), ns.getNetworkSubType());
                                long netId = Channel.networkToNetId(n);
                                CLSLog.d(TAG, "detect " + Channel.stringConnectionType(ct) + " isConnected: " + ns.isConnected() + " type: " + ns.getNetworkType() + ", subType: " + ns.getNetworkSubType() + ", netId: " + netId);
                                if (!cmd.hasInternetCapability(n)) {
                                    CLSLog.d(TAG, "not has internet capability");
                                } else {
                                    if (ct == ConnectionType.CONNECTION_WIFI) {
                                        netId = -1L;
                                    } else if (ct != ConnectionType.CONNECTION_VPN && ct != ConnectionType.CONNECTION_ETHERNET) {
                                        if (ct != ConnectionType.CONNECTION_2G && ct != ConnectionType.CONNECTION_3G && ct != ConnectionType.CONNECTION_4G && ct != ConnectionType.CONNECTION_5G && ct != ConnectionType.CONNECTION_UNKNOWN_CELLULAR) {
                                            continue;
                                        }
                                        hasCellular = true;
                                    }
                                    if (!ns.isConnected()) {
                                        CLSLog.w(TAG, "startDetect connection type " + ct + " is not active");
                                    } else {
                                        String ctType = Channel.stringConnectionType(ct);
                                        JSONObject netInfo = cmd.getNetInfo(n, netId);
                                        OverLayDetect(func, taskId, ctType, n, netInfo, config);
                                        ++detectNetworkNum;
                                    }
                                }
                            }

                            if (0 == detectNetworkNum) {
                                CLSLog.w(TAG, "all network are invalid");
                                directCallback((DetectConfig)config, -9004, "all network are invalid");
                            }
                        }
                    } else {
                        if (cmd.getDefaultNetId() == -1L) {
                            directCallback((DetectConfig)config, -9004, "current network NONE");
                        } else {
                            Network network = cmd.getDefaultNetwork();
                            JSONObject netInfo = cmd.getNetInfo(network, -1L);
                            OverLayDetect(func, taskId, Channel.stringConnectionType(connectionType), network, netInfo, config);
                        }
                    }
                }
            } catch (Throwable e) {
                CLSLog.e(TAG, "startDetect exception: " + e.getMessage());
            }
    }


    @SuppressLint({"NewApi"})
    public static void startPing(DetectConfig config) {
        configQueue.add(config);
    }
    @SuppressLint({"NewApi"})
    public static void startTcpPing(DetectConfig config) {
        configQueue.offer(config);
    }
    @SuppressLint({"NewApi"})
    public static void startHttpPing(DetectConfig config) {
        configQueue.add(config);
    }
    @SuppressLint({"NewApi"})
    public static void startMtr(DetectConfig config) {
        configQueue.add(config);
    }
    @SuppressLint({"NewApi"})
    public static void startDns(DetectConfig config) {
        configQueue.add(config);
    }

    @SuppressLint({"NewApi"})
    private static void startHttpPingInner(HttpConfig config) {
        config.domain = fixDomain(config.domain);
        startDetect(new DetectionFunc() {
            public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig) {
                HttpConfig config = (HttpConfig)oConfig;
                JSONObject res = new DetectHttpPing().doDetectHttpPing(taskId, connectionType, network, netInfo, config);
                config.callback.onComplete(res);
            }
        }, config, config.taskId);
    }

    @SuppressLint({"NewApi"})
    private static void startTcpPingInner(TcpPingConfig config) {
        config.domain = fixDomain(config.domain);
        startDetect(new DetectionFunc() {
            public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig) {
                TcpPingConfig config = (TcpPingConfig)oConfig;
                JSONObject res = new DetectTcpPing().doDetectTcpPing(taskId, connectionType, network, netInfo, config);
                config.callback.onComplete(res);
            }
        }, config, config.taskId);
    }

    @SuppressLint({"NewApi"})
    private static void statPingInner(PingConfig config) {
        config.domain = fixDomain(config.domain);
        startDetect(new DetectionFunc() {
            public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig) {
                PingConfig config = (PingConfig)oConfig;
                if (config.interval <= 0) {
                    config.interval = 200;
                }
//                Ping.Result res = new DetectPing().doDetectPing(taskId, connectionType, network, netInfo, config);
//                if (null != res) {
//                    try {
//                        res.put("method", "ping");
//                    } catch (Exception ignored) {
//                    }
//                }
            }
        }, config, config.taskId);
    }

    private static String fixDomain(String domain) {
        if (domain != null && domain.contains(":")) {
            String[] array = domain.split(":");
            if (array.length == 2) {
                return array[0];
            }
        }
        return domain;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            CLSLog.printStackTrace(e);
        }
    }
}

