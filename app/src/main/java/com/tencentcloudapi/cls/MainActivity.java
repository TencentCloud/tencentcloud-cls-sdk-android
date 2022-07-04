package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;

import com.tencentcloudapi.cls.android.CLSAdapter;
import com.tencentcloudapi.cls.android.CLSConfig;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.producer.AsyncProducerClient;
import com.tencentcloudapi.cls.android.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.android.producer.common.LogException;
import com.tencentcloudapi.cls.android.producer.request.SearchLogRequest;
import com.tencentcloudapi.cls.android.producer.response.SearchLogResponse;
import com.tencentcloudapi.cls.android.producer.util.NetworkUtils;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosisPlugin;


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

        String endpoint = "ap-guangzhou.cls.tencentcs.com";
        // API密钥 secretId，必填
        String secretId = "";
        // API密钥 secretKey，必填
        String secretKey = "";

        final AsyncProducerConfig config = new AsyncProducerConfig(this, endpoint, secretId, secretKey,"", NetworkUtils.getLocalMachineIP());

        // 构建一个客户端实例
        final AsyncProducerClient client = new AsyncProducerClient(config);

        try {
            SearchLogResponse resp = client.SearchLog(new SearchLogRequest(
                    "",
                    "",
                    "2022-07-04 10:49:42",
                    "2022-07-04 15:49:42",
                    "",
                    100
            ));
            System.out.println(resp.GetAllHeaders());
            System.out.println(resp.GetResult());
        } catch (LogException e) {
            e.printStackTrace();
        }


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