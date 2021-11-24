package com.tencentcloud.cls.producer.common;

/**
 * The exception is thrown if error happen.
 *
 * @author farmerx
 */
public class LogException extends Exception {

    private int httpCode = -1;

    private String errorCode;

    /**
     * Construct LogException
     *
     * @param code      error code
     * @param message   error message
     */
    public LogException(String code, String message) {
        super(message);
        this.errorCode = code;
    }

    /**
     * Construct LogException
     *
     * @param code      error code
     * @param message   error message
     * @param cause     inner exception, which cause the error
     */
    public LogException(String code, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = code;
    }

    /**
     * Construct LogException
     *
     * @param httpCode  http code, -1 the error is happened in the client
     * @param code      error code
     * @param message   error message
     */
    public LogException(int httpCode, String code, String message) {
        super(message);
        this.httpCode = httpCode;
        this.errorCode = code;
    }

    /**
     * Get the error code
     *
     * @return error code
     */
    public String GetErrorCode() {
        return this.errorCode;
    }

    /**
     * Get the error message
     *
     * @return error message
     */
    public String GetErrorMessage() {
        return super.getMessage();
    }

    /**
     * Get the http response code
     *
     * @return http code, -1 the error is happened in the client
     */
    public int GetHttpCode() {
        return httpCode;
    }

    /**
     * Set the http response code
     *
     * @param httpCode http code, -1 the error is happened in the client
     */
    public void SetHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }
}
