package com.tencentcloudapi.cls.producer.common;

/**
 * @author farmerx
 */
public class LogContent {
    private static final long serialVersionUID = 6042186396863898096L;
    public Logs.Log.Content.Builder content;

    /**
     * Construct a empty log content
     */
    public LogContent() {
    }

    /**
     * Construct a log content pair
     *
     * @param key
     *            log content key
     * @param value
     *            log content value
     */
    public LogContent(String key, String value) {
        this.content = Logs.Log.Content.newBuilder();
        content.setKey(key).setValue(value);
    }

    /**
     * Get log content key
     *
     * @return log content key
     */
    public String GetKey() {
        return content.getKey();
    }

    /**
     * Get log content value
     *
     * @return log content value
     */
    public String GetValue() {
        return content.getValue();
    }
}
