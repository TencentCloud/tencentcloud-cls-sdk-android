package com.tencentcloudapi.cls;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.Credential;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.ClsDataAPI;
import com.tencentcloudapi.cls.android.exceptions.InvalidDataException;
import com.tencentcloudapi.cls.android.plugin.INetworkDiagnosisPlugin;
import com.tencentcloudapi.cls.android.producer.common.LogItem;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetworkDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.INetworkDiagnosis;
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
            clsDNSPing();
            clsPing();
            clsMTR();
            clsHttpPing(this);
            clsTcpPing();
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
                "a211",
                new Credential("AK", "rX5"));
        clsConfigOptions.enableLog(true);
        clsConfigOptions.addTag("cls_android", "3.0.0");
        ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);
        // 添加插件，自定义插件上报CLS内容
        INetworkDiagnosisPlugin clsNetDiagnosisPlugin = new NetworkDiagnosisPlugin();
        clsNetDiagnosisPlugin.addCustomField("test", "tag");
        clsNetDiagnosisPlugin.setAppCredentialToken("");
        ClsDataAPI.sharedInstance(context).
                addPlugin(clsNetDiagnosisPlugin).
                startPlugin(context);
    }

    public void clsHttpPing(Context context) throws NoSuchAlgorithmException {
        CLSNetworkDiagnosis.HttpRequest request = new CLSNetworkDiagnosis.HttpRequest();
        request.headerOnly = true;
        request.downloadBytesLimit = 1024;
        //可选参数，证书检验回调。getSSLContext的配置参考下文。
        request.credential = new INetworkDiagnosis.HttpCredential(getSSLContext(context), null);
        //可选参数，设置当次网络探测的扩展业务参数。
        request.extension = new HashMap<String, String>() {
            {
                put("custom_field", "httpPing");
            }
        };
        request.domain = "https://ap-guangzhou.cls.tencentcs.com";
        CLSNetworkDiagnosis.getInstance().http(request);
    }

    public void clsDNSPing() {
        INetworkDiagnosis.DnsRequest request = new INetworkDiagnosis.DnsRequest();
        request.extension = new HashMap<String, String>() {
            {
                put("custom_field", "dns");
            }
        };
        request.domain = "ap-guangzhou-open.cls.tencentcs.com";
        CLSNetworkDiagnosis.getInstance().dns(request);
    }

    public void clsPing() {
        INetworkDiagnosis.PingRequest request = new INetworkDiagnosis.PingRequest();
        request.extension = new HashMap<String, String>() {
            {
                put("custom_field", "ping");
            }
        };
        request.domain = "ap-guangzhou-open.cls.tencentcs.com";
        CLSNetworkDiagnosis.getInstance().ping(request);
    }

    public void clsMTR() {
        Map<String, String> customFiled = new LinkedHashMap<>();
        customFiled.put("cls", "custom field");
        INetworkDiagnosis.MtrRequest request = new INetworkDiagnosis.MtrRequest();
        request.protocol = INetworkDiagnosis.MtrRequest.Protocol.ICMP;
        request.extension = new HashMap<String, String>() {
            {
                put("custom_field", "mtr");
            }
        };
        request.domain = "ap-guangzhou-open.cls.tencentcs.com";
        CLSNetworkDiagnosis.getInstance().mtr(request);
    }

    public void clsTcpPing() {
        INetworkDiagnosis.TcpPingRequest request = new INetworkDiagnosis.TcpPingRequest();
        request.extension = new HashMap<String, String>() {
            {
                put("custom_field", "ping");
            }
        };
        request.domain = "ap-guangzhou-open.cls.tencentcs.com";
        request.port = 80;
        request.payload = "hello";
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