package com.tencentcloudapi.cls.producer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.util.concurrent.ListenableFuture;
import com.tencentcloudapi.cls.producer.common.*;
import com.tencentcloudapi.cls.producer.errors.MaxBatchCountExceedException;
import com.tencentcloudapi.cls.producer.errors.ProducerException;
import com.tencentcloudapi.cls.producer.util.Utils;


/**
 * @author farmerx
 */
public class AsyncProducerClient {

    private static final AtomicInteger INSTANCE_ID_GENERATOR = new AtomicInteger(0);
    /**
     * producer config
     */
    private final AsyncProducerConfig producerConfig;

    private final Semaphore memoryController;

    private final RetryQueue retryQueue;

    private final SendThreadPool sendThreadPool;

    private final LogAccumulator accumulator;

    private final TimerSendBatchTask timerSendBatchTask;

    private final BatchHandler successBatchHandler;

    private final BatchHandler failureBatchHandler;

    private final AtomicInteger batchCount = new AtomicInteger(0);

    /**
     * 获取配置文件
     *
     * @return 配置文件
     */
    public AsyncProducerConfig getProducerConfig() {
        return producerConfig;
    }

    /**
     * 获取batch count
     *
     * @return int
     */
    public int getBatchCount() {
        return batchCount.get();
    }

    /**
     * 获取剩余可用内存
     *
     * @return int
     */
    public int availableMemoryInBytes() {
        return memoryController.availablePermits();
    }

    /**
     * 初始化日志上传client
     *
     * @param producerConfig 配置文件
     */
    public AsyncProducerClient(AsyncProducerConfig producerConfig) {
        int instanceId = INSTANCE_ID_GENERATOR.getAndIncrement();
        String name = Constants.LOG_PRODUCER_PREFIX + instanceId;
        String producerHash = Utils.generateProducerHash(instanceId);
        this.producerConfig = producerConfig;
        this.memoryController = new Semaphore(producerConfig.getTotalSizeInBytes());
        this.retryQueue = new RetryQueue();
        BlockingQueue<ProducerBatch> successQueue = new LinkedBlockingQueue<>();
        BlockingQueue<ProducerBatch> failureQueue = new LinkedBlockingQueue<>();
        this.sendThreadPool = new SendThreadPool(producerConfig.getSendThreadCount(), name);

        this.accumulator = new LogAccumulator(
                producerHash,
                producerConfig,
                this.memoryController,
                this.retryQueue,
                successQueue,
                failureQueue,
                this.batchCount,
                this.sendThreadPool
        );

        this.timerSendBatchTask = new TimerSendBatchTask(
                name + Constants.TIMER_SEND_BATCH_TASK_SUFFIX,
                true,
                producerConfig,
                this.accumulator,
                this.retryQueue,
                successQueue,
                failureQueue,
                this.sendThreadPool,
                this.batchCount
        );

        this.successBatchHandler =
                new BatchHandler(
                        name + Constants.SUCCESS_BATCH_HANDLER_SUFFIX, true,
                        successQueue,
                        this.batchCount,
                        this.memoryController);
        this.failureBatchHandler =
                new BatchHandler(
                        name + Constants.FAILURE_BATCH_HANDLER_SUFFIX, true,
                        failureQueue,
                        this.batchCount,
                        this.memoryController);

        this.timerSendBatchTask.start();
        this.successBatchHandler.start();
        this.failureBatchHandler.start();
    }

    /**
     * 写入日志
     *
     * @param topicId  Tencent Cloud CLS Topic ID
     * @param logItems 日志集合
     * @param callback 回掉函数
     * @return ListenableFuture<Result>
     * @throws InterruptedException error
     * @throws ProducerException    error
     */
    public ListenableFuture<Result> putLogs(
            String topicId,
            List<LogItem> logItems,
            Callback callback)
            throws InterruptedException, ProducerException {
        if (topicId == null || topicId.isEmpty()) {
            throw new IllegalArgumentException("topicIDInvalid", new Exception("topic id cannot be empty"));
        }
        if (logItems.isEmpty()) {
            throw new IllegalArgumentException("logItems cannot be empty");
        }
        int count = logItems.size();
        if (count > Constants.MAX_BATCH_COUNT) {
            throw new MaxBatchCountExceedException(
                    "the log list size is "
                            + count
                            + " which exceeds the MAX_BATCH_COUNT "
                            + Constants.MAX_BATCH_COUNT);
        }
        return accumulator.append(topicId, logItems, callback);
    }

    public void close() throws InterruptedException, ProducerException {
        close(Long.MAX_VALUE);
    }

    public void close(long timeoutMs) throws InterruptedException, ProducerException {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException(
                    "timeoutMs must be greater than or equal to 0, got " + timeoutMs);
        }
        ProducerException firstException = null;
        try {
            timeoutMs = closeTimerSendBatchTask(timeoutMs);
            timeoutMs = closeSendThreadPool(timeoutMs);
            timeoutMs = closeSuccessBatchHandler(timeoutMs);
            closeFailureBatchHandler(timeoutMs);
        } catch (ProducerException e) {
            firstException = e;
        }
        if (firstException != null) {
            throw firstException;
        }
    }

    private long closeTimerSendBatchTask(long timeoutMs) throws InterruptedException, ProducerException {
        long startMs = System.currentTimeMillis();
        accumulator.close();
        retryQueue.close();

        timerSendBatchTask.close();
        timerSendBatchTask.join(timeoutMs);
        if (timerSendBatchTask.isAlive()) {
            throw new ProducerException("the mover thread is still alive");
        }
        long nowMs = System.currentTimeMillis();
        return Math.max(0, timeoutMs - nowMs + startMs);
    }

    private long closeSendThreadPool(long timeoutMs) throws InterruptedException, ProducerException {
        long startMs = System.currentTimeMillis();
        sendThreadPool.shutdown();
        if (!sendThreadPool.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new ProducerException("the ioThreadPool is not fully terminated");
        }
        long nowMs = System.currentTimeMillis();
        return Math.max(0, timeoutMs - nowMs + startMs);
    }

    private long closeSuccessBatchHandler(long timeoutMs) throws InterruptedException, ProducerException {
        long startMs = System.currentTimeMillis();
        successBatchHandler.close();
        boolean invokedFromCallback = Thread.currentThread() == this.successBatchHandler;
        if (invokedFromCallback) {
            return timeoutMs;
        }
        successBatchHandler.join(timeoutMs);
        if (successBatchHandler.isAlive()) {
            throw new ProducerException("the success batch handler thread is still alive");
        }
        long nowMs = System.currentTimeMillis();
        return Math.max(0, timeoutMs - nowMs + startMs);
    }

    private long closeFailureBatchHandler(long timeoutMs) throws InterruptedException, ProducerException {
        long startMs = System.currentTimeMillis();
        failureBatchHandler.close();
        boolean invokedFromCallback =
                Thread.currentThread() == this.successBatchHandler
                        || Thread.currentThread() == this.failureBatchHandler;
        if (invokedFromCallback) {
            return timeoutMs;
        }
        failureBatchHandler.join(timeoutMs);
        if (failureBatchHandler.isAlive()) {
            throw new ProducerException("the failure batch handler thread is still alive");
        }
        long nowMs = System.currentTimeMillis();
        return Math.max(0, timeoutMs - nowMs + startMs);
    }

}
