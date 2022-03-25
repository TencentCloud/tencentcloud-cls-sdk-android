package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;

import com.tencentcloudapi.cls.android.CLSAdapter;
import com.tencentcloudapi.cls.android.CLSConfig;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosisPlugin;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        CLSAdapter adapter = CLSAdapter.getInstance();
//        // 添加网络探测插件。
//        adapter.addPlugin(new CLSNetDiagnosisPlugin());
//
//        CLSConfig config = new CLSConfig(this);
//        config.endpoint = "ap-guangzhou.cls.tencentcs.com";
//        config.accessKeyId = "222";
//        config.accessKeySecret = "222";
//        config.pluginAppId = "123456";
//        config.topicId = "2222";
//        // 发布时，建议关闭，即配置为config.debuggable = false。
//        config.debuggable = true;
//        adapter.init(config);

        singletonInit(this);

        CLSNetDiagnosis.getInstance().traceroute("www.tencentcloud.com",  new CLSNetDiagnosis.Output(){
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
        });

    }

    public void singletonInit(Context context) {
        CLSAdapter adapter = CLSAdapter.getInstance();
        // 添加网络探测插件
        adapter.addPlugin(new CLSNetDiagnosisPlugin());

        CLSConfig config = new CLSConfig(context);

        config.endpoint = "ap-guangzhou.cls.tencentcs.com";
        config.accessKeyId = "";
        config.accessKeySecret = "";
        config.pluginAppId = "666233";
        config.topicId = "";
        // 发布时，建议关闭，即配置为config.debuggable = false。
        config.debuggable = true;
        adapter.init(config);
    }
}