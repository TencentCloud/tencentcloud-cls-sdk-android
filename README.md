# tencentcloud-cls-sdk-android
腾讯云CLS Android SDK

## 接入android日志上报sdk
您需要在Android Studio工程对应模块下的build.gradle文件中增加以下依赖。

```
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '2.0.0')
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
3、本地缓存日志数目是否大于 flushBulkSize
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
    implementation(group: 'com.tencentcloudapi.cls', name: 'cls-network-diagnosis-reporter-android', version: '2.0.0')
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '2.0.0')
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

}
```

### CLSConfig

CLSConfig类定义了关键的配置字段

### CLSAdapter

CLSAdapter类是插件的管理类。

### Ping网络探测

方法1：
```
/**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void ping(String domain, Output output, Callback callback) {
        this.ping(domain, 10, DEFAULT_PING_BYTES, output, callback);
    }
```

方法2：

```
 /**
     * @param domain   目标 host，如 cloud.tencent.com
     * @param maxTimes 探测的次数
     * @param size     探测包体积
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void ping(String domain, int maxTimes, int size, Output output, Callback callback) {
        Diagnosis.ping(domain, maxTimes, size, output, new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.PING, result, callback);
            }
        });
    }
```

调用接口:

```
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
       
```

### TCPPing 网络探测

方法1： 

```
    /**
     * @param domain   目标 host，如：cloud.tencent.com
     * @param port     目标端口，如：80
     * @param output   输出 callback                
     * @param callback 回调 callback
     */
    public void tcpPing(String domain, int port, Output output, Callback callback) {
        this.tcpPing(domain, port, 10, DEFAULT_TIMEOUT, output, callback);
    }
```

方法2：

```
/**
  * @param domain   目标 host，如：cloud.tencent.com
  * @param port     目标端口，如：80
  * @param maxTimes 探测的次数
  * @param timeout  单次探测的超时时间
  * @param output   输出 callback   
  * @param callback 回调 callback
  */
public void tcpPing(String domain, int port, int maxTimes, int timeout, Output output, Callback callback) {
    Diagnosis.tcpPing(domain, port, maxTimes, timeout, output, new Callback() {
         @Override
         public void onComplete(String result) {
             report(Type.TCPPING, result, callback);
         }
    });
}
```

调用方法：

```
        CLSNetDiagnosis.getInstance().tcpPing("www.tencentcloud.com", 80, new CLSNetDiagnosis.Output(){
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
```

### TraceRoute 网络探测


方法1：

```
    /**
     * @param domain 目标 host，如：cloud.tencent.com
     * @param output 输出 callback
     * @param callback 回调 callback
     */
    public void traceroute(String domain, Output output, Callback callback) {
        Traceroute traceroute = new Traceroute(new Traceroute.Config(domain), new Callback() {
            @Override
            public void onComplete(String result) {
                report(Type.TRACEROUTE, result, callback);
            }
        }, output);
        traceroute(traceroute);
    }
```

方法2: 

```
    /**
     *
     * @param domain 目标 host，如：cloud.tencent.com
     * @param maxHop
     * @param countPerRoute
     * @param output   输出 callback
     * @param callback 回调 callback
     */
    public void traceroute(String domain, int maxHop, int countPerRoute, Output output, Callback callback) {
        Traceroute.Config config =  new Traceroute.Config(domain);
        config.setMaxHop(maxHop);
        config.setCountPerRoute(countPerRoute);
        Traceroute traceroute = new Traceroute(new Traceroute.Config(domain), callback, output);
        traceroute(traceroute);
    }
```

调用方法：

```
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
```

### HttpPing网络探测

```
CLSNetDiagnosis.getInstance().httpPing("https://www.tencentcloud.com",  new CLSNetDiagnosis.Output(){
            @Override
            public void write(String line) {
                System.out.println(line);
            }
        }, new CLSNetDiagnosis.Callback() {
            @Override
            public void onComplete(String result) {
                // result为探测结果，JSON格式。
                CLSLog.d("HttpPing", String.format("traceRoute result: %s", result));
            }
        });
```


