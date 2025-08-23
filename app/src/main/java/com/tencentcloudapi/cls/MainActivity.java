package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.Credential;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.ClsDataAPI;
import com.tencentcloudapi.cls.android.exceptions.InvalidDataException;
import com.tencentcloudapi.cls.android.plugin.AbstractPlugin;
import com.tencentcloudapi.cls.android.producer.common.LogItem;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosisPlugin;

import java.util.LinkedHashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        singletonInit(this);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        clsNetDiagnosis();
        sendLog(this);
    }


    public void singletonInit(Context context) {
        ClsConfigOptions clsConfigOptions = new ClsConfigOptions(
                "ap-guangzhou-open.cls.tencentcs.com",
                "1",
                new Credential("", ""));
        clsConfigOptions.enableLog(true);
        ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);
        // 添加插件，自定义插件上报CLS内容
        AbstractPlugin clsNetDiagnosisPlugin = new CLSNetDiagnosisPlugin();
        clsNetDiagnosisPlugin.addCustomField("test", "tag");
        ClsDataAPI.sharedInstance(context).
                addPlugin(clsNetDiagnosisPlugin).
                startPlugin(context);
    }

    public void clsNetDiagnosis() {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls","custom field");
        CLSNetDiagnosis.getInstance().tcpPing("www.tencentcloud.com", 80, new CLSNetDiagnosis.Output(){
            @Override
            public void write(String line) {
                System.out.println(line);
            }
        }, new CLSNetDiagnosis.Callback() {
            @Override
            public void onComplete(String result) {
                // result为探测结果，JSON格式。
                CLSLog.d("TraceRoute", String.format("traceRoute result: %s", result));
            }
        }, customFiled);
    }

    public void sendLog(Context context) {
        LogItem logItem = new LogItem();
        logItem.SetTime(System.currentTimeMillis());
        logItem.PushBack("hello", "world");
        try {
            ClsDataAPI.sharedInstance(context).trackLog(logItem);
        } catch (InvalidDataException e) {
            CLSLog.printStackTrace(e);
        }
    }

}