package com.tencentcloud.cls.producer.common;


/**
 * @author farmerx
 */
public class Constants {
    public enum CompressType {
        NONE(""),
        LZ4(Constants.CONST_LZ4),
        GZIP(Constants.CONST_GZIP_ENCODING);

        private String strValue;

        CompressType(String strValue) {
            this.strValue = strValue;
        }

        public String toString() {
            return strValue;
        }

        public static CompressType fromString(final String compressType) {
            for (CompressType type : values()) {
                if (type.strValue.equals(compressType)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("invalid CompressType: " + compressType + ", should be (" + CompressType.NONE + ", " + CompressType.GZIP + ", " + CompressType.LZ4 + ")");
        }
    }

    public static int CONST_MAX_PUT_SIZE = 1 * 1024 * 1024;
    public static final String CONST_X_SLS_COMPRESSTYPE = "x-cls-compress-type";
    public static final String CONST_LZ4 = "lz4";

    public static final String CONST_CONTENT_TYPE = "Content-Type";
    public static final String CONST_PROTO_BUF = "application/x-protobuf";
    public static final String CONST_CONTENT_LENGTH = "Content-Length";
    public static final String CONST_AUTHORIZATION = "Authorization";
    public static final String CONST_GZIP_ENCODING = "deflate";
    public static final String CONST_X_SLS_REQUESTID = "x-log-requestid";
    public static final String CONST_HOST = "Host";
    public static final String CONST_MD5 = "MD5";
    public static final String UTF_8_ENCODING = "UTF-8";
    public static final String CONST_LOCAL_IP = "127.0.0.1";
    public static int HTTP_CONNECT_TIME_OUT = 60 * 1000;
    public static int HTTP_SEND_TIME_OUT = 60 * 1000;
    public static final String TOPIC_ID = "topic_id";
    public static final String UPLOAD_LOG_RESOURCE_URI = "/structuredlog";
}
