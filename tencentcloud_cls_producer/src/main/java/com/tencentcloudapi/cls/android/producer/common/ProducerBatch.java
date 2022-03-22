package com.tencentcloudapi.cls.android.producer.common;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.tencentcloudapi.cls.android.producer.Callback;
import com.tencentcloudapi.cls.android.producer.Result;
import com.tencentcloudapi.cls.android.producer.errors.ResultFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author farmerx
 */
public class ProducerBatch implements Delayed {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerBatch.class);

    private long nextRetryMs;

    private final String packageId;
    private final String topicId;

    private final int batchSizeThresholdInBytes;

    private final int batchCountThreshold;

    private final long createdMs;

    /**
     * 当前batch日志体积大小
     */
    private int curBatchSizeInBytes;

    /**
     * 当前batch日志条数
     */
    private int curBatchCount;

    /**
     * 发送次数
     */
    private int attemptCount;

    private final List<LogItem> logItems = new ArrayList<>();

    private final List<Thunk> thunks = new ArrayList<>();

    private final EvictingQueue<Attempt> reservedAttempts;

    public long getNextRetryMs() {
        return nextRetryMs;
    }

    public void setNextRetryMs(long nextRetryMs) {
        this.nextRetryMs = nextRetryMs;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getTopicId() {
        return topicId;
    }

    public List<LogItem> getLogItems() {
        return logItems;
    }

    public int getCurBatchSizeInBytes() {
        return curBatchSizeInBytes;
    }

    public int getRetries() {
        return Math.max(0, attemptCount - 1);
    }

    public ProducerBatch(
            String topicId,
            String packageId,
            int batchSizeThresholdInBytes,
            int batchCountThreshold,
            int maxReservedAttempts,
            long nowMs
    ) {
        this.topicId = topicId;
        this.packageId = packageId;
        this.createdMs = nowMs;
        this.batchSizeThresholdInBytes = batchSizeThresholdInBytes;
        this.batchCountThreshold = batchCountThreshold;
        this.curBatchCount = 0;
        this.curBatchSizeInBytes = 0;
        this.attemptCount = 0;
        this.reservedAttempts = EvictingQueue.create(maxReservedAttempts);
    }

    /**
     * put log
     * @param item single log
     * @param sizeInBytes log size
     * @param callback 回调函数
     * @return  ListenableFuture<Result>
     */
    public ListenableFuture<Result> tryAppend(LogItem item, int sizeInBytes, Callback callback) {
        if (hasRoomFor(sizeInBytes, 1)) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            logItems.add(item);
            thunks.add(new Thunk(callback, future));
            curBatchCount++;
            curBatchSizeInBytes += sizeInBytes;
            return future;
        }
    }

    /**
     * put log list log item
     * @param items logs
     * @param sizeInBytes log size
     * @param callback 回调函数
     * @return ListenableFuture<Result>
     */
    public ListenableFuture<Result> tryAppend(
            List<LogItem> items, int sizeInBytes, Callback callback) {
        if (hasRoomFor(sizeInBytes, items.size())) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            logItems.addAll(items);
            thunks.add(new Thunk(callback, future));
            curBatchCount += items.size();
            curBatchSizeInBytes += sizeInBytes;
            return future;
        }
    }

    // 触发上传条件
    public boolean isMeetSendCondition() {
        return curBatchSizeInBytes >= batchSizeThresholdInBytes || curBatchCount >= batchCountThreshold;
    }

    private boolean hasRoomFor(int sizeInBytes, int count) {
        return curBatchSizeInBytes + sizeInBytes > Constants.MAX_BATCH_SIZE_IN_BYTES
                || curBatchCount + count > Constants.MAX_BATCH_COUNT;
    }

    private long createdTimeMs(long nowMs) {
        return Math.max(0, nowMs - createdMs);
    }

    public long remainingMs(long nowMs, long lingerMs) {
        return lingerMs - createdTimeMs(nowMs);
    }

    public void appendAttempt(Attempt attempt) {
        reservedAttempts.add(attempt);
        this.attemptCount++;
    }

    public void fireCallbacksAndSetFutures() {
        List<Attempt> attempts = new ArrayList<>(reservedAttempts);
        Attempt attempt = Iterables.getLast(attempts);
        Result result = new Result(attempt.isSuccess(), attempts, attemptCount);
        fireCallbacks(result);
        setFutures(result);
    }

    private void fireCallbacks(Result result) {
        for (Thunk thunk : thunks) {
            try {
                if (thunk.callback != null) {
                    thunk.callback.onCompletion(result);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to execute user-provided callback, topic_id={}, e=", topicId, e);
            }
        }
    }

    private void setFutures(Result result) {
        for (Thunk thunk : thunks) {
            try {
                if (result.isSuccessful()) {
                    thunk.future.set(result);
                } else {
                    thunk.future.setException(new ResultFailedException(result));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to set future, topic_id={}, e=", topicId, e);
            }
        }
    }

    @Override
    public long getDelay(@Nonnull TimeUnit unit) {
        return unit.convert(nextRetryMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@Nonnull Delayed o) {
        return (int) (nextRetryMs - ((ProducerBatch) o).getNextRetryMs());
    }

    private static final class Thunk {

        final Callback callback;

        final SettableFuture<Result> future;

        Thunk(Callback callback, SettableFuture<Result> future) {
            this.callback = callback;
            this.future = future;
        }
    }
}
