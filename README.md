# 腾讯CLS Android端日志采集SDK使用说明
---

## 集成方法

android中引用的包一般分为两种：jar包、arr包。 本项目目前提供arr包的接入方式： 

- 在build.gradle文件中dependencies标签中添加下面的依赖。
```
dependencies {
    implementation("com.google.protobuf:protobuf-java:3.13.0")
    implementation("com.google.protobuf:protobuf-java-util:3.13.0")
    compile(group: 'com.tencent.cls', name: 'tencent_cls_producer', version: '1.0.0.3', ext: 'aar')
}
```

- 因为cls日志上传是基于http的，android 9.0使用HttpUrlConnection进行http请求会出现以下异常

```
 W/System.err: java.io.IOException: Cleartext HTTP traffic to **** not permitted
```

解决办法：

1.在res文件夹下创建一个xml文件夹，然后创建一个network_security_config.xml文件，文件内容如下：
    (ap-guangzhou.cls.tencentcs.com 按需要制定)

```
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!--<base-config cleartextTrafficPermitted="true" />-->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">ap-guangzhou.cls.tencentcs.com</domain>
    </domain-config>
</network-security-config>
```

2、接着，在AndroidManifest.xml文件下的application标签增加以下属性

```
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    ...
/>
```



## CLS Android SDK接入demo示例

```
public static void main(String[] args) throws LogException, ExecutionException, InterruptedException {
        String endpoint = "ap-guangzhou.cls.tencentcs.com";
        // API密钥 secretId，必填
        String secretId = "*";
        // API密钥 secretKey，必填
        String secretKey = "*";
        // 日志主题ID，必填
        String topicId = "*";
        // 日志源机器IP，可选
        String source = "test_source";
        // 日志源文件名，可选
        String filename = "test_filename";

        // 构建一个客户端实例
        final AsyncClient client = new AsyncClient(endpoint, secretId, secretKey, NetworkUtils.getLocalMachineIP(), 5, 10);


        int ts = (int) (System.currentTimeMillis() / 1000);
        LogItem logItem = new LogItem(ts);
        logItem.PushBack(new LogContent("__CONTENT__", "你好，我来自深圳|hello world"));
        logItem.PushBack(new LogContent("city", "guangzhou"));
        logItem.PushBack(new LogContent("logNo",
                String.valueOf(System.currentTimeMillis() + new Random(1000).nextInt())));
        logItem.PushBack(new LogContent("__PKG_LOGID__", (String.valueOf(System.currentTimeMillis()))));
        Logs.LogGroup.Builder logGroup = Logs.LogGroup.newBuilder();
        logGroup.addLogs(logItem.mContents);

        final PutLogsRequest req = new PutLogsRequest(topicId, source, filename, logGroup);
        Future<PutLogsResponse> resq = client.PutLogs(req);
        // resq.get() 是阻塞的
        System.out.println(resq.get().GetAllHeaders());
        client.executor.shutdown();
}
```


