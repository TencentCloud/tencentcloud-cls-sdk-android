package com.tencentcloudapi.cls.android.exceptions;

public class ResponseErrorException extends Exception {
    private int httpCode;

    public ResponseErrorException(String error, int httpCode) {
        super(error);
        this.httpCode = httpCode;
    }

    public ResponseErrorException(Throwable throwable, int httpCode) {
        super(throwable);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return this.httpCode;
    }
}

