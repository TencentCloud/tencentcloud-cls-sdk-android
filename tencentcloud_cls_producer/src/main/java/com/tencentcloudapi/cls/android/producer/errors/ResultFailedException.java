package com.tencentcloudapi.cls.android.producer.errors;

import com.tencentcloudapi.cls.android.producer.Result;
import com.tencentcloudapi.cls.android.producer.common.Attempt;
import java.util.List;

/**
 * @author farmerx
 */
public class ResultFailedException extends ProducerException {

    private final Result result;

    public ResultFailedException(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    public String getErrorCode() {
        return result.getErrorCode();
    }

    public String getErrorMessage() {
        return result.getErrorMessage();
    }

    public List<Attempt> getReservedAttempts() {
        return result.getReservedAttempts();
    }
}
