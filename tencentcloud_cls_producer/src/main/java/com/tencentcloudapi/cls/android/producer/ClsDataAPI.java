package com.tencentcloudapi.cls.android.producer;

import android.content.Context;
import android.util.Base64;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.common.LogItem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClsDataAPI {
    protected static final Map<Context, ClsDataAPI> sInstanceMap = new ConcurrentHashMap<>();
    private static final AtomicInteger INSTANCE_ID_GENERATOR = new AtomicInteger(0);
    protected static ClsConfigOptions mClsConfigOptions;
    private EventMessages mMessages;

    protected TrackTaskManager mTrackTaskManager;
    protected TrackTaskManagerThread mTrackTaskManagerThread;

    public ClsConfigOptions getClsConfigOptions() {
        return mClsConfigOptions;
    }
    /**
     * 初始化CLS SDK
     *
     * @param context App 的 Context
     * @param clsConfigOptions SDK 的配置项
     */
    public static void startWithConfigOptions(Context context, ClsConfigOptions clsConfigOptions) {
        try {
            getInstance(context, clsConfigOptions);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    private synchronized static void getInstance(Context context, ClsConfigOptions clsConfigOptions) {
        if (context == null || clsConfigOptions == null) {
            throw new NullPointerException("Context、ClsConfigOptions can not be null");
        }
        final Context appContext = context.getApplicationContext();
        ClsDataAPI instance = sInstanceMap.get(appContext);
        if (null == instance) {
            instance = new ClsDataAPI(context, clsConfigOptions);
            sInstanceMap.put(appContext, instance);
        }
    }

    /**
     * 初始化日志上传client
     *
     * @param context 上下文
     * @param clsConfigOptions 配置文件
     */
    ClsDataAPI(Context context, ClsConfigOptions clsConfigOptions) {
        mClsConfigOptions = clsConfigOptions;
        if (mClsConfigOptions.isLogEnabled()) {
            CLSLog.setEnableLog(true);
        }
        this.mMessages = EventMessages.getInstance(context, mClsConfigOptions);
        this.mMessages.flushScheduled();
        mTrackTaskManager = TrackTaskManager.getInstance();
        mTrackTaskManagerThread = new TrackTaskManagerThread();
        new Thread(mTrackTaskManagerThread, "CLS.TaskQueueThread").start();
    }

    /**
     * 获取 ClsDataAPI 单例
     *
     * @param context App的Context
     * @return ClsDataAPI 单例
     */
    public static ClsDataAPI sharedInstance(Context context) {
        try {
            if (null == context) {
                throw new RuntimeException("Context can not be null");
            }

            final Context appContext = context.getApplicationContext();
            ClsDataAPI instance = sInstanceMap.get(appContext);
            if (null == instance) {
                throw new RuntimeException("The static method sharedInstance(context) should be called before calling startWithConfigOptions()");
            }
            return instance;
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            throw e;
        }
    }

    public void trackLog(LogItem logItem) {
        try {
            String data = Base64.encodeToString(logItem.mContents.build().toByteArray(), Base64.DEFAULT);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        int ret = mMessages.addLogEvent(data);
                        if (ret < 0) {
                            String error = "Failed to enqueue the event: " + data;
                            CLSLog.i("CLSDataAPI trackLog", error);
                        }
                    } catch (Exception e) {
                        CLSLog.printStackTrace(e);
                    }
                }
            });

        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    public void flush() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                mMessages.flush();
            }
        });
    }



    public void stopTrackThread() {
        if (mTrackTaskManagerThread != null && !mTrackTaskManagerThread.isStopped()) {
            mTrackTaskManagerThread.stop();
            CLSLog.i("CLSDataAPI", "Data collection thread has been stopped");
        }
    }

    public void close() {
        stopTrackThread();
        if (mMessages!=null && !mMessages.isStopped()){
            mMessages.stop();
        }
    }

    public void deleteAll() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                mMessages.deleteAll();
            }
        });
    }

}
