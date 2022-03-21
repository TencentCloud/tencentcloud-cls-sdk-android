package com.tencentcloudapi.cls.producer;

import com.google.common.collect.Iterables;
import com.tencentcloudapi.cls.producer.common.Attempt;
import java.util.List;

/**
 * @author farmerx
 */
public class Result {

    /**
     * 任务执行结果
     */
    private final boolean successful;

    /**
     * 每批发送的结果
     */
    private final List<Attempt> reservedAttempts;

    /**
     * 每批发送的数量
     */
    private final int attemptCount;

    /**
     * New  ...
     * @param successful 任务结果
     * @param reservedAttempts 任务状态列表
     * @param attemptCount 任务数量
     */
    public Result(boolean successful, List<Attempt> reservedAttempts, int attemptCount) {
        this.successful = successful;
        this.reservedAttempts = reservedAttempts;
        this.attemptCount = attemptCount;
    }

    /**
     * 获取任务执行结果
     * @return successful boolean
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * 获取Reserved Attempts
     * @return List<Attempt>
     */
    public List<Attempt> getReservedAttempts() {
        return reservedAttempts;
    }

    /**
     * 获取 attempt Count
     * @return int attemptCount
     */
    public int getAttemptCount() {
        return attemptCount;
    }

    /**
     * 获取error code
     * @return error code
     */
    public String getErrorCode() {
        Attempt lastAttempt = Iterables.getLast(reservedAttempts);
        return lastAttempt.getErrorCode();
    }

    /**
     * 获取error message
     * @return error message
     */
    public String getErrorMessage() {
        Attempt lastAttempt = Iterables.getLast(reservedAttempts);
        return lastAttempt.getErrorMessage();
    }

    @Override
    public String toString() {
        return "Result{"
                + "successful="
                + successful
                + ", reservedAttempts="
                + reservedAttempts
                + ", attemptCount="
                + attemptCount
                + '}';
    }
}
