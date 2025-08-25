package com.tencentcloudapi.cls.android.producer.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.data.adapter.DbParams;

public class ClsDataContentProvider extends ContentProvider {
    private static final String TAG = "ClsDataContentProvider";
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private ClsProviderHelper mProviderHelper;


    @Override
    public boolean onCreate() {
        try {
            Context context = getContext();
            if (context != null) {
                mProviderHelper = ClsProviderHelper.getInstance(context);
                mProviderHelper.appendUri(uriMatcher, context.getPackageName() + ".ClsDataContentProvider");
            }
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
        return true;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        try {
            return mProviderHelper.bulkInsert(uri, values);
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            int code = uriMatcher.match(uri);
            if (code == ClsProviderHelper.URI_CODE.EVENTS) {
                return mProviderHelper.queryByTable(DbParams.TABLE_EVENTS, projection, selection, selectionArgs, sortOrder);
            }
            cursor = mProviderHelper.queryByTable(DbParams.TABLE_EVENTS, projection, selection, selectionArgs, sortOrder);
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 不处理 values = null 或者 values 为空的情况
        if (values == null || values.size() == 0) {
            return uri;
        }
        try {
            int code = uriMatcher.match(uri);
            if (code == ClsProviderHelper.URI_CODE.EVENTS) {
                return mProviderHelper.insertEvent(uri, values);
            }
            return uri;
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int deletedCounts = 0;
        try {
            int code = uriMatcher.match(uri);
            if (ClsProviderHelper.URI_CODE.EVENTS == code) {
                return mProviderHelper.deleteEvents(selection, selectionArgs);
            }
            //目前逻辑不处理其他 Code
        } catch (Exception e) {
            CLSLog.e(TAG, e.getMessage());
        }
        return deletedCounts;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
