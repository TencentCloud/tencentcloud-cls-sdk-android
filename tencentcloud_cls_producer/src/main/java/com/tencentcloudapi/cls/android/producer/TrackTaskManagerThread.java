package com.tencentcloudapi.cls.android.producer;

import com.tencentcloudapi.cls.android.CLSLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TrackTaskManagerThread implements Runnable {
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private static final int POOL_SIZE = 1;

    private TrackTaskManager mTrackTaskManager;
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private ExecutorService mPool;
    /**
     * 是否停止
     */
    private boolean isStop = false;

    public TrackTaskManagerThread() {
        try {
            this.mTrackTaskManager = TrackTaskManager.getInstance();
            mPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "ClsTrackTaskExecuteThread");
                }
            });
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!isStop) {
                Runnable downloadTask = mTrackTaskManager.takeTrackEventTask();
                mPool.execute(downloadTask);
            }
            while (true) {
                Runnable downloadTask = mTrackTaskManager.pollTrackEventTask();
                if (downloadTask == null) {
                    break;
                }
                mPool.execute(downloadTask);
            }
            mPool.shutdown();
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    public void stop() {
        isStop = true;
        //解决队列阻塞时,停止队列还会触发一次事件
        if (mTrackTaskManager.isEmpty()) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    public boolean isStopped() {
        return isStop;
    }
}
