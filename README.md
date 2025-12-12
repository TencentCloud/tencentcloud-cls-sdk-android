# tencentcloud-cls-sdk-android
腾讯云CLS Android SDK

## 接入android日志上报sdk
您需要在Android Studio工程对应模块下的build.gradle文件中增加以下依赖。

```
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '2.0.1')
```
### 密钥信息

secretId和secretKey为云API密钥，密钥信息获取请前往[密钥获取](https://console.cloud.tencent.com/cam/capi)。并请确保云API密钥关联的账号具有相应的[SDK上传日志权限](https://cloud.tencent.com/document/product/614/68374#.E4.BD.BF.E7.94.A8-api-.E4.B8.8A.E4.BC.A0.E6.95.B0.E6.8D.AE)

## 参数使用说明

| 参数名 | 类型 | 说明 |
|--------|------|------|
| `endpoint` | `String` | 日志服务CLS endpoint接入域名，如：`ap-guangzhou.cls.tencentcs.com` |
| `host` | `String` | 主机地址 |
| `credential` | `Credential` | 认证信息 |
| `topicId` | `String` | 日志主题ID |
| `flushInterval` | `int` | 两次数据发送的最小时间间隔，单位毫秒，默认5秒 |
| `flushBulkSize` | `int` | flush日志的最大条目数，默认50，最大4096 |
| `maxCacheSize` | `long` | 本地缓存上限值，单位byte，默认32MB |
| `mLogEnabled` | `boolean` | 是否开启打印日志 |
| `mNetworkTypePolicy` | `int` | 网络上传策略，支持2G/3G/4G/WIFI/5G |
| `appVersion` | `String` | 应用版本 |
| `appName` | `String` | 应用名称 |
| `tag` | `Map<String, String>` | 标签键值对 |
| `callback` | `TrackLogEventCallBack` | 回调函数 |

##
```agsl
1、SDK 本地数据库默认缓存数据的上限值为 32 MB。支持通过 setMaxCacheSize() 方法来设定缓存数据的上限值。参数单位为 byte
2、默认的 flushBulkSize 为 100 条，默认的 flushInterval 为 15 秒。满足条件后，cls SDK 会将数据 lz4 压缩后，批量发送到cls。
3、当存储数量达到上限值，会依次丢弃老数据，保留最新的数据

数据flush条件：
1、用户主动触发
2、与上次发送的时间间隔是否大于 flushInterval
```

## 日志上传Demo

```
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
        sendLog(this);
    }


    public void singletonInit(Context context) {
        ClsConfigOptions clsConfigOptions = new ClsConfigOptions(
                "ap-guangzhou-open.cls.tencentcs.com",
                "1",
                new Credential("", ""));
        clsConfigOptions.enableLog(true);
        ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);
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
   
```

### 配置参数详解



## 接入Android App网络数据

### 引入库文件
您需要在Android Studio工程对应模块下的build.gradle文件中增加以下依赖。

```
    implementation(group: 'com.tencentcloudapi.cls', name: 'cls-network-diagnosis-reporter-android', version: '2.0.2')
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '2.0.2')
```

接入Android应用的网络数据所涉及的依赖包说明如下表所示。

* tencentcloud-log-android-sdk	核心SDK，用于采集Android应用的网络数据到日志服务。
* cls-android-network-diagnosis-reporter  网络探测插件。

### 配置权限
```
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### 网络权限问题

##### cls日志上传是基于http的，android 9.0使用HttpUrlConnection进行http请求会出现以下异常。
```
    W/System.err: java.io.IOException: Cleartext HTTP traffic to **** not permitted
```
###### 解决办法

* 在res文件夹下创建一个xml文件夹，然后创建一个network_security_config.xml文件，文件内容如下： (ap-guangzhou.cls.tencentcs.com 按需要制定)
```
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!--<base-config cleartextTrafficPermitted="true" />-->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">ap-guangzhou.cls.tencentcs.com</domain>
    </domain-config>
</network-security-config>
```
* 在AndroidManifest.xml文件下的application标签增加以下属性

```
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    ...
/>
```


### 混淆异常
lz4 压缩算法混淆异常，需要skip掉

```
 -keep class net.jpountz.lz4.** { *; } 
```

### 配置接入服务

* 添加Application类，即在$PROJECT/app/src/main/AndroidManifest.xml文件中增加Application类。
例如添加MyApplication类，配置示例如下：
  
```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.tencentcloudapi.cls">
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tencentcloudclssdkandroid"
        android:networkSecurityConfig="@xml/network_security_config"
        >
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```
IDE将根据Android Studio提示，自动创建一个名为MyApplication的类添加到当前项目。

* 在MyApplication.onCreate方法中，增加如下初始化代码。

```
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
    }


     public void singletonInit(Context context) {
        ClsConfigOptions clsConfigOptions = new ClsConfigOptions(
                "https://ap-guangzhou-open.cls.tencentcs.com",
                "[日志主题id]",
                new Credential("[secret_id]", "[secret_key]"));
        clsConfigOptions.enableLog(true);
        clsConfigOptions.addTag("cls_android", "2.0.0");
        ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);
        // 添加插件，自定义插件上报CLS内容
        INetworkDiagnosisPlugin clsNetDiagnosisPlugin = new NetworkDiagnosisPlugin();
        clsNetDiagnosisPlugin.addCustomField("test", "tag");
        clsNetDiagnosisPlugin.setAppCredentialToken("oiNWM4NmQxZGQtYWIyNi00ZmJhLTk3ZTMtNTRmNDZkMWZiZmRhIiwicmVnaW9uIjoiYXAtZ3Vhbmd6aG91LW9wZW4iLCJ0b3BpY19pZCI6ImJiNTA5NDYzLWFlZGEtNDgyZi1hZjg3LTc5NTAwN2Q5MjYzMSJ9");
        ClsDataAPI.sharedInstance(context).
                addPlugin(clsNetDiagnosisPlugin).
                startPlugin(context);
    }

}
```

### CLSConfig

CLSConfig类定义了关键的配置字段

### CLSAdapter

CLSAdapter类是插件的管理类。
```agsl

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
        CLSNetworkDiagnosis.getInstance().mtr(request, new INetworkDiagnosis.Callback() {
            @Override
            public void onComplete(INetworkDiagnosis.Response response) {
                CLSLog.i("onComplete",response.content);
            }
        });
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


```



