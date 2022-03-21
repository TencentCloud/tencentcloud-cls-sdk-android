package com.tencentcloudapi.cls.producer.http.comm;

/**
 * Represents the communication protocol to use when sending requests to OSS, we
 * use HTTPS by default.
 */
public enum Protocol {

    HTTP("http"),

    HTTPS("https");

    private final String protocol;

    Protocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return protocol;
    }
}
