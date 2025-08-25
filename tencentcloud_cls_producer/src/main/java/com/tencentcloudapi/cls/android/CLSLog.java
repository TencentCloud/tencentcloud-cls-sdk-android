package com.tencentcloudapi.cls.android;

import android.util.Log;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public final class CLSLog {
    private static final String TAG = "CLSAndroid";
    private static boolean enableLog;

    /**
     * 设置是否打印 Log
     *
     * @param isEnableLog Log 状态
     */
    public static void setEnableLog(boolean isEnableLog) {
        enableLog = isEnableLog;
    }


    public static void v(String module, Object msg) {
        if (enableLog) {
            Log.v(TAG, format(module, msg));
        }
    }

    public static void d(String module, Object msg) {
        if (enableLog) {
            Log.d(TAG, format(module, msg));
        }
    }

    public static void w(String module, Object msg) {
        if (enableLog) {
            Log.w(TAG, format(module, msg));
        }

    }

    public static void e(String module, Object msg) {
        if (enableLog) {
            Log.e(TAG, format(module, msg));
        }
    }

    public static void i(String module, Object msg) {
        if (enableLog) {
            Log.i(TAG, format(module, msg));
        }
    }
    public static void printStackTrace(Exception e) {
        if (enableLog && e != null) {
            Log.e("SA.Exception", "", e);
        }

    }

    private static String format(String module, Object msg) {
        return String.format("module: %s, %s", module, toString(msg));
    }

    public static String format(String format, Object... args) {
        return String.format(format, args);
    }

    public static String toString(Object obj) {
        if (null == obj) {
            return "null";
        }

        if (obj instanceof String) {
            return (String)obj;
        }

        if (obj instanceof Number) {
            return obj.toString();
        }

        if (obj instanceof Boolean) {
            return String.valueOf(obj);
        }

        return obj.toString();
    }

}
