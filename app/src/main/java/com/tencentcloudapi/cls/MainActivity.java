package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;

import com.tencentcloudapi.cls.android.CLSAdapter;
import com.tencentcloudapi.cls.android.CLSConfig;
import com.tencentcloudapi.cls.android.CLSLog;
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

        System.out.println("0000------");

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
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

    public void singletonInit(Context context) {
        CLSAdapter adapter = CLSAdapter.getInstance();
        // 添加网络探测插件
        adapter.addPlugin(new CLSNetDiagnosisPlugin());

        CLSConfig config = new CLSConfig(context);

        config.endpoint = "ap-guangzhou-open.cls.tencentcs.com";
        config.accessKeyId = "1";
        config.accessKeySecret = "1";
        config.pluginAppId = "666233";
        config.topicId = "1";
        // 发布时，建议关闭，即配置为config.debuggable = false。
        config.debuggable = true;
        adapter.init(config);
    }
}