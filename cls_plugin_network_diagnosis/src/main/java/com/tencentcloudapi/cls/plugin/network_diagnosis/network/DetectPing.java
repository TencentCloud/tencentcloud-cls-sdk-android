package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.net.Network;

import com.tencentcloudapi.cls.android.CLSLog;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Locale;


public class DetectPing {
    private static final String TAG = "DetectPing";

    DetectPing() {
    }

    private String getIp(String host) throws Exception {
        InetAddress i = InetAddress.getByName(host);
        return i.getHostAddress();
    }

    public JSONObject doDetectPing(String taskId, String connectionType, Network network, JSONObject netInfo, PingConfig config) {
        String ip;
        try {
            InetAddress i = InetAddress.getByName(config.domain);
            boolean isReachable = i.isReachable(config.timeout);
            if (isReachable) {
                ip = i.getHostAddress();
            } else {
                return null;
            }
        } catch (IOException e) {
            CLSLog.printStackTrace(e);
            return null;
        }
        JSONObject result = new JSONObject();
        return result;
    }
}
