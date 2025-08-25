//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.tencentcloudapi.cls.android.scheme;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.utdid.Utdid;

/**
 * Utils set for get device info.
 *
 * @author farmerx
 * @date 2022/03/10
 */
class DeviceUtils {

    public static final String COMMAND_HARMONYOS_VERSION = "getprop hw_sc.build.platform.version";
    private static final String TAG = "DeviceUtils";

    public static final String NETWORK_CLASS_WIFI = "Wi-Fi";
    private static final String NETWORK_CLASS_2_G = "2G";
    private static final String NETWORK_CLASS_3_G = "3G";
    private static final String NETWORK_CLASS_4_G = "4G";
    private static final String NETWORK_CLASS_UNKNOWN = "Unknown";
    private static String cpuName = null;
    private static final String[] NETWORK_INFO_DEFAULT = new String[] {"Unknown", "Unknown"};
    private static final String[] NETWORK_INFO = new String[] {"Unknown", "Unknown"};
    private static String imsi = null;
    private static String imei = null;

    private DeviceUtils() {
    }

    public static String getCpuName() {
        if (cpuName != null) {
            return cpuName;
        } else {
            String str1 = "/proc/cpuinfo";
            String str2 = "";
            FileReader fr = null;
            BufferedReader localBufferedReader = null;

            try {
                fr = new FileReader(str1);
                localBufferedReader = new BufferedReader(fr);

                while ((str2 = localBufferedReader.readLine()) != null) {
                    if (str2.contains("Hardware")) {
                        cpuName = str2.split(":")[1];
                        String var4 = cpuName;
                        return var4;
                    }
                }
            } catch (IOException var15) {
            } finally {
                try {
                    if (fr != null) {
                        fr.close();
                    }

                    if (localBufferedReader != null) {
                        localBufferedReader.close();
                    }
                } catch (Exception var14) {
                }

            }

            return null;
        }
    }

