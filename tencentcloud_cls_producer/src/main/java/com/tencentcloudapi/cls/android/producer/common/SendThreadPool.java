package com.tencentcloudapi.cls.android.producer.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author farmerx
 */
public class SendThreadPool {
    private static final String SEND_THREAD_SUFFIX_FORMAT = "-send-thread-%d";
    private final ExecutorService sendThreadPool;

    public SendThreadPool(int threadCount, String prefix) {
        this.sendThreadPool =
                Executors.newFixedThreadPool(
                        threadCount,
                        new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat(prefix + SEND_THREAD_SUFFIX_FORMAT)
                                .build()
                );
    }

    public void submit(SendProducerBatchTask task) {
        sendThreadPool.submit(task);
    }

    public void shutdown() {
        sendThreadPool.shutdown();
    }

    public boolean isTerminated() {
        return sendThreadPool.isTerminated();
    }

    // 优雅关闭
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return sendThreadPool.awaitTermination(timeout, unit);
    }
}
