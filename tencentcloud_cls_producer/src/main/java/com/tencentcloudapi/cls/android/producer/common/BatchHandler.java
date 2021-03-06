package com.tencentcloudapi.cls.android.producer.common;

import com.tencentcloudapi.cls.android.CLSLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author farmerx
 */
public class BatchHandler extends LogThread {

    private final BlockingQueue<ProducerBatch> batches;

    private final AtomicInteger batchCount;

    private final Semaphore memoryController;

    private volatile boolean closed;

    public BatchHandler(
            String name,
            boolean daemon,
            BlockingQueue<ProducerBatch>batches,
            AtomicInteger batchCount,
            Semaphore memoryController) {
        super(name, daemon);
        this.batches = batches;
        this.batchCount = batchCount;
        this.memoryController = memoryController;
        this.closed = false;
    }


    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
        interrupt();
    }

    @Override
    public void run() {
        loopHandleBatches();
        handleRemainingBatches();
    }

    private void loopHandleBatches() {
        while (!closed) {
            try {
                ProducerBatch b = batches.take();
                handle(b);
            } catch (InterruptedException e) {
                CLSLog.w("producer", "The batch handler has been interrupted");
            }
        }
    }

    private void handleRemainingBatches() {
        List<ProducerBatch> remainingBatches = new ArrayList<ProducerBatch>();
        batches.drainTo(remainingBatches);
        for (ProducerBatch b : remainingBatches) {
            handle(b);
        }
    }

    private void handle(ProducerBatch batch) {
        try {
            batch.fireCallbacksAndSetFutures();
        } catch (Throwable t) {
            CLSLog.e("producer",CLSLog.format("Failed to handle batch, e=%s", t.getMessage()));
        } finally {
            batchCount.decrementAndGet();
            memoryController.release(batch.getCurBatchSizeInBytes());
        }
    }
}
