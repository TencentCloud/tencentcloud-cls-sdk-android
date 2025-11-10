package com.tencentcloudapi.cls.android.utils;

import android.os.SystemClock;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public enum TimeUtils {
    instance;

    private final long start;
    private final long nanoTime;

    private TimeUtils() {
        this.start = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
        this.nanoTime = System.nanoTime();
    }

    public Long getTime() {
        return this.getDate().getTime();
    }

    public Date getDate() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
    }

    public Long getUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public Long now() {
        return this.start + (System.nanoTime() - this.nanoTime);
    }
}
