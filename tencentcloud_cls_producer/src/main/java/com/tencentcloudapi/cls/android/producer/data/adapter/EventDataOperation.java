package com.tencentcloudapi.cls.android.producer.data.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.common.Logs;

import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class EventDataOperation extends DataOperation {

    EventDataOperation(Context context) {
        super(context);
        TAG = "EventDataOperation";
    }

    @Override
    int insertData(Uri uri, String event) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, event);
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            cv.put(DbParams.KEY_IS_INSTANT_EVENT, 0);
            contentResolver.insert(uri, cv);
        } catch (Throwable e) {
            CLSLog.i(TAG, e.getMessage());
        }
        return 0;
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            contentResolver.insert(uri, contentValues);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    Map<String, EventData> queryData(Uri uri, int limit) {
        return queryData(uri, false, limit);
    }

    @Override
    Map<String, EventData> queryData(Uri uri, boolean is_instant_event, int limit) {
        try {
            return queryDataInner(uri, is_instant_event, limit);
        } catch (SQLiteBlobTooBigException bigException) {
            CLSLog.i(TAG, bigException.getMessage());
            return handleBigException(uri, is_instant_event);
        } catch (final SQLiteException e) {
            CLSLog.i(TAG, e.getMessage());
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }

    private Map<String, EventData> queryDataInner(Uri uri, boolean is_instant_event, int limit) {
        Map<String, EventData> dataMap = new HashMap<>();
        dataMap.put("InvalidIds", new EventData(new JSONArray(), Logs.LogGroup.newBuilder()));
        dataMap.put("AllEventIds", new EventData(new JSONArray(), Logs.LogGroup.newBuilder()));
        Cursor cursor = null;
        try {
            String instant_event = "0";
            if (is_instant_event) {
                instant_event = "1";
            }
            cursor = contentResolver.query(uri, null, DbParams.KEY_IS_INSTANT_EVENT + "=?", new String[]{instant_event}, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                String keyData;
                while (cursor.moveToNext()) {
                    String eventId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                    // Add event ID to AllEventIds
                    Objects.requireNonNull(dataMap.get("AllEventIds")).getEventIds().put(eventId);
                    try {
                        keyData = cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_DATA));
                        if (TextUtils.isEmpty(keyData)) {
                            Objects.requireNonNull(dataMap.get("InvalidIds")).getEventIds().put(eventId);
                            continue;
                        }
                        String topic = "";
                        int index = keyData.lastIndexOf("\t");
                        if (index > -1) {
                            topic = keyData.substring(index).replaceFirst("\t", "");
                            keyData = keyData.substring(0, index);
                        }
                        if (TextUtils.isEmpty(keyData)) {
                            Objects.requireNonNull(dataMap.get("InvalidIds")).getEventIds().put(eventId);
                            continue;
                        }
                        if (TextUtils.isEmpty(topic)) {
                            topic = "OldEventData";
                        }
                        if (!dataMap.containsKey(topic)) {
                            dataMap.put(topic, new EventData(new JSONArray(), Logs.LogGroup.newBuilder()));
                        }
                        Logs.Log.Builder log = Logs.Log.newBuilder().mergeFrom(Base64.decode(keyData, Base64.DEFAULT));
                        Objects.requireNonNull(dataMap.get(topic)).setEventData(eventId, log);
                    } catch (Exception e) {
                        CLSLog.printStackTrace(e);
                    }
                }
            }
        } catch (Throwable error) {
            CLSLog.i(TAG, error.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return dataMap;
    }


    /*
     用于处理 SQLiteBlobTooBigException 的场景，出现此类情况时，只读取一条数据上报
     */
    private Map<String, EventData> handleBigException(Uri uri, boolean is_instant_event) {
        try {
            return queryDataInner(uri, is_instant_event, 1);
        } catch (SQLiteBlobTooBigException bigException) {//说明第一条数据就是 SQLiteBlobTooBigException，该条数据一直无法上报，所以删除处理
            deleteData(uri, getFirstRowId(uri, is_instant_event ? "1" : "0"));
            CLSLog.printStackTrace(bigException);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
        return null;
    }
}
