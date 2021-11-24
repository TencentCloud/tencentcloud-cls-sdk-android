package com.tencentcloud.cls.producer.common;

import java.util.Date;

/**
 * @author farmerx
 */
public class LogItem {
    private static final long serialVersionUID = -3488075856612935955L;
    public Logs.Log.Builder mContents = Logs.Log.newBuilder();

    /**
     * Construct a logItem, the log time is set according to the sys time
     */
    public LogItem() {
        this.mContents.setTime((long) (new Date().getTime() / 1000));
    }

    /**
     * Construct a logItem with a certain time stamp
     *
     * @param logTime log time stamp
     *
     */
    public LogItem(long logTime) {
        this.mContents.setTime(logTime);
    }

    /**
     * Construct a logItem with a certain time stamp and log contents
     *
     * @param logTime log time stamp
     * @param contents log contents
     */
    public LogItem(long logTime, Logs.Log.Builder contents) {
        SetLogContents(contents);
    }

    /**
     * Set logTime
     *
     * @param logTime log time
     */
    public void SetTime(long logTime) {
        this.mContents.setTime(logTime);
    }

    /**
     * Get log time
     *
     * @return log time
     */
    public long GetTime() {
        return this.mContents.getTime();
    }

    /**
     * Add a log content key/value pair to the log
     *
     * @param key  log content key
     * @param value  log content value
     */
    public void PushBack(String key, String value) {
        PushBack(new LogContent(key, value));
    }

    /**
     * Add a log content to the log
     *
     * @param content  log content
     */
    public void PushBack(LogContent content) {
        this.mContents.addContents(content.content);
    }

    /**
     * set log contents
     * @param contents  log contents
     */
    public void SetLogContents(Logs.Log.Builder contents) {
        this.mContents = contents;
    }


}
