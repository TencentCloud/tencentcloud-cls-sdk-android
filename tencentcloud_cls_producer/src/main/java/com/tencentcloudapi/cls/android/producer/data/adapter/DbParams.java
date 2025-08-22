package com.tencentcloudapi.cls.android.producer.data.adapter;

import android.net.Uri;

public class DbParams {
    public static final String DATABASE_NAME = "clsdata";
    public static final int DATABASE_VERSION = 6;
    public static final String TABLE_EVENTS = "events";

    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_IS_INSTANT_EVENT = "is_instant_event";
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
    public static final String DB_DELETE_ALL = "DB_DELETE_ALL";
    private static DbParams instance;
    private final Uri mUri;

    private DbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".ClsDataContentProvider/" + TABLE_EVENTS);
    }

    public static DbParams getInstance(String packageName) {
        if (instance == null) {
            instance = new DbParams(packageName);
        }
        return instance;
    }

    public static DbParams getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * 获取 Event Uri
     *
     * @return Uri
     */
    Uri getEventUri() {
        return mUri;
    }
}
