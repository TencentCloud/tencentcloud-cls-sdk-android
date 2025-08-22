package com.tencentcloudapi.cls.android.producer.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.data.adapter.DbParams;

public class ClsProviderHelper {
    private static final String TAG = "CLS.ProviderHelper";

    private static ClsProviderHelper INSTANCE;

    private SQLiteOpenHelper mDbHelper;
    private Context mContext;
    private boolean isDbWritable = true;
    private boolean mIsFlushDataState = false;

    private ClsProviderHelper(Context context) {
        try {
            this.mDbHelper = new ClsDataDBHelper(context);
            this.mContext = context.getApplicationContext();
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
    }

    public static synchronized ClsProviderHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ClsProviderHelper(context);
        }
        return INSTANCE;
    }

    interface QueryEventsListener {
        void insert(String data, String keyCreated);
    }

    /**
     * 构建 Uri 类型
     *
     * @param uriMatcher UriMatcher
     * @param authority authority
     */
    public void appendUri(UriMatcher uriMatcher, String authority) {
        try {
            uriMatcher.addURI(authority, DbParams.TABLE_EVENTS, URI_CODE.EVENTS);
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
    }


    /**
     * 插入 Event 埋点数据
     *
     * @param uri Uri
     * @param values 数据
     * @return Uri
     */
    public Uri insertEvent(Uri uri, ContentValues values) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database == null || !values.containsKey(DbParams.KEY_DATA)
                    || !values.containsKey(DbParams.KEY_CREATED_AT)) {
                return uri;
            }
            database.insert(DbParams.TABLE_EVENTS, "_id", values);
            return uri;
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
        return uri;
    }

    /**
     * 删除埋点数据
     *
     * @param selection 条件
     * @param selectionArgs 参数
     * @return 受影响数
     */
    public int deleteEvents(String selection, String[] selectionArgs) {
        if (!isDbWritable) {
            return 0;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database != null) {
                return database.delete(DbParams.TABLE_EVENTS, selection, selectionArgs);
            }
        } catch (SQLiteException e) {
            isDbWritable = false;
            CLSLog.e(TAG, e.getMessage());
        }
        return 0;
    }

    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numValues;
        SQLiteDatabase database = null;
        try {
            try {
                database = mDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                CLSLog.e(TAG, e.getMessage());
                return 0;
            }
            database.beginTransaction();
            numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                insertEvent(uri, values[i]);
            }
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return numValues;
    }

    /**
     * 查询数据
     *
     * @param tableName 表名
     * @param projection 列明
     * @param selection 筛选条件
     * @param selectionArgs 筛选参数
     * @param sortOrder 排序
     * @return Cursor
     */
    public Cursor queryByTable(String tableName, String[] projection, String selection, String[]
            selectionArgs, String sortOrder) {
        if (!isDbWritable) {
            return null;
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase liteDatabase = getWritableDatabase();
            if (liteDatabase != null) {
                cursor = liteDatabase.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
            }
        } catch (SQLiteException e) {
            isDbWritable = false;
            CLSLog.e(TAG, e.getMessage());
        }
        return cursor;
    }
    /**
     * 获取数据库
     *
     * @return SQLiteDatabase
     */
    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase database = null;
        try {
            if (!isDBExist()) {
                mDbHelper.close();
                isDbWritable = true;
            }
            database = mDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            CLSLog.e(TAG, e.getMessage());
            isDbWritable = false;
        }
        return database;
    }

    private boolean isDBExist() {
        return mContext.getDatabasePath(DbParams.DATABASE_NAME).exists();
    }

    /**
     * URI 对应的 Code
     */
    public interface URI_CODE {
        int EVENTS = 1;
    }

}
