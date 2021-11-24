package com.example.android.demo;

import android.app.Application;
import android.content.Context;
import com.tencent.cls.producer.AsyncClient;
import com.tencent.cls.producer.util.NetworkUtils;


public class MainApplication extends Application {
    private static Context mContext;
    private AsyncClient clsClient;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        String endpoint = "ap-guangzhou.cls.tencentcs.com";
        // API密钥 secretId，必填
        String secretId = "*";
        // API密钥 secretKey，必填
        String secretKey = "*";

        // 构建一个客户端实例
        this.clsClient = new AsyncClient(endpoint, secretId, secretKey, NetworkUtils.getLocalMachineIP(), 5, 10);
    }

    public static Context getInstance() {
        return mContext;
    }

    public AsyncClient getAsyncClientInstance() {
        return clsClient;
    }


}
