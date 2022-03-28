package com.tencentcloudapi.cls.android.producer.errors;

/**
 * @author farmerx
 */
public class MaxBatchCountExceedException extends ProducerException {
    public MaxBatchCountExceedException(String message) {
        super(message);
    }
}
