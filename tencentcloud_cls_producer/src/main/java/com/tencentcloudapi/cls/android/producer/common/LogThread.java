package com.tencentcloudapi.cls.android.producer.common;


import com.tencentcloudapi.cls.android.CLSLog;

/**
 * @author farmerx
 */
public class LogThread extends Thread {

    public static LogThread daemon(final String name, Runnable runnable) {
        return new LogThread(name, runnable, true);
    }

    public static LogThread nonDaemon(final String name, Runnable runnable) {
        return new LogThread(name, runnable, false);
    }

    public LogThread(final String name, boolean daemon) {
        super(name);
        configureThread(name, daemon);
    }

    public LogThread(final String name, Runnable runnable, boolean daemon) {
        super(runnable, name);
        configureThread(name, daemon);
    }

    private void configureThread(final String name, boolean daemon) {
        setDaemon(daemon);
        setUncaughtExceptionHandler( new UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                CLSLog.e("producer", CLSLog.format("Uncaught error in thread, name=%s, e=%s", name, e.getMessage()));
            }
        });
    }
}
