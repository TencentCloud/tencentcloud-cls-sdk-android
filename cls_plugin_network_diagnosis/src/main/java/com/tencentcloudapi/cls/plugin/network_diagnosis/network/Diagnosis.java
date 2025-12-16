package com.tencentcloudapi.cls.plugin.network_diagnosis.network;


import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.Network;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.Channel.ConnectionType;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
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

    public static void init(Context context, String networkAppId, String appKey, String uin, ClsConfigOptions mConfig) {
        Application application = (Application) context.getApplicationContext();
        Utils.storeApplication(application);
        Utils.storeNetworkAppIdId(networkAppId);
        Utils.storeUin(uin);
        Utils.storeAppKey(appKey);
        Utils.storeConfig(mConfig);
        startTask();
    }

    private static native String PingDetect(String target, int size, int maxTimes, int timeout, int interval, int prefer, SocketBinder binder);

    private static native String DnsDetect(String domain, Object dnsServers, int timeout, int prefer, SocketBinder binder);

    private static native String MtrDetect(String target, String protocol, int maxTtl, int timeout, int times, int prefer, SocketBinder binder);

    private static boolean loadLib() {
        try {
            System.loadLibrary("clsnetworkdiagnosis");
            return true;
        } catch (Throwable e) {
            CLSLog.e(TAG, e.getMessage());
            return false;
        }
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
                                    Diagnosis.statDnsInner(dnsConfig);
                                } else if (config instanceof HttpConfig) {
                                    HttpConfig httpConfig = (HttpConfig)config;
                                    Diagnosis.startHttpPingInner(httpConfig);
                                } else if (config instanceof MtrConfig) {
                                    MtrConfig mtrConfig = (MtrConfig)config;
                                    Diagnosis.startMtrInner(mtrConfig);
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
        void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object config, long netId, String interfaceName);
    }

    private static void OverLayDetect(DetectionFunc func, String taskId, String connectionType, Network network, JSONObject netInfo, Object config, long netId, String interfaceName) {
        func.detection(taskId, connectionType, network, netInfo, config, netId, interfaceName);
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
                    OverLayDetect(func, taskId, Channel.stringConnectionType(ctype), null, netInfo, config, -1L, "");
                } else {
                    boolean hasCellular = false;
                    final Channel.ConnectivityManagerDelegate cmd = new Channel.ConnectivityManagerDelegate(Utils.getApplication().getApplicationContext());
                    Channel.NetworkState networkState = cmd.getNetworkState();
                    Channel.ConnectionType connectionType = Channel.getConnectionType(networkState.isConnected(), networkState.getNetworkType(), networkState.getNetworkSubType());
                    DetectConfig dc = (DetectConfig)config;
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
                                String interfaceName = cmd.getInterfaceName(n);
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
                                        netInfo.put("dns", cmd.getDnsServers(n));
                                        OverLayDetect(func, taskId, ctType, n, netInfo, config, netId, interfaceName);
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
                            directCallback(dc, -9004, "current network NONE");
                        } else {
                            Network network = cmd.getDefaultNetwork();
                            long netId = cmd.getDefaultNetId();
                            JSONObject netInfo = cmd.getNetInfo(network, netId);
                            netInfo.put("dns", cmd.getDnsServers(network));
                            OverLayDetect(func, taskId, Channel.stringConnectionType(connectionType), network, netInfo, config, netId, cmd.getInterfaceName(network));
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
            public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig, long netId, String interfaceName) {
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
            public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig, long netId, String interfaceName) {
                TcpPingConfig config = (TcpPingConfig)oConfig;
                JSONObject res = new DetectTcpPing().doDetectTcpPing(taskId, connectionType, network, netInfo, config);
                config.callback.onComplete(res);
            }
        }, config, config.taskId);
    }

    private static void closeSocket(ParcelFileDescriptor pfd) {
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException e) {
                CLSLog.e(TAG, "Failed to close socket: " + e.getMessage());
                CLSLog.printStackTrace(e);
            }
        }
    }

    @SuppressLint({"NewApi"})
    private static void statPingInner(PingConfig config) {
        if (loadLib()) {
            config.domain = fixDomain(config.domain);
            startDetect(new DetectionFunc() {
                public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig, long netId, String interfaceName) {
                    SocketBinder binder = null;
                    try {
                        // 如果提供了Network对象，创建binder（networkId由binder内部管理）
                        if (null != network) {
                            binder = new NetworkSocketBinder(network, interfaceName);
                        }
                        // 直接调用PingDetect，socket创建和绑定都在JNI层完成
                        String value = Diagnosis.PingDetect(
                                config.domain,
                                config.getSize(),
                                config.maxTimes,
                                config.timeout,
                                config.interval,
                                0,
                                binder
                        );
                        JSONObject resultJson = new JSONObject(value);
                        resultJson.put("netInfo", netInfo);
                        resultJson.put("interface", connectionType);
                        config.callback.onComplete(resultJson);
                    } catch (JSONException e) {
                        CLSLog.e(TAG, "Failed to parse ping result: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    } catch (Exception e) {
                        CLSLog.e(TAG, "Ping detection failed: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    }
                }
            }, config, config.taskId);
        }
    }

    @SuppressLint({"NewApi"})
    private static void statDnsInner(DnsConfig config) {
        if (loadLib()) {
            config.domain = fixDomain(config.domain);
            startDetect(new DetectionFunc() {
                public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig, long netId, String interfaceName) {
                    DnsConfig config = (DnsConfig)oConfig;
                    if (null == config.type) {
                        config.type = "A";
                    }
                    Object dnsServers = null;
                    try {
                        if (null != config.server && !config.server.isEmpty()) {
                            ArrayList<String> dnsList = new ArrayList<>();
                            dnsList.add(config.server);
                            dnsServers = dnsList;
                        } else if (netInfo.has("dns")) {
                            String dnsString = netInfo.getString("dns");
                            dnsServers = Utils.parseDnsStringToJsonArray(dnsString);
                        }
                    } catch (JSONException e) {
                        CLSLog.e(TAG, "Failed to parse DNS servers: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    }

                    SocketBinder binder = null;
                    try {
                        // 如果提供了Network对象，创建binder（networkId由binder内部管理）
                        if (null != network) {
                            binder = new NetworkSocketBinder(network, interfaceName);
                        }
                        // 直接调用DnsDetect，socket创建和绑定都在JNI层完成
                        String value = Diagnosis.DnsDetect(
                                config.domain,
                                dnsServers,
                                config.timeout,
                                Objects.equals(config.type, "A") ? 2 : 3,// prefer: 2=IPv4 only, 0=IPv4优先
                                binder      // SocketBinder回调接口（可以为null，networkId由binder内部管理）
                        );

                        JSONObject resultJson = new JSONObject(value);
                        resultJson.put("netInfo", netInfo);
                        resultJson.put("interface", connectionType);
                        config.callback.onComplete(resultJson);
                    } catch (JSONException e) {
                        CLSLog.e(TAG, "Failed to parse DNS result: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    } catch (Exception e) {
                        CLSLog.e(TAG, "DNS detection failed: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    }
                }
            }, config, config.taskId);
        }
    }

    @SuppressLint({"NewApi"})
    private static void startMtrInner(MtrConfig config) {
        if (loadLib()) {
            config.domain = fixDomain(config.domain);
            startDetect(new DetectionFunc() {
                public void detection(String taskId, String connectionType, Network network, JSONObject netInfo, Object oConfig, long netId, String interfaceName) {
                    MtrConfig config = (MtrConfig)oConfig;
                    SocketBinder binder = null;
                    try {
                        // 如果提供了Network对象，创建binder（networkId由binder内部管理）
                        if (null != network) {
                            binder = new NetworkSocketBinder(network, interfaceName);
                        }
                        String value = Diagnosis.MtrDetect(
                                config.domain,
                                config.protocol,
                                config.maxTtl,
                                config.timeout,
                                config.maxTimes,
                                0,  // prefer: 0=IPv4优先
                                binder
                        );
                        JSONObject resultJson = new JSONObject(value);
                        resultJson.put("netInfo", netInfo);
                        resultJson.put("host", config.domain);
                        resultJson.put("max_paths", config.maxPaths);
                        resultJson.put("type", config.protocol);
//                        resultJson.put("interface", connectionType);
                        config.callback.onComplete(resultJson);
                    } catch (JSONException e) {
                        CLSLog.e(TAG, "Failed to parse MTR result: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    } catch (Exception e) {
                        CLSLog.e(TAG, "MTR detection failed: " + e.getMessage());
                        CLSLog.printStackTrace(e);
                    }
                }
            }, config, config.taskId);
        }
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

