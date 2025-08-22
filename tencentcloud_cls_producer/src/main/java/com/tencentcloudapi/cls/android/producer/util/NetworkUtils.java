package com.tencentcloudapi.cls.android.producer.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.common.Constants;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * @author farmerx
 */
public final class NetworkUtils {

    private NetworkUtils() {
    }

    /**
     * Checks if the specified string is a valid IP address.
     *
     * @param ipAddress the string to check
     * @return true if the string validates as an IP address
     */
    public static boolean isIPAddr(final String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        try {
            final String[] tokens = ipAddress.split("\\.");
            if (tokens.length != 4) {
                return false;
            }
            for (String token : tokens) {
                int i = Integer.parseInt(token);
                if (i < 0 || i > 255) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Get the IP address of current machine.
     *
     * @return An IP address or {@code null} if unable to get the IP address
     */
    public static String getLocalMachineIP() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress address = addresses.nextElement();
                    if (!address.isLinkLocalAddress() && address.getHostAddress() != null) {
                        String ipAddress = address.getHostAddress();
                        if (ipAddress.equals(Constants.CONST_LOCAL_IP)) {
                            continue;
                        }
                        if (isIPAddr(ipAddress)) {
                            return ipAddress;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            // swallow it
        }
        return null;
    }

    /**
     * 是否有可用网络
     *
     * @param context Context
     * @return true：网络可用，false：网络不可用
     */
    @SuppressLint("WrongConstant")
    public static boolean isNetworkAvailable(Context context) {
        // 检测权限
        if (!PermissionUtils.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return isNetworkAvailable(cm);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            return false;
        }
    }

    /**
     * 判断网络是否可用
     *
     * @param connectivityManager ConnectivityManager
     * @return true：可用；false：不可用
     */
    private static boolean isNetworkAvailable(ConnectivityManager connectivityManager) {
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    if (capabilities != null) {
                        return isNetworkValid(capabilities);
                    }
                }
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }

    @SuppressLint("WrongConstant")
    public static boolean isNetworkValid(NetworkCapabilities capabilities) {
        if (capabilities != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || capabilities.hasTransport(7)  //目前已知在车联网行业使用该标记作为网络类型（TBOX 网络类型）
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
        return false;
    }
}
