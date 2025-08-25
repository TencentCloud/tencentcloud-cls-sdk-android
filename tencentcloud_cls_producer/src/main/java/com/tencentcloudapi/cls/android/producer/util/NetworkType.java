package com.tencentcloudapi.cls.android.producer.util;

/**
 * 网络类型
 */
public interface NetworkType {
    // NULL
    int TYPE_NONE = 0;
    // 2G
    int TYPE_2G = 1;
    // 3G
    int TYPE_3G = 1 << 1;
    // 4G
    int TYPE_4G = 1 << 2;
    // WIFI
    int TYPE_WIFI = 1 << 3;
    // 5G
    int TYPE_5G = 1 << 4;
    // ALL
    int TYPE_ALL = 0xFF;
}
