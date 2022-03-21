package com.tencentcloudapi.cls.producer.common;

import com.google.common.util.concurrent.ListenableFuture;
import com.tencentcloudapi.cls.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.producer.Callback;
import com.tencentcloudapi.cls.producer.Result;
import com.tencentcloudapi.cls.producer.errors.LogSizeTooLargeException;
import com.tencentcloudapi.cls.producer.errors.ProducerException;
import com.tencentcloudapi.cls.producer.errors.TimeoutException;
import com.tencentcloudapi.cls.producer.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author farmerx
 * 日志聚合类库
 */
final public class LogAccumulator {
    private static final AtomicLong BATCH_ID = new AtomicLong(0);

    private final String producerHash;
    private final AsyncProducerConfig producerConfig;
    private final AtomicInteger batchCount;
    private volatile boolean closed;
    private final RetryQueue retryQueue;
    private final BlockingQueue<ProducerBatch> successQueue;
    private final BlockingQueue<ProducerBatch> failureQueue;
    private final AtomicInteger appendsInProgress;
    private final Semaphore memoryController;
    private final SendThreadPool sendThreadPool;
    private final ConcurrentMap<String, ProducerBatchHolder> batches;

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
    }

    private boolean appendsInProgress() {
        return appendsInProgress.get() > 0;
    }

    public LogAccumulator(
            String producerHash,
            AsyncProducerConfig producerConfig,
            Semaphore memoryController,
            RetryQueue retryQueue,
            BlockingQueue<ProducerBatch> successQueue,
            BlockingQueue<ProducerBatch> failureQueue,
            AtomicInteger batchCount,
            SendThreadPool sendThreadPool
    ) {
        this.producerHash = producerHash;
        this.producerConfig = producerConfig;
        this.memoryController = memoryController;
        this.batchCount = batchCount;
        this.retryQueue = retryQueue;
        this.successQueue = successQueue;
        this.failureQueue = failureQueue;
        this.sendThreadPool = sendThreadPool;
        this.appendsInProgress = new AtomicInteger(0);
        this.batches = new ConcurrentHashMap<>();
        this.closed = false;
    }

    /**
     * 合并日志
     * @param topicId topic_id
     * @param logItems 日志集
     * @param callback 回掉函数
     * @return ListenableFuture<Result>
     * @throws InterruptedException error
     * @throws ProducerException error
     */
    public ListenableFuture<Result> append(
            String topicId,
            List<LogItem> logItems,
            Callback callback)
            throws InterruptedException, ProducerException {
        appendsInProgress.incrementAndGet();
        try {
            return doAppend(topicId, logItems, callback);
        } finally {
            appendsInProgress.decrementAndGet();
        }
    }

    private ListenableFuture<Result> doAppend(
            String topicId,
            List<LogItem> logItems,
            Callback callback)
            throws InterruptedException, ProducerException {
        if (closed) {
            throw new IllegalStateException("cannot append after the log accumulator was closed");
        }
        int sizeInBytes = LogSizeCalculator.calculate(logItems);
        ensureValidLogSize(sizeInBytes);
        long maxBlockMs = producerConfig.getMaxBlockMs();
        if (maxBlockMs >= 0) {
            boolean acquired =
                    memoryController.tryAcquire(sizeInBytes, maxBlockMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new TimeoutException(
                        "failed to acquire memory within the configured max blocking time "
                                + producerConfig.getMaxBlockMs()
                                + " ms");
            }
        } else {
            memoryController.acquire(sizeInBytes);
        }
        try {
            ProducerBatchHolder holder = getOrCreateProducerBatchHolder(topicId);
            synchronized (holder) {
                return appendToHolder(topicId, logItems, callback, sizeInBytes, holder);
            }
        } catch (Exception e) {
            memoryController.release(sizeInBytes);
            throw new ProducerException(e);
        }
    }

    private ProducerBatchHolder getOrCreateProducerBatchHolder(String TopicId) {
        ProducerBatchHolder holder = batches.get(TopicId);
        if (holder != null) {
            return holder;
        }
        holder = new ProducerBatchHolder();
        ProducerBatchHolder previous = batches.putIfAbsent(TopicId, holder);
        if (previous == null) {
            return holder;
        } else {
            return previous;
        }
    }

    private ListenableFuture<Result> appendToHolder(
            String topicId,
            List<LogItem> logItems,
            Callback callback,
            int sizeInBytes,
            ProducerBatchHolder holder) {
        if (holder.producerBatch != null) {
            ListenableFuture<Result> f = holder.producerBatch.tryAppend(logItems, sizeInBytes, callback);
            if (f != null) {
                if (holder.producerBatch.isMeetSendCondition()) {
                    holder.transferProducerBatch(
                            sendThreadPool,
                            producerConfig,
                            retryQueue,
                            successQueue,
                            failureQueue,
                            batchCount);
                }
                return f;
            } else {
                holder.transferProducerBatch(
                        sendThreadPool,
                        producerConfig,
                        retryQueue,
                        successQueue,
                        failureQueue,
                        batchCount);
            }
        }
        holder.producerBatch =
                new ProducerBatch(
                        topicId,
                        Utils.generatePackageId(producerHash, BATCH_ID),
                        producerConfig.getBatchSizeThresholdInBytes(),
                        producerConfig.getBatchCountThreshold(),
                        producerConfig.getMaxReservedAttempts(),
                        System.currentTimeMillis());
        ListenableFuture<Result> f = holder.producerBatch.tryAppend(logItems, sizeInBytes, callback);
        batchCount.incrementAndGet();
        if (holder.producerBatch.isMeetSendCondition()) {
            holder.transferProducerBatch(
                    sendThreadPool,
                    producerConfig,
                    retryQueue,
                    successQueue,
                    failureQueue,
                    batchCount);
        }
        return f;
    }

    private void ensureValidLogSize(int sizeInBytes) throws LogSizeTooLargeException {
        if (sizeInBytes > Constants.MAX_BATCH_SIZE_IN_BYTES) {
            throw new LogSizeTooLargeException(
                    "the logs is "
                            + sizeInBytes
                            + " bytes which is larger than MAX_BATCH_SIZE_IN_BYTES "
                            + Constants.MAX_BATCH_SIZE_IN_BYTES);
        }
        if (sizeInBytes > producerConfig.getTotalSizeInBytes()) {
            throw new LogSizeTooLargeException(
                    "the logs is "
                            + sizeInBytes
                            + " bytes which is larger than the totalSizeInBytes you specified");
        }
    }

    public ExpiredBatches expiredBatches() {
        long nowMs = System.currentTimeMillis();
        ExpiredBatches expiredBatches = new ExpiredBatches();
        long remainingMs = producerConfig.getLingerMs();
        for (Map.Entry<String, ProducerBatchHolder> entry : batches.entrySet()) {
            ProducerBatchHolder holder = entry.getValue();
            synchronized (holder) {
                if (holder.producerBatch == null) {
                    continue;
                }
                long curRemainingMs = holder.producerBatch.remainingMs(nowMs, producerConfig.getLingerMs());
                if (curRemainingMs <= 0) {
                    holder.transferProducerBatch(expiredBatches);
                } else {
                    remainingMs = Math.min(remainingMs, curRemainingMs);
                }
            }
        }
        expiredBatches.setRemainingMs(remainingMs);
        return expiredBatches;
    }

    public List<ProducerBatch> remainingBatches() {
        if (!closed) {
            throw new IllegalStateException(
                    "cannot get the remaining batches before the log accumulator closed");
        }
        List<ProducerBatch> remainingBatches = new ArrayList<>();
        while (appendsInProgress()) {
            drainTo(remainingBatches);
        }
        drainTo(remainingBatches);
        batches.clear();
        return remainingBatches;
    }

    private int drainTo(List<ProducerBatch> c) {
        int n = 0;
        for (Map.Entry<String, ProducerBatchHolder> entry : batches.entrySet()) {
            ProducerBatchHolder holder = entry.getValue();
            synchronized (holder) {
                if (holder.producerBatch == null) {
                    continue;
                }
                c.add(holder.producerBatch);
                ++n;
                holder.producerBatch = null;
            }
        }
        return n;
    }

    public static final class ProducerBatchHolder {
        ProducerBatch producerBatch;

        void transferProducerBatch(
                SendThreadPool sendThreadPool,
                AsyncProducerConfig producerConfig,
                RetryQueue retryQueue,
                BlockingQueue<ProducerBatch> successQueue,
                BlockingQueue<ProducerBatch> failureQueue,
                AtomicInteger batchCount) {
            if (producerBatch == null) {
                return;
            }
            sendThreadPool.submit(
                    new SendProducerBatchTask(
                            producerBatch,
                            producerConfig,
                            retryQueue,
                            successQueue,
                            failureQueue,
                            batchCount));
            producerBatch = null;
        }

        void transferProducerBatch(ExpiredBatches expiredBatches) {
            if (producerBatch == null) {
                return;
            }
            expiredBatches.add(producerBatch);
            producerBatch = null;
        }
    }
}

