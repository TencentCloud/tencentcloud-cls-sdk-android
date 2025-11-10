package com.tencentcloudapi.cls.android.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;

import com.tencentcloudapi.cls.android.CLSLog;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public class AppUtils {
    private static String packageName;
    private static String appVersion;
    private static String appName;
    private static boolean isForeground = false;
    private static Integer appVersionCode = null;
    private static String topActivity = null;

    private AppUtils() {
        //no instance
    }

    public static String getAppVersion(Context context) {
        if (context == null) return "";
        if (!TextUtils.isEmpty(appVersion)) {
            return appVersion;
        }
        try {
            final PackageInfo info = getPackageInfo(context);
            if (null != info) {
                appVersionCode = info.versionCode;
                appVersion = info.versionName;
            }
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return appVersion;
    }


    public static int getAppVersionCode(Context context) {
        if (null != appVersionCode) {
            return appVersionCode;
        }

        final PackageInfo info = getPackageInfo(context);
        if (null != info) {
            appVersion = info.versionName;
            return appVersionCode = info.versionCode;
        }

        return 0;
    }

    private static PackageInfo getPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static String getAppName(Context context) {
        if (context == null) return "";
        if (!TextUtils.isEmpty(appName)) {
            return appName;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            appName = appInfo.loadLabel(packageManager).toString();
            return appName;
        } catch (Throwable e) {
            CLSLog.i("CLS.AppInfoUtils", e.getMessage());
        }
        return "";
    }

    public static boolean isForeground() {
        return isForeground;
    }

    public static void setForeground(boolean foreground) {
        AppUtils.isForeground = foreground;
    }

    public static String getTopActivity() {
        return topActivity;
    }

    public static void setTopActivity(String topActivity) {
        AppUtils.topActivity = topActivity;
    }
}
