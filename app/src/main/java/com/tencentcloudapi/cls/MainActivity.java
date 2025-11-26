package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.Credential;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.ClsDataAPI;
import com.tencentcloudapi.cls.android.exceptions.InvalidDataException;
import com.tencentcloudapi.cls.android.plugin.AbstractPlugin;
import com.tencentcloudapi.cls.android.producer.common.LogItem;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetworkDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.NetworkDiagnosisPlugin;


import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        singletonInit(this);
        try {
            clsNetDiagnosisMTR(this);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public SSLContext getSSLContext(Context context) throws NoSuchAlgorithmException {
        return SSLContext.getDefault();
    }

    public void singletonInit(Context context) {
        ClsConfigOptions clsConfigOptions = new ClsConfigOptions(
                "https://ap-guangzhou-open.cls.tencentcs.com",
                "88-46eea4769a05",
                new Credential("", ""));
        clsConfigOptions.enableLog(true);
        clsConfigOptions.addTag("cls_android", "2.0.0");
        ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);
        // 添加插件，自定义插件上报CLS内容
        AbstractPlugin clsNetDiagnosisPlugin = new NetworkDiagnosisPlugin();
        clsNetDiagnosisPlugin.addCustomField("test", "tag");
        ClsDataAPI.sharedInstance(context).
                addPlugin(clsNetDiagnosisPlugin).
                startPlugin(context);
    }

    public void clsNetDiagnosisHttp(Context context) throws NoSuchAlgorithmException {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls", "custom field");
        com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.HttpRequest request = new CLSNetworkDiagnosis.HttpRequest();
        request.context = "<your http context id>";
        request.headerOnly = true;
        request.downloadBytesLimit = 1024;
//可选参数，证书检验回调。getSSLContext的配置参考下文。
        request.credential = new com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.HttpCredential(getSSLContext(context), null);
//可选参数，设置当次网络探测的扩展业务参数。
        request.extension = new HashMap<String, String>() {
            {
                put("custom_key", "custom_value");
            }
        };

        request.domain = "https://www.baidu.com";
        CLSNetworkDiagnosis.getInstance().http(request);
    }

    public void clsNetDiagnosisDNS(Context context) throws NoSuchAlgorithmException {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls", "custom field");
        com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.DnsRequest request = new com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.DnsRequest();
        request.context = "<your http context id>";
        request.extension = new HashMap<String, String>() {
            {
                put("custom_key", "custom_value");
            }
        };

        request.domain = "ap-guangzhou-open.cls.tencentcs.com";
        CLSNetworkDiagnosis.getInstance().dns(request);
    }

    public void clsNetDiagnosisPing(Context context) throws NoSuchAlgorithmException {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls", "custom field");
        com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.PingRequest request = new com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.PingRequest();
        request.context = "<your http context id>";
        request.extension = new HashMap<String, String>() {
            {
                put("custom_key", "custom_value");
            }
        };

        request.domain = "www.baidu.com";
        CLSNetworkDiagnosis.getInstance().ping(request);
    }

    public void clsNetDiagnosisMTR(Context context) throws NoSuchAlgorithmException {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls", "custom field");
        com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.MtrRequest request = new com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.MtrRequest();
        request.context = "<your http context id>";
        request.protocol = com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.MtrRequest.Protocol.UDP;
        request.extension = new HashMap<String, String>() {
            {
                put("custom_key", "custom_value");
            }
        };

        request.domain = "ap-guangzhou-open.cls.tencentcs.com";
        CLSNetworkDiagnosis.getInstance().mtr(request);
    }

    public void clsNetDiagnosis() {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls", "custom field");
        com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis.TcpPingRequest request = new CLSNetworkDiagnosis.TcpPingRequest();
        request.domain = "www.baidu.com";
//可选参数。
        request.multiplePortsDetect = true; //启用多网卡探测。
//可选参数，设置当次网络探测的扩展业务参数。
        request.extension = new HashMap<String, String>() {
            {
                put("custom_key", "custom_value");
            }
        };
        CLSNetworkDiagnosis.getInstance().tcpPing(request);
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