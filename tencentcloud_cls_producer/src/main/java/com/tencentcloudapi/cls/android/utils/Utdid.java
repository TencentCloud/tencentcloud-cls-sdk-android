package com.tencentcloudapi.cls.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import com.tencentcloudapi.cls.android.CLSLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public final class Utdid {
    private Utdid() {
        //no instance
    }

    // SharedPreferences 存储位置
    private static final String SP_NAME = "cls_android_utdid";
    private static final String SP_KEY_UTDID = "utdid";

    // 兼容读取旧版本文件的路径（旧版本使用文件持久化）
    private static final String LEGACY_FILE_DIR = "/cls_android/files";
    private static final String LEGACY_FILE_NAME = "unique";

    // 进程内内存缓存：一旦成功读到/生成过 utdid，后续调用直接返回，
    // 完全避免任何 IO，从根本上规避 fdsan 冲突。
    private static volatile String sCachedUtdid = null;

    private static class Holder {
        final static Utdid INSTANCE = new Utdid();
    }

    public static Utdid getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void setUtdid(Context context, String utdid) {
        if (null == context || TextUtils.isEmpty(utdid)) {
            return;
        }
        try {
            Storage.getInstance().setUtdid(context, utdid);
            sCachedUtdid = utdid;
        } catch (Throwable t) {
            // ignore
        }
    }

    public synchronized String getUtdid(Context context) {
        // 命中内存缓存直接返回，避免任何 IO
        String cached = sCachedUtdid;
        if (!TextUtils.isEmpty(cached)) {
            return cached;
        }

        if (null == context) {
            return "ffffffffffffffffffffffff";
        }

        String utdid;
        try {
            utdid = Storage.getInstance().getUtdid(context);
        } catch (Throwable t) {
            utdid = "";
        }
        if (!TextUtils.isEmpty(utdid)) {
            sCachedUtdid = utdid;
            return utdid;
        }

        try {
            utdid = UUID.randomUUID().toString();
            String[] parts = utdid.split("-");
            utdid = parts[0] + parts[1] + parts[2];
            //noinspection CharsetObjectCanBeUsed
            utdid = Base64.encodeToString(utdid.getBytes("UTF-8"), Base64.DEFAULT);

            Storage.getInstance().setUtdid(context, utdid);
            sCachedUtdid = utdid;
        } catch (Throwable t) {
            utdid = "ffffffffffffffffffffffff";
        }

        return utdid;
    }

    private static class Storage {
        private static class Holder {
            final static Utdid.Storage INSTANCE = new Utdid.Storage();
        }

        static Storage getInstance() {
            return Holder.INSTANCE;
        }

        void setUtdid(Context context, String utdid) {
            if (null == context || TextUtils.isEmpty(utdid)) {
                return;
            }
            try {
                SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                sp.edit().putString(SP_KEY_UTDID, utdid).apply();
            } catch (Throwable e) {
                CLSLog.printStackTrace(e instanceof Exception ? (Exception) e : new RuntimeException(e));
            }
        }

        String getUtdid(Context context) {
            if (null == context) {
                return "";
            }

            // 1. 优先从 SharedPreferences 读取（SP 由系统统一管理 fd，不会触发 fdsan 冲突）
            try {
                SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                String utdid = sp.getString(SP_KEY_UTDID, null);
                if (!TextUtils.isEmpty(utdid)) {
                    return validUtdid(utdid);
                }
            } catch (Throwable e) {
                CLSLog.printStackTrace(e instanceof Exception ? (Exception) e : new RuntimeException(e));
            }

            // 2. SP 中没有，兼容读取旧版本的文件；读到后立即迁移到 SP 中，
            //    之后就不会再打开这个文件，fdsan 冲突风险仅存在于本次迁移读取。
            String legacy = readLegacyFile(context);
            if (!TextUtils.isEmpty(legacy)) {
                // 迁移写入 SP
                try {
                    SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                    sp.edit().putString(SP_KEY_UTDID, legacy).apply();
                } catch (Throwable ignore) {
                    // ignore
                }
                return validUtdid(legacy);
            }

            return "";
        }

        /**
         * 读取旧版本的持久化文件，仅在 SP 中查不到时执行一次。
         */
        private String readLegacyFile(Context context) {
            File file = getLegacyFile(context);
            if (null == file || !file.exists()) {
                return "";
            }
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.readLine();
            } catch (Throwable e) {
                CLSLog.printStackTrace(e instanceof Exception ? (Exception) e : new RuntimeException(e));
            }
            return "";
        }

        private File getLegacyFile(Context context) {
            try {
                File dir = context.getFilesDir();
                if (null == dir) {
                    return null;
                }
                return new File(new File(dir, LEGACY_FILE_DIR), LEGACY_FILE_NAME);
            } catch (Throwable t) {
                return null;
            }
        }

        private String validUtdid(String utidid) {
            if (TextUtils.isEmpty(utidid)) {
                return "ffffffffffffffffffffffff";
            }

            if (utidid.endsWith("\n")) {
                return utidid.substring(0, utidid.length() - 1);
            }

            return utidid;
        }
    }
}