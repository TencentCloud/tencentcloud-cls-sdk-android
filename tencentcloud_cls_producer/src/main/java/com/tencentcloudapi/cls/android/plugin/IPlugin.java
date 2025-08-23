package com.tencentcloudapi.cls.android.plugin;

import android.content.Context;

import com.tencentcloudapi.cls.android.ClsConfigOptions;

import java.util.Map;

/**
 * @author farmerx
 * @date 2022/03/10
 */
public interface IPlugin {

    String name();

    String version();

    void init(Context context, ClsConfigOptions config, Map<String, String> ext);
}
