# tencentcloud-cls-sdk-android
腾讯云CLS Android SDK

## 接入android日志上报sdk
您需要在Android Studio工程对应模块下的build.gradle文件中增加以下依赖。

```
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '1.0.10')
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
         
        // NetworkUtils.getLocalMachineIP() 获取本地网卡ip，如果不指定，默认填充服务端接收到的网络出口ip
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

### 配置参数详解

| 参数                | 类型   | 描述                                                         |
| ------------------- | ------ | ------------------------------------------------------------ |
| TotalSizeInBytes    | Int64  | 实例能缓存的日志大小上限，默认为 100MB。       |
| MaxSendThreadCount  | Int64  | client能并发的最多"goroutine"的数量，默认为50 |
| MaxBlockSec         | Int    | 如果client可用空间不足，调用者在 send 方法上的最大阻塞时间，默认为 60 秒。<br/>如果超过这个时间后所需空间仍无法得到满足，send 方法会抛出TimeoutException。如果将该值设为0，当所需空间无法得到满足时，send 方法会立即抛出 TimeoutException。如果您希望 send 方法一直阻塞直到所需空间得到满足，可将该值设为负数。 |
| MaxBatchSize        | Int64  | 当一个Batch中缓存的日志大小大于等于 batchSizeThresholdInBytes 时，该 batch 将被发送，默认为 512 KB，最大可设置成 5MB。 |
| MaxBatchCount       | Int    | 当一个Batch中缓存的日志条数大于等于 batchCountThreshold 时，该 batch 将被发送，默认为 4096，最大可设置成 40960。 |
| LingerMs            | Int64  | Batch从创建到可发送的逗留时间，默认为 2 秒，最小可设置成 100 毫秒。 |
| Retries             | Int    | 如果某个Batch首次发送失败，能够对其重试的次数，默认为 10 次。<br/>如果 retries 小于等于 0，该 ProducerBatch 首次发送失败后将直接进入失败队列。 |
| MaxReservedAttempts | Int    | 每个Batch每次被尝试发送都对应着一个Attemp，此参数用来控制返回给用户的 attempt 个数，默认只保留最近的 11 次 attempt 信息。<br/>该参数越大能让您追溯更多的信息，但同时也会消耗更多的内存。 |
| BaseRetryBackoffMs  | Int64  | 首次重试的退避时间，默认为 100 毫秒。 client采样指数退避算法，第 N 次重试的计划等待时间为 baseRetryBackoffMs * 2^(N-1)。 |
| MaxRetryBackoffMs   | Int64  | 重试的最大退避时间，默认为 50 秒。                           |



## 接入Android App网络数据

### 引入库文件
您需要在Android Studio工程对应模块下的build.gradle文件中增加以下依赖。

```
    implementation(group: 'com.tencentcloudapi.cls', name: 'cls-network-diagnosis-reporter-android', version: '1.0.5')
    implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '1.0.5')
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


