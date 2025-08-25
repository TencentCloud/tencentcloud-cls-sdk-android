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

import java.nio.charset.StandardCharsets;

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
    byte[][] queryData(Uri uri, int limit) {
        return queryData(uri, false, limit);
    }

    @Override
    byte[][] queryData(Uri uri, boolean is_instant_event, int limit) {
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

    private byte[][] queryDataInner(Uri uri, boolean is_instant_event, int limit) {
        Cursor cursor = null;
        byte[] data = null;
        byte[] eventIds = null;
        JSONArray idArray = new JSONArray();
        try {
            String instant_event = "0";
            if (is_instant_event) {
                instant_event = "1";
            }
            cursor = contentResolver.query(uri, null, DbParams.KEY_IS_INSTANT_EVENT + "=?", new String[]{instant_event}, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                Logs.LogGroup.Builder logGroupBuilder = Logs.LogGroup.newBuilder();
                String keyData;
                while (cursor.moveToNext()) {
                    String eventId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                    idArray.put(eventId);
                    try {
                        keyData = cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_DATA));
                        if (TextUtils.isEmpty(keyData)) {
                            continue;
                        }
                        Logs.Log.Builder log = Logs.Log.newBuilder().mergeFrom(Base64.decode(keyData, Base64.DEFAULT));
                        logGroupBuilder.addLogs(log);
                    } catch (Exception e) {
                        CLSLog.printStackTrace(e);
                    }
                }
                data = logGroupBuilder.build().toByteArray();
                if (idArray.length() > 0) {
                    eventIds = idArray.toString().getBytes(StandardCharsets.UTF_8);
                }
            }
        } catch (Throwable error) {
            CLSLog.i(TAG, error.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (eventIds != null) {
            return new byte[][]{eventIds, data};
        }
        return null;
    }

    /*
     用于处理 SQLiteBlobTooBigException 的场景，出现此类情况时，只读取一条数据上报
     */
    private byte[][] handleBigException(Uri uri, boolean is_instant_event) {
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
