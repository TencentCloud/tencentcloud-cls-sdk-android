package com.tencentcloudapi.cls.android.producer.data.adapter;

import android.content.Context;

import com.tencentcloudapi.cls.android.CLSLog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class DbAdapter {
    private static DbAdapter instance;
    private final DbParams mDbParams;
    private DataOperation mTrackEventOperation;

    private DbAdapter(Context context) {
        mDbParams = DbParams.getInstance(context.getPackageName());
        mTrackEventOperation = new EventDataOperation(context);
    }

    public static DbAdapter getInstance(Context context) {
        if (instance == null) {
            instance = new DbAdapter(context);
        }
        return instance;
    }

    public static DbAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(Context context) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param event the Cls log to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addLogEvent(String event) {
        int code = mTrackEventOperation.insertData(mDbParams.getEventUri(), event);
        if (code == 0) {
            return mTrackEventOperation.queryDataCount(mDbParams.getEventUri(), 2);
        }
        return code;
    }

    /**
     * Removes all events from table
     */
    public void deleteAllEvents() {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), DbParams.DB_DELETE_ALL);
    }

    public int cleanupEvents(JSONArray ids, boolean is_instant_event) {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), ids);
        return mTrackEventOperation.queryDataCount(mDbParams.getEventUri(), is_instant_event ? 1 : 0);
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param tableName 表名
     * @param limit 条数限制
     * @param is_instant_event 是否实时数据
     * @return 数据
     */
    public Map<String, EventData> generateDataString(String tableName, int limit, boolean is_instant_event) {
        try {
            return mTrackEventOperation.queryData(mDbParams.getEventUri(), is_instant_event, limit);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return null;
    }
}
