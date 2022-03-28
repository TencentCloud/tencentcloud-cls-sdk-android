package com.tencentcloudapi.cls.android.utils;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public final class PermissionHelper {
    private PermissionHelper() {
        //no instance
    }

    public static boolean checkPermission(Context context, String permission) {
        if (null == context) {
            return false;
        }

        if (VERSION.SDK_INT >= 23) {
            try {
                Class<?> clazz = Class.forName("android.content.Context");
                Method method = clazz.getMethod("checkSelfPermission", String.class);
                Integer rest = (Integer)method.invoke(context, permission);
                if (null != rest) {
                    return rest == PackageManager.PERMISSION_GRANTED;
                }

                return false;
            } catch (Throwable e) {
                return false;
            }
        } else {
            PackageManager pm = context.getPackageManager();
            return pm.checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        }
    }
}
