package com.tencentcloudapi.cls.android.producer.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.data.adapter.DbParams;

public class ClsDataDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "CLS.SQLiteOpenHelper";
    private static final String CREATE_EVENTS_TABLE =
            String.format("CREATE TABLE IF NOT EXISTS %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, " +
                    "%s INTEGER NOT NULL, %s INTEGER NOT NULL DEFAULT 0);",
                    DbParams.TABLE_EVENTS, DbParams.KEY_DATA, DbParams.KEY_CREATED_AT, DbParams.KEY_IS_INSTANT_EVENT);
    private static final String EVENTS_TIME_INDEX =
            String.format("CREATE INDEX IF NOT EXISTS time_idx ON %s (%s);", DbParams.TABLE_EVENTS, DbParams.KEY_CREATED_AT);

    public ClsDataDBHelper(Context context) {
        super(context, DbParams.DATABASE_NAME, null, DbParams.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        CLSLog.i(TAG, "Creating a new CLS Analytics DB");
        createTable(db);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        CLSLog.i(TAG, "Upgrading app, replacing CLS DB, oldVersion:" + oldVersion + ", newVersion:" + newVersion);
        createTable(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


    private void createTable(SQLiteDatabase db) {
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
    }

    /**
     * 检查某表中某列是否存在
     *
     * @param db 数据库
     * @param tableName 表名
     * @param columnName 列名
     * @return 列表某列是否存在
     */
    private boolean checkColumnExist(SQLiteDatabase db, String tableName
            , String columnName) {
        boolean result = false;
        Cursor cursor = null;
        try {
            //查询一行
            cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0"
                    , null);
            result = cursor != null && cursor.getColumnIndex(columnName) != -1;
        } catch (Exception e) {
            CLSLog.e("CLS.Exception", e.getMessage());
        } finally {
            try {
                if (null != cursor && !cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e) {
                CLSLog.e("CLS.Exception", e.getMessage());
            }
        }
        return result;
    }

}