    public static String getCarrier(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(
                Context.TELEPHONY_SERVICE);
            return telephonyManager.getNetworkOperatorName();
        } catch (Exception var2) {
            return null;
        }
    }

    public static String getAccessName(Context context) {
        return DeviceUtils.getNetworkType(context)[0];
    }

    public static String getAccessSubTypeName(Context context) {
        String[] networkStatus = DeviceUtils.getNetworkType(context);
        String accessName = networkStatus[0];
        if (networkStatus.length > 1 && accessName != null && !"Wi-Fi".equals(accessName)) {
            return networkStatus[1];
        }

        return NETWORK_INFO_DEFAULT[1];
    }

    @SuppressLint({"WrongConstant"})
    public static String[] getNetworkType(Context context) {
        if (context == null) {
            return NETWORK_INFO_DEFAULT;
        } else {
            try {
                ConnectivityManager cManager = (ConnectivityManager)context.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
                if (cManager == null) {
                    return NETWORK_INFO_DEFAULT;
                }

                NetworkInfo nInfo = cManager.getActiveNetworkInfo();
                if (nInfo == null) {
                    return NETWORK_INFO_DEFAULT;
                }

                if (nInfo.isConnected()) {
                    if (nInfo.getType() == 1) {
                        NETWORK_INFO[0] = "Wi-Fi";
                        return NETWORK_INFO;
                    }

                    if (nInfo.getType() == 0) {
                        NETWORK_INFO[0] = getNetworkClass(nInfo.getSubtype());
                        NETWORK_INFO[1] = nInfo.getSubtypeName();
                        return NETWORK_INFO;
                    }
                }
            } catch (Throwable var4) {
                // ignore
            }

            return NETWORK_INFO_DEFAULT;
        }
    }

    private static String getNetworkClass(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
//            case TelephonyManager.NETWORK_TYPE_GSM:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
//            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
//            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "4G";
//            case TelephonyManager.NETWORK_TYPE_NR:
//                return "5G";
            default:
                return "Unknown";
        }
    }

    public static String getLanguage() {
        try {
            return Locale.getDefault().getLanguage();
        } catch (Exception var1) {
            CLSLog.e(TAG, "get country error: " + var1.getMessage());
            return null;
        }
    }

    public static String getCountry() {
        try {
            return Locale.getDefault().getCountry();
        } catch (Exception var1) {
            CLSLog.e(TAG, "get country error: " + var1.getMessage());
            return null;
        }
    }

    public static String getResolution(Context context) {
        String resolution = "Unknown";

        try {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int width = dm.widthPixels;
            int height = dm.heightPixels;
            if (width > height) {
                width ^= height;
                height ^= width;
                width ^= height;
            }

            resolution = height + "*" + width;
        } catch (Exception var5) {
            CLSLog.e(TAG, "DeviceUtils getResolution: error: " + var5.getMessage());
        }

        return resolution;
    }

    public static String getUtdid(Context context) {
        try {
            return Utdid.getInstance().getUtdid(context);
        } catch (Exception var2) {
            return "";
        }
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getImsi(Context context) {
        if (imsi != null) {
            return imsi;
        } else {
            imsi = Utdid.getImsi(context);
            return imsi;
        }
    }

    public static String getImei(Context context) {
        if (imei != null) {
            return imei;
        } else {
            imei = Utdid.getImei(context);
            return imei;
        }
    }

    public static String getDns(Context context) {
        String[] dnsServers = getDnsFromCommand();
        if (dnsServers == null || dnsServers.length == 0) {
            dnsServers = getDnsFromConnectionManager(context);
        }
        StringBuffer sb = new StringBuffer();
        if (dnsServers != null) {
            // 使用for-each循环遍历数组并输出每个元素
            for (String str : dnsServers) {
                sb.append(str).append(",");
            }
        }
        String res = sb.toString();
        if (res.length() > 0) {
            return res.substring(0, res.length() - 1);
        }
        return "-";
    }

    //通过 getprop 命令获取
    private static String[] getDnsFromCommand() {
        LinkedList<String> dnsServers = new LinkedList<>();
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            InputStream inputStream = process.getInputStream();
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = lnr.readLine()) != null) {
                int split = line.indexOf("]: [");
                if (split == -1) continue;
                String property = line.substring(1, split);
                String value = line.substring(split + 4, line.length() - 1);
                if (property.endsWith(".dns")
                        || property.endsWith(".dns1")
                        || property.endsWith(".dns2")
                        || property.endsWith(".dns3")
                        || property.endsWith(".dns4")) {
                    InetAddress ip = InetAddress.getByName(value);
                    if (ip == null) continue;
                    value = ip.getHostAddress();
                    if (value == null) continue;
                    if (value.length() == 0) continue;
                    dnsServers.add(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dnsServers.isEmpty() ? new String[0] : dnsServers.toArray(new String[dnsServers.size()]);
    }


    private static String[] getDnsFromConnectionManager(Context context) {
        LinkedList<String> dnsServers = new LinkedList<>();
        try {
            if (Build.VERSION.SDK_INT >= 21 && context != null) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null) {
                        for (Network network : connectivityManager.getAllNetworks()) {
                            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                            if (networkInfo != null && networkInfo.getType() == activeNetworkInfo.getType()) {
                                LinkProperties lp = connectivityManager.getLinkProperties(network);
                                for (InetAddress addr : lp.getDnsServers()) {
                                    dnsServers.add(addr.getHostAddress());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dnsServers.isEmpty() ? new String[0] : dnsServers.toArray(new String[dnsServers.size()]);
    }

    public static String getOS() {
        return Build.VERSION.RELEASE == null ? "Unknown" : Build.VERSION.RELEASE;
    }

    public static String getBrand() {
        try {
            String brand = Build.BRAND;
            if (brand != null) {
                return brand.trim().toUpperCase();
            }
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return "Unknown";
    }

    /**
     * 获取鸿蒙系统 Version
     *
     * @return HarmonyOS Version
     */
    public static String getHarmonyOSVersion() {
        String version = null;
        if (isHarmonyOs()) {
            version = getProp("hw_sc.build.platform.version", "");
            if (TextUtils.isEmpty(version)) {
                version = exec(COMMAND_HARMONYOS_VERSION);
            }
        }
        return version;
    }

    /**
     * 判断当前是否为鸿蒙系统
     *
     * @return 是否是鸿蒙系统，是：true，不是：false
     */
    private static boolean isHarmonyOs() {
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            if (osBrand == null) {
                return false;
            }
            return "harmony".equalsIgnoreCase(osBrand.toString());
        } catch (Throwable e) {
            CLSLog.i("CLS.HasHarmonyOS", e.getMessage());
            return false;
        }
    }

    private static String getProp(String property, String defaultValue) {
        try {
            Class spClz = Class.forName("android.os.SystemProperties");
            Method method = spClz.getDeclaredMethod("get", String.class);
            String value = (String) method.invoke(spClz, property);
            if (TextUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (Throwable throwable) {
            CLSLog.i("CLS.SystemProperties", throwable.getMessage());
        }
        return defaultValue;
    }

    /**
     * 执行命令获取对应内容
     *
     * @param command 命令
     * @return 命令返回内容
     */
    public static String exec(String command) {
        InputStreamReader ir = null;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(command);
            ir = new InputStreamReader(process.getInputStream());
            input = new BufferedReader(ir);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = input.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            CLSLog.i("CLS.Exec", e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable e) {
                    CLSLog.i("CLS.Exec", e.getMessage());
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException e) {
                    CLSLog.i("CLS.Exec", e.getMessage());
                }
            }
        }
        return null;
    }
}
