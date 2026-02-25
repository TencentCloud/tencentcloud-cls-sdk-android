# tencentcloud-cls-sdk-android

腾讯云 CLS Android SDK，提供 **日志上传** 和 **网络探测** 两大功能模块。

---

## 目录

- [一、日志上传](#一日志上传)
  - [1.1 引入依赖](#11-引入依赖)
  - [1.2 密钥信息](#12-密钥信息)
  - [1.3 权限配置](#13-权限配置)
  - [1.4 初始化配置（ClsConfigOptions）](#14-初始化配置clsconfigoptions)
  - [1.5 核心 API（ClsDataAPI）](#15-核心-apiclsdataapi)
  - [1.6 快速接入示例](#16-快速接入示例)
  - [1.7 数据 Flush 机制说明](#17-数据-flush-机制说明)
  - [1.8 混淆配置](#18-混淆配置)
- [二、网络探测](#二网络探测)
  - [2.1 引入依赖](#21-引入依赖)
  - [2.2 权限配置](#22-权限配置)
  - [2.3 网络安全配置](#23-网络安全配置)
  - [2.4 初始化与插件接入](#24-初始化与插件接入)
  - [2.5 网络探测 API](#25-网络探测-api)

---

## 一、日志上传

### 1.1 引入依赖

在 Android Studio 工程对应模块的 `build.gradle` 文件中增加以下依赖：

```groovy
implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '3.0.1')
```

---

### 1.2 密钥信息

`secretId` 和 `secretKey` 为云 API 密钥，请前往 [密钥获取](https://console.cloud.tencent.com/cam/capi) 获取。  
并请确保云 API 密钥关联的账号具有相应的 [SDK 上传日志权限](https://cloud.tencent.com/document/product/614/68374#.E4.BD.BF.E7.94.A8-api-.E4.B8.8A.E4.BC.A0.E6.95.B0.E6.8D.AE)。

---

### 1.3 权限配置

在 `AndroidManifest.xml` 中添加以下权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

---

### 1.4 初始化配置（ClsConfigOptions）

`ClsConfigOptions` 是 SDK 的核心配置类，构造函数签名如下：

```java
ClsConfigOptions(String endpoint, String topicId, Credential credential)
```

#### 构造参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| `endpoint` | `String` | 必填 | CLS 接入域名，如 `ap-guangzhou.cls.tencentcs.com`，支持 `http://` 或 `https://` 前缀，不填前缀默认使用 `https://` |
| `topicId` | `String` | 必填 | 日志主题 ID |
| `credential` | `Credential` | 必填 | 认证信息，包含 `secretId`、`secretKey`（可选 `token`） |

#### 可选配置方法

| 方法 | 参数类型 | 默认值 | 说明 |
|------|----------|--------|------|
| `setFlushInterval(int)` | `int`（毫秒） | `5000`（5秒） | 两次数据发送的最小时间间隔，最小值 5 秒 |
| `setFlushBulkSize(int)` | `int` | `50` | 单次 flush 最大日志条数，范围 50 ~ 4096 |
| `setMaxCacheSize(long)` | `long`（byte） | `33554432`（32MB） | 本地缓存上限，最小 16MB，超出后丢弃最旧数据 |
| `enableLog(boolean)` | `boolean` | `false` | 是否开启 SDK 内部日志打印 |
| `setNetworkTypePolicy(int)` | `int` | 全网络类型 | 允许上传数据的网络类型，可组合 `NetworkType.TYPE_2G`、`TYPE_3G`、`TYPE_4G`、`TYPE_5G`、`TYPE_WIFI` |
| `setAppVersion(String)` | `String` | 自动获取 | 应用版本号 |
| `setAppName(String)` | `String` | 自动获取 | 应用名称 |
| `addTag(String key, String value)` | `String, String` | — | 添加全局标签键值对，随每条日志上报 |
| `setCallback(TrackLogEventCallBack)` | `TrackLogEventCallBack` | `null` | 设置日志上报结果回调 |

#### Credential 参数说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `secretId` | `String` | 云 API 密钥 ID |
| `secretKey` | `String` | 云 API 密钥 Key |
| `token` | `String` | 临时密钥 Token（使用临时密钥时填写） |

---

### 1.5 核心 API（ClsDataAPI）

#### 初始化

```java
// 初始化 SDK（建议在 Application.onCreate 中调用）
ClsDataAPI.startWithConfigOptions(Context context, ClsConfigOptions clsConfigOptions);
```

#### 获取实例

```java
// 获取单例（需在 startWithConfigOptions 之后调用）
ClsDataAPI.sharedInstance(Context context);
```

#### 上报日志

```java
// 上报日志到默认 topicId
void trackLog(LogItem logItem) throws InvalidDataException;

// 上报日志到指定 topicId
void trackLog(String topicId, LogItem logItem) throws InvalidDataException;
```

**LogItem 参数说明：**

| 方法 | 说明 |
|------|------|
| `SetTime(long timestamp)` | 设置日志时间戳（毫秒），必填且必须大于 0 |
| `PushBack(String key, String value)` | 添加日志键值对，至少需要一对 |

#### 主动触发上报

```java
// 立即将缓存中的日志发送到 CLS
void flush();
```

#### 清空本地缓存

```java
// 删除本地数据库中所有未上报的日志
void deleteAll();
```

#### 停止上报线程

```java
// 停止后台上报线程
void stopTrackThread();
```

#### 回调接口（TrackLogEventCallBack）

```java
public interface TrackLogEventCallBack {
    int SUCCESS = 0;
    int FAIL    = 1;
    void onCompletion(int status, String message);
}
```

---

### 1.6 快速接入示例

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initCls(this);
        sendLog(this);
    }

    private void initCls(Context context) {
        ClsConfigOptions clsConfigOptions = new ClsConfigOptions(
                "ap-guangzhou.cls.tencentcs.com",   // endpoint
                "[日志主题 ID]",                      // topicId
                new Credential("[secret_id]", "[secret_key]"));

        clsConfigOptions
                .enableLog(true)                    // 开启调试日志
                .setFlushInterval(10 * 1000)        // 10 秒上报一次
                .setFlushBulkSize(100)              // 每批最多 100 条
                .setMaxCacheSize(64 * 1024 * 1024L) // 本地缓存 64MB
                .addTag("app", "demo");             // 全局标签

        ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);
    }

    private void sendLog(Context context) {
        LogItem logItem = new LogItem();
        logItem.SetTime(System.currentTimeMillis());
        logItem.PushBack("level", "info");
        logItem.PushBack("message", "hello cls");
        try {
            ClsDataAPI.sharedInstance(context).trackLog(logItem);
        } catch (InvalidDataException e) {
            CLSLog.printStackTrace(e);
        }
    }
}
```

---

### 1.7 数据 Flush 机制说明

```
触发 flush 的条件（满足任一即触发）：
1. 调用 flush() 主动触发
2. 距上次发送时间超过 flushInterval（默认 5 秒）
3. 缓存日志条数达到 flushBulkSize（默认 50 条）

其他说明：
- 日志在本地以 SQLite 数据库缓存，上限默认 32MB（最小 16MB）
- 上报前使用 LZ4 算法压缩，批量发送
- 缓存达到上限时，自动丢弃最旧的数据，保留最新数据
- 网络不可用或不符合网络策略时，数据保留在本地等待下次 flush
```

---

### 1.8 混淆配置

LZ4 压缩算法需要跳过混淆，在 `proguard-rules.pro` 中添加：

```
-keep class net.jpountz.lz4.** { *; }
```

---

## 二、网络探测

### 2.1 引入依赖

在 `build.gradle` 中同时引入核心 SDK 和网络探测插件：

```groovy
implementation(group: 'com.tencentcloudapi.cls', name: 'tencentcloud-cls-sdk-android', version: '3.0.1')
implementation(group: 'com.tencentcloudapi.cls', name: 'cls-network-diagnosis-reporter-android', version: '3.0.1')
```

| 依赖包 | 说明 |
|--------|------|
| `tencentcloud-cls-sdk-android` | 核心 SDK，负责将探测结果上报到 CLS |
| `cls-network-diagnosis-reporter-android` | 网络探测插件，提供 HTTP Ping、DNS、Ping、MTR、TCP Ping 等能力 |

---

### 2.2 权限配置

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

---

### 2.3 网络安全配置

Android 9.0（API 28）及以上默认禁止明文 HTTP 流量，需配置网络安全策略。

**步骤一：** 在 `res/xml/` 目录下创建 `network_security_config.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <!-- 按需替换为实际的 CLS 域名 -->
        <domain includeSubdomains="true">ap-guangzhou.cls.tencentcs.com</domain>
    </domain-config>
</network-security-config>
```

**步骤二：** 在 `AndroidManifest.xml` 的 `<application>` 标签中引用：

```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    ...
/>
```

完整 `AndroidManifest.xml` 示例：

```xml
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
        android:networkSecurityConfig="@xml/network_security_config">
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

---

### 2.4 初始化与插件接入

在 `Application.onCreate` 或 `Activity.onCreate` 中完成初始化：

```java
private void initCls(Context context) {
    // 1. 初始化核心 SDK
    ClsConfigOptions clsConfigOptions = new ClsConfigOptions(
            "https://ap-guangzhou-open.cls.tencentcs.com",
            "[日志主题 ID]",
            new Credential("[secret_id]", "[secret_key]"));
    clsConfigOptions.enableLog(true);
    clsConfigOptions.addTag("cls_android", "3.0.1");
    ClsDataAPI.startWithConfigOptions(context, clsConfigOptions);

    // 2. 创建并配置网络探测插件
    INetworkDiagnosisPlugin clsNetDiagnosisPlugin = new NetworkDiagnosisPlugin();
    clsNetDiagnosisPlugin.addCustomField("env", "production");  // 可选：添加自定义字段
    clsNetDiagnosisPlugin.setAppCredentialToken("[移动端接入 Token]"); // 从 CLS 控制台获取

    // 3. 注册并启动插件
    ClsDataAPI.sharedInstance(context)
            .addPlugin(clsNetDiagnosisPlugin)
            .startPlugin(context);
}
```

#### INetworkDiagnosisPlugin 接口说明

| 方法 | 说明 |
|------|------|
| `addCustomField(String key, String value)` | 添加自定义字段，随探测结果一起上报 |
| `setAppCredentialToken(String token)` | 设置移动端接入 Token（从 CLS 控制台获取） |

---

### 2.5 网络探测 API

所有探测方法均通过 `CLSNetworkDiagnosis.getInstance()` 调用，探测结果自动上报到 CLS。

#### HTTP Ping

检测目标 URL 的 HTTP 连通性、响应时间、证书等信息。

```java
public void clsHttpPing(Context context) throws NoSuchAlgorithmException {
    INetworkDiagnosis.HttpRequest request = new INetworkDiagnosis.HttpRequest();
    request.domain = "https://ap-guangzhou.cls.tencentcs.com"; // 必填：目标 URL
    request.ip = "1.2.3.4";                                    // 可选：指定目标 IP（绕过 DNS）
    request.headerOnly = true;                                  // 可选：仅获取响应头（默认 false）
    request.downloadBytesLimit = 1024;                          // 可选：限制下载字节数（默认 64KB）
    request.timeout = 30 * 1000;                                // 可选：超时时间，单位毫秒（默认 30000ms）
    request.maxTimes = 3;                                       // 可选：探测次数（默认 10）
    request.multiplePortsDetect = false;                        // 可选：是否多端口探测（默认 true）
    // 可选：SSL 证书校验配置
    request.credential = new INetworkDiagnosis.HttpCredential(getSSLContext(context), null);
    // 可选：自定义扩展字段
    request.extension = new HashMap<String, String>() {{
        put("custom_field", "httpPing");
    }};
    // 不带回调
    CLSNetworkDiagnosis.getInstance().http(request);
    // 带回调
    CLSNetworkDiagnosis.getInstance().http(request, new INetworkDiagnosis.Callback() {
        @Override
        public void onComplete(INetworkDiagnosis.Response response) {
            CLSLog.i("HTTP", response.content);
        }
    });
}
```

**HttpRequest 参数说明：**

> `HttpRequest` 继承自 `PingRequest`，包含以下所有字段：

| 字段 | 类型 | 默认值 | 是否必填 | 说明 |
|------|------|--------|----------|------|
| `domain` | `String` | — | 必填 | 目标 URL，需包含协议头（`http://` 或 `https://`） |
| `ip` | `String` | `null` | 可选 | 指定目标 IP，设置后跳过 DNS 解析直接连接 |
| `headerOnly` | `boolean` | `false` | 可选 | 是否只请求响应头 |
| `downloadBytesLimit` | `int` | `65536`（64KB） | 可选 | 限制下载字节数，0 表示不限制 |
| `credential` | `HttpCredential` | `null` | 可选 | SSL 证书校验配置，含 `SSLContext` 和 `X509TrustManager` |
| `timeout` | `int`（毫秒） | `30000`（30秒） | 可选 | 请求超时时间 |
| `maxTimes` | `int` | `10` | 可选 | 探测次数 |
| `size` | `int`（byte） | `64` | 可选 | 探测包大小 |
| `multiplePortsDetect` | `boolean` | `true` | 可选 | 是否启用多端口探测 |
| `extension` | `Map<String, String>` | `null` | 可选 | 自定义扩展字段，随结果一起上报 |

---

#### DNS 解析

检测目标域名的 DNS 解析耗时和结果。

```java
public void clsDNSPing() {
    INetworkDiagnosis.DnsRequest request = new INetworkDiagnosis.DnsRequest();
    request.domain = "ap-guangzhou-open.cls.tencentcs.com"; // 必填：目标域名
    request.type = INetworkDiagnosis.DNS_TYPE_IPv4;          // 可选：DNS 类型，"A"（IPv4）或 "AAAA"（IPv6），默认 "A"
    request.nameServer = "8.8.8.8";                          // 可选：指定 DNS 服务器地址
    request.timeout = 2000;                                  // 可选：超时时间，单位毫秒（默认 2000ms）
    request.extension = new HashMap<String, String>() {{
        put("custom_field", "dns");
    }};
    // 不带回调
    CLSNetworkDiagnosis.getInstance().dns(request);
    // 带回调
    CLSNetworkDiagnosis.getInstance().dns(request, new INetworkDiagnosis.Callback() {
        @Override
        public void onComplete(INetworkDiagnosis.Response response) {
            CLSLog.i("DNS", response.content);
        }
    });
}
```

**DnsRequest 参数说明：**

> `DnsRequest` 继承自 `PingRequest`，包含以下所有字段：

| 字段 | 类型 | 默认值 | 是否必填 | 说明 |
|------|------|--------|----------|------|
| `domain` | `String` | — | 必填 | 目标域名 |
| `type` | `String` | `"A"` | 可选 | DNS 查询类型：`"A"`（IPv4）或 `"AAAA"`（IPv6） |
| `nameServer` | `String` | `null` | 可选 | 指定 DNS 服务器地址，为空则使用系统默认 DNS |
| `timeout` | `int`（毫秒） | `2000` | 可选 | 查询超时时间 |
| `size` | `int`（byte） | `64` | 可选 | 探测包大小 |
| `multiplePortsDetect` | `boolean` | `true` | 可选 | 是否启用多端口探测 |
| `extension` | `Map<String, String>` | `null` | 可选 | 自定义扩展字段 |

---

#### Ping（ICMP）

检测目标域名的 ICMP 连通性和延迟。

```java
public void clsPing() {
    INetworkDiagnosis.PingRequest request = new INetworkDiagnosis.PingRequest();
    request.domain = "ap-guangzhou-open.cls.tencentcs.com"; // 必填：目标域名
    request.size = 64;                                      // 可选：探测包大小，单位 byte（默认 64）
    request.maxTimes = 10;                                  // 可选：探测次数（默认 10）
    request.timeout = 2000;                                 // 可选：超时时间，单位毫秒（默认 2000ms）
    request.multiplePortsDetect = true;                     // 可选：是否多端口探测（默认 true）
    request.extension = new HashMap<String, String>() {{
        put("custom_field", "ping");
    }};
    // 不带回调
    CLSNetworkDiagnosis.getInstance().ping(request);
    // 带回调
    CLSNetworkDiagnosis.getInstance().ping(request, new INetworkDiagnosis.Callback() {
        @Override
        public void onComplete(INetworkDiagnosis.Response response) {
            CLSLog.i("Ping", response.content);
        }
    });
}
```

**PingRequest 参数说明：**

| 字段 | 类型 | 默认值 | 是否必填 | 说明 |
|------|------|--------|----------|------|
| `domain` | `String` | — | 必填 | 目标域名或 IP |
| `size` | `int`（byte） | `64` | 可选 | ICMP 探测包大小 |
| `maxTimes` | `int` | `10` | 可选 | 探测次数 |
| `timeout` | `int`（毫秒） | `2000` | 可选 | 单次探测超时时间 |
| `multiplePortsDetect` | `boolean` | `true` | 可选 | 是否启用多端口探测 |
| `extension` | `Map<String, String>` | `null` | 可选 | 自定义扩展字段 |

---

#### MTR（路由追踪）

追踪到目标域名的完整网络路径，支持 ICMP 和 UDP 协议，支持结果回调。

```java
public void clsMTR() {
    INetworkDiagnosis.MtrRequest request = new INetworkDiagnosis.MtrRequest();
    request.domain = "ap-guangzhou-open.cls.tencentcs.com";        // 必填：目标域名
    request.protocol = INetworkDiagnosis.MtrRequest.Protocol.ICMP; // 可选：探测协议（默认 ALL）
    request.maxTTL = 30;                                            // 可选：最大跳数（默认 30）
    request.maxPaths = 1;                                           // 可选：最大路径数（默认 1）
    request.maxTimes = 10;                                          // 可选：每跳探测次数（默认 10）
    request.timeout = 2000;                                         // 可选：超时时间，单位毫秒（默认 2000ms）
    request.multiplePortsDetect = true;                             // 可选：是否多端口探测（默认 true）
    request.extension = new HashMap<String, String>() {{
        put("custom_field", "mtr");
    }};
    // 不带回调
    CLSNetworkDiagnosis.getInstance().mtr(request);
    // 带回调
    CLSNetworkDiagnosis.getInstance().mtr(request, new INetworkDiagnosis.Callback() {
        @Override
        public void onComplete(INetworkDiagnosis.Response response) {
            CLSLog.i("MTR", response.content);
        }
    });
}
```

**MtrRequest 参数说明：**

> `MtrRequest` 继承自 `PingRequest`，包含以下所有字段：

| 字段 | 类型 | 默认值 | 是否必填 | 说明 |
|------|------|--------|----------|------|
| `domain` | `String` | — | 必填 | 目标域名或 IP |
| `protocol` | `Protocol` | `Protocol.ALL` | 可选 | 探测协议：`ALL`（全部）、`ICMP`、`UDP` |
| `maxTTL` | `int` | `30` | 可选 | 最大路由跳数（TTL） |
| `maxPaths` | `int` | `1` | 可选 | 最大探测路径数 |
| `maxTimes` | `int` | `10` | 可选 | 每跳探测次数 |
| `timeout` | `int`（毫秒） | `2000` | 可选 | 单次探测超时时间 |
| `size` | `int`（byte） | `64` | 可选 | 探测包大小 |
| `multiplePortsDetect` | `boolean` | `true` | 可选 | 是否启用多端口探测 |
| `extension` | `Map<String, String>` | `null` | 可选 | 自定义扩展字段 |

**Callback 回调说明：**

| 方法 | 说明 |
|------|------|
| `onComplete(Response response)` | 探测完成时回调，`response.content` 为探测结果 JSON 字符串，`response.error` 为错误信息 |

---

#### TCP Ping

检测目标域名指定端口的 TCP 连通性和延迟。

```java
public void clsTcpPing() {
    INetworkDiagnosis.TcpPingRequest request = new INetworkDiagnosis.TcpPingRequest();
    request.domain = "ap-guangzhou-open.cls.tencentcs.com"; // 必填：目标域名
    request.port = 80;                                      // 必填：目标端口
    request.payload = "hello";                              // 可选：发送的探测数据
    request.maxTimes = 10;                                  // 可选：探测次数（默认 10）
    request.timeout = 2000;                                 // 可选：超时时间，单位毫秒（默认 2000ms）
    request.size = 64;                                      // 可选：探测包大小，单位 byte（默认 64）
    request.multiplePortsDetect = true;                     // 可选：是否多端口探测（默认 true）
    request.extension = new HashMap<String, String>() {{
        put("custom_field", "tcpPing");
    }};
    // 不带回调
    CLSNetworkDiagnosis.getInstance().tcpPing(request);
    // 带回调
    CLSNetworkDiagnosis.getInstance().tcpPing(request, new INetworkDiagnosis.Callback() {
        @Override
        public void onComplete(INetworkDiagnosis.Response response) {
            CLSLog.i("TcpPing", response.content);
        }
    });
}
```

**TcpPingRequest 参数说明：**

> `TcpPingRequest` 继承自 `PingRequest`，包含以下所有字段：

| 字段 | 类型 | 默认值 | 是否必填 | 说明 |
|------|------|--------|----------|------|
| `domain` | `String` | — | 必填 | 目标域名或 IP |
| `port` | `int` | `-1` | 必填 | 目标端口号 |
| `payload` | `String` | `null` | 可选 | 探测时发送的数据内容 |
| `maxTimes` | `int` | `10` | 可选 | 探测次数 |
| `timeout` | `int`（毫秒） | `2000` | 可选 | 单次探测超时时间 |
| `size` | `int`（byte） | `64` | 可选 | 探测包大小 |
| `multiplePortsDetect` | `boolean` | `true` | 可选 | 是否启用多端口探测 |
| `extension` | `Map<String, String>` | `null` | 可选 | 自定义扩展字段 |
