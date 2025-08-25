package com.tencentcloudapi.cls.android.exceptions;

public class InvalidDataException extends Exception {

    public InvalidDataException(String error) {
        super(error);
    }

    public InvalidDataException(Throwable throwable) {
        super(throwable);
    }

}