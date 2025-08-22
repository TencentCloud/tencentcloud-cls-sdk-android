package com.tencentcloudapi.cls.android.exceptions;

/**
 * 网络连接错误
 */
public class ConnectErrorException extends Exception {

    private int mRetryAfter;

    public ConnectErrorException(String message) {
        super(message);
        mRetryAfter = 30 * 1000;
    }

    public ConnectErrorException(String message, String strRetryAfter) {
        super(message);
        try {
            mRetryAfter = Integer.parseInt(strRetryAfter);
        } catch (NumberFormatException e) {
            mRetryAfter = 0;
        }
    }

    public ConnectErrorException(Throwable throwable) {
        super(throwable);
    }

    public int getRetryAfter() {
        return mRetryAfter;
    }

}
