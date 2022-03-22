package com.tencentcloudapi.cls.android.producer.common;
import java.util.List;


/**
 * @author farmerx
 */
public abstract class LogSizeCalculator {

    public static int calculate(LogItem logItem) {
        int sizeInBytes = 4;
        for (Logs.Log.Content content : logItem.mContents.getContentsList()) {
            if (content.getKey() != null) {
                sizeInBytes += content.getKey().length();
            }
            if (content.getValue() != null) {
                sizeInBytes += content.getValue().length();
            }
        }
        return sizeInBytes;
    }

    public static int calculate(List<LogItem> logItems) {
        int sizeInBytes = 0;
        for (LogItem logItem : logItems) {
            sizeInBytes += calculate(logItem);
        }
        return sizeInBytes;
    }
}
