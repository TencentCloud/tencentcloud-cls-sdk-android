package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

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

        CLSAdapter adapter = CLSAdapter.getInstance();
        // 添加网络探测插件。
        adapter.addPlugin(new CLSNetDiagnosisPlugin());

        CLSConfig config = new CLSConfig(this);
        config.endpoint = "ap-guangzhou.cls.tencentcs.com";
        config.accessKeyId = "";
        config.accessKeySecret = "";
        config.pluginAppId = "123456";
        config.topicId = "";
        // 发布时，建议关闭，即配置为config.debuggable = false。
        config.debuggable = true;
        adapter.init(config);

        CLSNetDiagnosis.getInstance().ping("www.tencentcloud.com",  new CLSNetDiagnosis.Output(){
            @Override
            public void write(String line) {
                System.out.println(line);
            }
        }, new CLSNetDiagnosis.Callback() {
            @Override
            public void onComplete(String result) {
                // result为探测结果，JSON格式。
                CLSLog.d("hh-----------h", String.format("ping result: %s", result));
            }
        });

    }
}