package com.tencentcloudapi.cls.android.producer.common;


import com.tencentcloudapi.cls.android.CLSLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryQueue {

    private final DelayQueue<ProducerBatch> retryBatches = new DelayQueue<>();

    private final AtomicInteger putsInProgress;

    private volatile boolean closed;

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
    }

    private boolean putsInProgress() {
        return putsInProgress.get() > 0;
    }

    public RetryQueue() {
        this.putsInProgress = new AtomicInteger(0);
        this.closed = false;
    }

    public void put(ProducerBatch batch) {
        putsInProgress.incrementAndGet();
        try {
            if (closed) {
                throw new IllegalStateException("cannot put after the retry queue was closed");
            }
            retryBatches.put(batch);
        } finally {
            putsInProgress.decrementAndGet();
        }
    }

    /**
     * 获取过去Producer batch
     * @param timeoutMs 过期时间
     * @return List<ProducerBatch>
     */
    public List<ProducerBatch> expiredBatches(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        List<ProducerBatch> expiredBatches = new ArrayList<>();
        retryBatches.drainTo(expiredBatches);
        if (!expiredBatches.isEmpty()) {
            return expiredBatches;
        }
        while (true) {
            if (timeoutMs < 0) {
                break;
            }
            ProducerBatch batch;
            try {
                batch = retryBatches.poll(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                CLSLog.w("producer", "Interrupted when poll batch from the retry batches");
                break;
            }
            if (batch == null) {
                break;
            }
            expiredBatches.add(batch);
            retryBatches.drainTo(expiredBatches);
            if (!expiredBatches.isEmpty()) {
                break;
            }
            timeoutMs = deadline - System.currentTimeMillis();
        }
        return expiredBatches;
    }

    /**
     * 取出积压的所有batch
     * @return List<ProducerBatch>
     */
    public List<ProducerBatch> remainingBatches() {
        if (!closed) {
            throw new IllegalStateException(
                    "cannot get the remaining batches before the retry queue closed");
        }
        while (true) {
            if (!putsInProgress()) {
                break;
            }
        }
        List<ProducerBatch> remainingBatches = new ArrayList<>(retryBatches);
        retryBatches.clear();
        return remainingBatches;
    }
}
