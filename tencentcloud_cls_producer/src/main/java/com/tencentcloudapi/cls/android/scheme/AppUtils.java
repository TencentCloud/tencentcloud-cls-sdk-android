package com.tencentcloudapi.cls.android.scheme;

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

    private AppUtils() {
        //no instance
    }

    public static String getAppVersion(Context context) {
        if (context == null) return "";
        if (!TextUtils.isEmpty(appVersion)) {
            return appVersion;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            appVersion = packageInfo.versionName;
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return appVersion;
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
            appVersion = appInfo.loadLabel(packageManager).toString();
            return appVersion;
        } catch (Throwable e) {
            CLSLog.i("SA.AppInfoUtils", e.getMessage());
        }
        return "";
    }
}
