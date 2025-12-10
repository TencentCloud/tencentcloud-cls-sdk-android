package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build.VERSION;
import android.provider.Settings.System;
import android.util.Log;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.ClsConfigOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    static Application mAapplication;
    static String mNetworkAppId = "";
    static String mAppKey = "";
    static String mUin = "";
    private static String countryId = "CN";
    private static JSONObject policyGeo = null;

    private static ClsConfigOptions mConfig;
    public Utils() {
    }
    static void storeConfig(ClsConfigOptions config) {
        if (config != null) {
            mConfig = config;
        }
    }
    public static ClsConfigOptions getConfig() {
        return mConfig;
    }

    static void storeNetworkAppIdId(String id) {
        if (id != null && !id.equalsIgnoreCase("")) {
            mNetworkAppId = id;
        }
    }
    public static String getNetworkAppId() {
        return mNetworkAppId;
    }

    static void storeAppKey(String key) {
        if (key != null && !key.equalsIgnoreCase("")) {
            mAppKey = key;
        }
    }

    public static String getAppKey() {
        return mAppKey;
    }

    static void storeUin(String uin) {
        if (uin != null && !uin.equalsIgnoreCase("")) {
            mUin = uin;
        }
    }
    public static String getUin() {
        return mUin;
    }

    static void storeApplication(Application application) {
        if (application != null) {
            mAapplication = application;
        }
    }

    public static Application getApplication() {
        return mAapplication;
    }

    public static boolean canWriteSetting() {
        return VERSION.SDK_INT < 23 || System.canWrite(getApplication().getApplicationContext());
    }

    static void setCountryId(String id) {
        if (id != null && !id.equalsIgnoreCase("")) {
            if (!id.equalsIgnoreCase(countryId)) {
                countryId = id;
            }
            CLSLog.d("Utils", "setCountryId: " + id + ", old id:" + countryId);
        }
    }

    static String getCountryId() {
        return countryId;
    }

    static boolean isOversea() {
        return !countryId.equalsIgnoreCase("CN");
    }

    /**
     * 计算float数组的标准差（优化版）
     * @param data 输入数据数组
     * @param isSample 是否为样本标准差（true使用n-1，false使用n）
     * @return 标准差计算结果
     */
    public static double calculateStdDevOptimized(float[] data, boolean isSample) {
        // 1. 检查输入有效性
        if (data == null || data.length == 0) {
            return 0.0;
        }

        // 2. 计算总和和平方和（单次遍历优化）
        double sum = 0.0;
        double sumSquared = 0.0;

        for (float num : data) {
            sum += num;
            sumSquared += num * num;
        }

        int n = data.length;

        // 3. 计算方差（避免两次遍历）
        double variance;
        if (isSample && n > 1) {
            // 样本标准差公式：方差 = [Σx² - (Σx)²/n] / (n-1)
            variance = (sumSquared - sum * sum / n) / (n - 1);
        } else {
            // 总体标准差公式：方差 = [Σx² - (Σx)²/n] / n
            variance = (sumSquared - sum * sum / n) / n;
        }

        // 4. 处理可能的浮点计算误差（确保方差非负）
        variance = Math.max(0.0, variance);

        // 5. 返回标准差（方差的平方根）
        return Math.sqrt(variance);
    }

    public static ArrayList<String> parseDnsStringToJsonArray(String dnsString) {
        ArrayList<String> dnsList = new ArrayList<>();

        if (dnsString != null && !dnsString.trim().isEmpty()) {
            // 去除空格并按逗号分割
            String[] dnsServers = dnsString.split("\\s*,\\s*");

            for (String dns : dnsServers) {
                if (!dns.trim().isEmpty()) {
                    dnsList.add(dns.trim());
                }
            }
        }
        return dnsList;
    }
}
