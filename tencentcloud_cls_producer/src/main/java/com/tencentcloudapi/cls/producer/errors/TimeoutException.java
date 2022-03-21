package com.tencentcloudapi.cls.producer.errors;

/**
 * @author farmerx
 */
public class TimeoutException extends ProducerException {
    public TimeoutException(String message) {
        super(message);
    }
}
