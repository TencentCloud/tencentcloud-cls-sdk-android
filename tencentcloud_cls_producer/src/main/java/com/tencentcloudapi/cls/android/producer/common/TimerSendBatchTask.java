package com.tencentcloudapi.cls.android.producer.common;

import com.tencentcloudapi.cls.android.producer.AsyncProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author farmerx
 */
public class TimerSendBatchTask extends LogThread{

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerSendBatchTask.class);

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
                LOGGER.error("Uncaught exception in timer batch send task, e=", e);
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
