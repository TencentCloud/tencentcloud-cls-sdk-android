package com.tencentcloudapi.cls.android.producer.common;

/**
 * @author farmerx
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }
    public static final String BAD_RESPONSE = "BadResponse";
    public static final String ENDPOINT_INVALID = "EndpointInvalid";
    public static final String ENCODING_EXCEPTION = "EncodingException";
    public static final String INTERNAL_SERVER_ERROR = "InternalError";
    public static final String SpeedQuotaExceed = "SpeedQuotaExceed";
    public static final String LogSizeExceed = "LogSizeExceed";
    public static final String SendFailed = "SendFailed";
}
