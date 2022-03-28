package com.tencentcloudapi.cls.android.producer.common;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.AsyncProducerConfig;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author farmerx
 */
public class TimerSendBatchTask extends LogThread{

    private final AsyncProducerConfig producerConfig;

    private final LogAccumulator accumulator;

    private final RetryQueue retryQueue;

    private final BlockingQueue<ProducerBatch> successQueue;

    private final BlockingQueue<ProducerBatch> failureQueue;

    private final SendThreadPool sendThreadPool;

    private final AtomicInteger batchCount;

    private volatile boolean closed;

    public TimerSendBatchTask(
            String name,
            boolean daemon,
            AsyncProducerConfig producerConfig,
            LogAccumulator accumulator,
            RetryQueue retryQueue,
            BlockingQueue<ProducerBatch> successQueue,
            BlockingQueue<ProducerBatch> failureQueue,
            SendThreadPool sendThreadPool,
            AtomicInteger batchCount) {
        super(name, daemon);
        this.producerConfig = producerConfig;
        this.accumulator = accumulator;
        this.retryQueue = retryQueue;
        this.successQueue = successQueue;
        this.failureQueue = failureQueue;
        this.sendThreadPool = sendThreadPool;
        this.batchCount = batchCount;
        this.closed = false;
    }

    public void close() {
        this.closed = true;
        interrupt();
    }

    @Override
    public void run() {
        loopCheckSendBatches();
        List<ProducerBatch> incompleteBatches = incompleteBatches();
        submitIncompleteBatches(incompleteBatches);
    }

    private void loopCheckSendBatches() {
        while (!closed) {
            try {
                doSendBatches();
            } catch (Exception e) {
                CLSLog.e("producer", CLSLog.format("Uncaught exception in timer batch send task, e=%s", e.getMessage()));
            }
        }
    }

    private void doSendBatches() {
        ExpiredBatches expiredBatches = accumulator.expiredBatches();
        for (ProducerBatch b : expiredBatches.getBatches()) {
            sendThreadPool.submit(createSendProducerBatchTask(b));
        }
        List<ProducerBatch> expiredRetryBatches =
                retryQueue.expiredBatches(expiredBatches.getRemainingMs());

        for (ProducerBatch b : expiredRetryBatches) {
            sendThreadPool.submit(createSendProducerBatchTask(b));
        }
    }

    private SendProducerBatchTask createSendProducerBatchTask(ProducerBatch batch) {
        return new SendProducerBatchTask(
                batch, producerConfig, retryQueue, successQueue, failureQueue, batchCount);
    }

    private List<ProducerBatch> incompleteBatches() {
        List<ProducerBatch> incompleteBatches = accumulator.remainingBatches();
        incompleteBatches.addAll(retryQueue.remainingBatches());
        return incompleteBatches;
    }

    private void submitIncompleteBatches(List<ProducerBatch> incompleteBatches) {
        for (ProducerBatch b : incompleteBatches) {
            sendThreadPool.submit(createSendProducerBatchTask(b));
        }
    }
}
