package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class Utils {
    static void runInMain(Runnable r) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(r);
    }

    static void runInBack(Runnable r) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            AsyncTask.execute(r);
        } else {
            new Thread(r).start();
        }
    }

     static String getIp(String host) throws UnknownHostException {
        InetAddress i = InetAddress.getByName(host);
        return i.getHostAddress();
    }

    public static void closeAllCloseable(Closeable... closeables) {
        if (closeables == null || closeables.length <= 0)
            return;

        for (Closeable closeable : closeables) {
            if (closeable != null)
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
