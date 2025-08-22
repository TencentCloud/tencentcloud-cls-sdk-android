package com.tencentcloudapi.cls.android.producer;

import com.tencentcloudapi.cls.android.CLSLog;

import java.util.concurrent.LinkedBlockingQueue;

public class TrackTaskManager {
    private static TrackTaskManager trackTaskManager;
    /**
     * 请求线程队列
     */
    private final LinkedBlockingQueue<Runnable> mTrackEventTasks;

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedBlockingQueue<>();
    }

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
            }
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return trackTaskManager;
    }

    public void addTrackEventTask(Runnable trackEvenTask) {
        try {
            mTrackEventTasks.put(trackEvenTask);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    public Runnable takeTrackEventTask() {
        try {
            return mTrackEventTasks.take();
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return null;
    }

    public Runnable pollTrackEventTask() {
        try {
            return mTrackEventTasks.poll();
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return null;
    }

    public boolean isEmpty(){
        return mTrackEventTasks.isEmpty();
    }
}
