# tencentcloud-cls-sdk-android
腾讯云CLS Android SDK

## 接入Android App网络数据

### 引入库文件
您需要在Android Studio工程对应模块下的build.gradle文件中增加以下依赖。

```
    implementation(group: 'com.tencentcloudapi.cls', name: 'cls-network-diagnosis-reporter-android', version: '1.0.1')
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '1.0.1')
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
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
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



## 日志上传Demo

```
public static void main(String[] args) {
        String endpoint = "ap-guangzhou.cls.tencentcs.com";
        // API密钥 secretId，必填
        String secretId = "";
        // API密钥 secretKey，必填
        String secretKey = "";
        // 日志主题ID，必填
        String topicId = "";

        final AsyncProducerConfig config = new AsyncProducerConfig(endpoint, secretId, secretKey, "", NetworkUtils.getLocalMachineIP());

        // 构建一个客户端实例
        final AsyncProducerClient client = new AsyncProducerClient(config);

        for (int i = 0; i < 10000; ++i) {
            List<LogItem> logItems = new ArrayList<>();
            int ts = (int) (System.currentTimeMillis() / 1000);
            LogItem logItem = new LogItem(ts);
            logItem.PushBack(new LogContent("__CONTENT__", "你好，我来自深圳|hello world"));
            logItem.PushBack(new LogContent("city", "guangzhou"));
            logItem.PushBack(new LogContent("logNo", Integer.toString(i)));
            logItem.PushBack(new LogContent("__PKG_LOGID__", (String.valueOf(System.currentTimeMillis()))));
            logItems.add(logItem);
            client.putLogs(topicId, logItems, result -> System.out.println(result.toString()));
        }
        client.close();
}
   
```

### 检索接口相关文档

https://cloud.tencent.com/document/product/614/16875
