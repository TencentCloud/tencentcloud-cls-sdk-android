package com.tencentcloudapi.cls.plugin.network_diagnosis.network;



import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import java.util.UUID;


/**
 * @author farmerx
 */
public class Diagnosis {

    private static String appKey;
    private static String siteId;
    private static String deviceId;
    private static final String TAG = Diagnosis.class.getCanonicalName();

    public Diagnosis() {
    }

    /**
     * @param domain 目标 host，如 www.tencentcloud.com
     * @param output 输出 callback
     * @param callback 回调 callback
     */
    public static void ping(String domain, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback callback) {
        Ping.start(domain, 10, 56, output, callback);
    }

    /**
     * @param domain   目标 host，如 www.tencentcloud.com
     * @param maxTimes 探测的次数
     * @param size     发包字节数
     * @param output 输出 callback
     * @param callback 回调 callback
     */
    public static void ping(String domain, int maxTimes, int size, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback callback) {
        Ping.start(domain, maxTimes, size, output, callback);
    }

    /**
     * @param domain   目标 host，如：www.tencentcloud.com
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public static void tcpPing(String domain, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback callback) {
        TcpPing.start(domain, output, callback);
    }

    /**
     * @param domain   目标 host，如：www.tencentcloud.com
     * @param port     目标端口，如：80
     * @param maxTimes 探测的次数
     * @param timeout  单次探测的超时时间
     * @param callback 回调 callback
     */
    public static void tcpPing(String domain, int port, int maxTimes, int timeout, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback callback) {
        TcpPing.start(domain, port, maxTimes, timeout, output, callback);
    }

    private static String getDeviceId() {
        if (deviceId == null || deviceId.equalsIgnoreCase("")) {
            deviceId = UUID.randomUUID().toString().replace("-", "");
        }
        return deviceId;
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

    private static void sleep(int millis) {
        try {
            Thread.sleep((long)millis);
        } catch (InterruptedException var2) {
            var2.printStackTrace();
        }

    }
}

