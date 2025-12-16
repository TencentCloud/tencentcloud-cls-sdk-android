package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import static com.tencentcloudapi.cls.plugin.network_diagnosis.network.Utils.calculateStdDevOptimized;

import android.annotation.SuppressLint;
import android.net.Network;
import com.tencentcloudapi.cls.android.CLSLog;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;


public class DetectTcpPing {
    public static final int TimeOut = -3;
    public static final int NotReach = -2;
    public static final int UnknownHost = -4;

    DetectTcpPing() {
    }

    private String getIp(String host) throws UnknownHostException {
        InetAddress i = InetAddress.getByName(host);
        return i.getHostAddress();
    }

    public JSONObject doDetectTcpPing(String taskId, String connectionType, Network network, JSONObject netInfo, TcpPingConfig config) {
        try {
            long dnsStart = System.nanoTime();
            String ip = getIp(config.domain);
            long dnsEnd = System.nanoTime();
            float dnsTime = (float) (dnsEnd-dnsStart)/1000000;
            InetSocketAddress server = new InetSocketAddress(ip, config.port);
            float[] times = new float[config.maxTimes];
            int index = -1;
            int dropped = 0;
            for (int i = 0; i < config.maxTimes; i++) {
                long start = System.nanoTime();
                try {
                    connect(network, server, config.timeout, config.payload);
                } catch (IOException e) {
                    int code = NotReach;
                    if (e instanceof SocketTimeoutException) {
                        code = TimeOut;
                    } else if (e instanceof UnknownHostException) {
                        code = UnknownHost;
                    }
                    if (i == 0) {
                        JSONObject o = new JSONObject();
                        o.put("domain", config.domain);
                        o.put("errCode", code);
                        o.put("errMsg", e.getMessage());
                        // 返回结果
                        return o;
                    } else {
                        dropped++;
                    }
                }
                long end = System.nanoTime();
                float t = (float) (end-start)/1000000;
                times[i] = t;
                index = i;
                try {
                    if (100 > t && t > 0) {
                        Thread.sleep(100 - (int)t);
                    }
                } catch (Exception e) {
                    CLSLog.printStackTrace(e);
                }
            }
            return buildResult(taskId, connectionType, netInfo, config, ip, times, index, dropped, dnsTime);
        } catch (UnknownHostException e) {
            CLSLog.printStackTrace(e);
            int code = UnknownHost;
            try {
                JSONObject o = new JSONObject();
                o.put("domain", config.domain);
                o.put("errCode", code);
                o.put("errMsg", e.getMessage());
                return o;
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
           CLSLog.printStackTrace(e);
        }
        return null;
    }

    private void connect(Network network, InetSocketAddress socketAddress, int timeout, String payload) throws IOException {
        Socket socket = null;
        OutputStream outputStream = null;
        try {
            if (null != network) {
                socket = network.getSocketFactory().createSocket();
            } else {
                socket = new Socket();
            }
            socket.connect(socketAddress, timeout);
            if (payload != null && !payload.isEmpty()) {
                outputStream = socket.getOutputStream();
                outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } catch (IOException e) {
            CLSLog.printStackTrace(e);
            throw e;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    CLSLog.printStackTrace(e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    CLSLog.printStackTrace(e);
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private JSONObject buildResult(String taskId, String connectionType, JSONObject netInfo,
                                   TcpPingConfig config, String ip, float[] times, int index,
                                   int dropped, float dnsTime) {
        float sum = 0;
        float min = 1000000;
        float max = 0;
        for (int i = 0; i <= index; i++) {
            float t = times[i];
            if (t > max) {
                max = t;
            }
            if (t < min) {
                min = t;
            }
            sum += t;
        }

        float stddev = (float) calculateStdDevOptimized(times, true);
        JSONObject o = new JSONObject();
        try {
            o.put("host", config.domain);
            o.put("host_ip", ip);
            o.put("port", config.port);
            o.put("dnsTime",  String.format("%.2f", dnsTime));
            o.put("task_id", taskId);
            o.put("count", config.maxTimes);
            o.put("interface", connectionType);
            o.put("loss",  String.format("%.2f", (float) dropped / (float) config.maxTimes));
            o.put("latency_max", String.format("%.2f", max));
            o.put("latency_min", String.format("%.2f", min));
            o.put("latency", String.format("%.2f", sum / (index + 1)));
            o.put("stddev", String.format("%.2f", stddev));
            o.put("total", String.format("%.2f", sum));
            o.put("responseNum", config.maxTimes - dropped);
            o.put("bindFailed", 0);
            o.put("netInfo", netInfo);
        } catch (JSONException e) {
            CLSLog.printStackTrace(e);
        }
        return o;
    }

}
