package com.tencentcloudapi.cls.android.producer.data.adapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.ClsDataAPI;
import com.tencentcloudapi.cls.android.producer.common.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;

abstract class DataOperation {
    String TAG = "EventDataOperation";
    ContentResolver contentResolver;
    private File mDatabaseFile;
    private final Context mContext;

    DataOperation(Context context) {
        this.mContext = context;
        contentResolver = context.getContentResolver();
    }

    /**
     * @param uri Uri
     * @param event String
     * @return ErrorCode
     */
    abstract int insertData(Uri uri, String event);

    /**
     * @param uri Uri
     * @param contentValues ContentValues
     * @return ErrorCode
     */
    abstract int insertData(Uri uri, ContentValues contentValues);

    /**
     * query data
     */
    abstract byte[][] queryData(Uri uri, int limit);

    abstract byte[][] queryData(Uri uri, boolean is_instant_event, int limit);

    /**
     * 查收数据库数据
     *
     * @param uri 查询的 uri
     * @param select 0 代表查询非实时数据，1 代表查询实时数据，2代表查询总数据
     * @return 返回查询的数据条数
     */
    int queryDataCount(Uri uri, int select) {
        String[] selectionArgs = null;
        switch (select) {
            case 0:
                selectionArgs = new String[]{"0"};
                break;
            case 1:
                selectionArgs = new String[]{"1"};
                break;
            default:
                break;
        }
        if (selectionArgs != null) {
            return queryDataCount(uri, new String[]{"_id"}, DbParams.KEY_IS_INSTANT_EVENT + "=?", selectionArgs, null);
        }
        return queryDataCount(uri, new String[]{"_id"}, null, null, null);
    }

    /**
     * query data count
     */
    int queryDataCount(Uri uri, String[] projection, String selection,
                       String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (Exception ex) {
            CLSLog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    String getFirstRowId(Uri uri, String instant_event) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, new String[]{"_id"},  DbParams.KEY_IS_INSTANT_EVENT + "=?", new String[]{instant_event}, DbParams.KEY_CREATED_AT + " ASC LIMIT " + 1);
            if (cursor != null) {
                return cursor.getString(cursor.getColumnIndexOrThrow("_id"));
            }
        } catch (Exception ex) {
            CLSLog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    /**
     * delete data
     */
    void deleteData(Uri uri, String id) {
        try {
            if (DbParams.DB_DELETE_ALL.equals(id)) {
                CLSLog.i(TAG, "deleteData DB_DELETE_ALL");
                contentResolver.delete(uri, null, null);
            } else {
                contentResolver.delete(uri, "_id <= ?", new String[]{id});
            }
        } catch (Exception ex) {
            CLSLog.printStackTrace(ex);
        }
    }

    /**
     * 删除指定 id 的 event 数据
     *
     * @param uri Uri
     * @param ids 指定的 id 集合
     */
    public void deleteData(Uri uri, JSONArray ids) {
        try {
            CLSLog.i(TAG, "deleteData ids = " + ids);
            mContext.getContentResolver().delete(uri, "_id in " + buildIds(ids), null);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    String parseData(String keyData) {
        try {
            if (TextUtils.isEmpty(keyData)) return "";
            return keyData;
        } catch (Exception ex) {
            CLSLog.printStackTrace(ex);
        }
        return keyData;
    }

    /**
     * delete data when db is full
     *
     * @param uri URI
     * @return ErrorCode
     */
    int deleteDataLowMemory(Uri uri) {
        if (belowMemThreshold()) {
            CLSLog.i(TAG, "There is not enough space left on the device to store events, so will delete 100 oldest events");
            byte[][] eventsData = queryData(uri, 100);
            if (eventsData == null) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }

            final String lastId = new String(eventsData[0], StandardCharsets.UTF_8);
            deleteData(uri, lastId);
            if (queryDataCount(uri, 2) <= 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
        }
        return 0;
    }

    private long getMaxCacheSize(Context context) {
        try {
            return ClsDataAPI.sharedInstance(context).getClsConfigOptions().getMaxCacheSize();
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            return 32 * 1024 * 1024;
        }
    }

    private boolean belowMemThreshold() {
        if (mDatabaseFile == null) {
            mDatabaseFile = mContext.getDatabasePath(DbParams.DATABASE_NAME);
        }
        if (mDatabaseFile.exists()) {
            return mDatabaseFile.length() >= getMaxCacheSize(mContext);
        }
        return false;
    }

    /**
     * 构造 SQL 中的 id 集合
     *
     * @param idArray id 集合
     * @return SQL 中 id 语句
     */
    private String buildIds(JSONArray idArray) throws JSONException {
        StringBuilder idArgs = new StringBuilder();
        idArgs.append("(");
        if (idArray != null && idArray.length() > 0) {
            for (int index = 0; index < idArray.length(); index++) {
                idArgs.append(idArray.get(index)).append(",");
            }
            idArgs.replace(idArgs.length() - 1, idArgs.length(), "");
        }
        idArgs.append(")");
        return idArgs.toString();
    }
}

