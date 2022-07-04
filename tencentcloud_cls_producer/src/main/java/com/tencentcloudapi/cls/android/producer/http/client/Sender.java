package com.tencentcloudapi.cls.android.producer.http.client;

import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.http.comm.RequestMessage;
import com.tencentcloudapi.cls.android.producer.http.utils.HttpUtil;
import com.tencentcloudapi.cls.android.producer.request.SearchLogRequest;
import com.tencentcloudapi.cls.android.producer.response.PutLogsResponse;
import com.tencentcloudapi.cls.android.producer.response.SearchLogResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author farmerx
 */
public class Sender {
    public static SearchLogResponse doGet(SearchLogRequest request, String httpUrl, Map<String, String> headers) throws Exception {
        String result = null;
        HttpURLConnection httpURLConnection = null;
        InputStream is = null;
        BufferedReader br = null;

        Set<String> keySet = request.GetAllParams().keySet();
        Iterator iterator = keySet.iterator();
        StringBuffer stringBuffer = new StringBuffer();
        while (iterator.hasNext()){
            String key = (String)iterator.next();
            Object value = (Object)request.GetAllParams().get(key);
            // 1.普通get接口拼接参数方式: url?param1=value1&param2=value2...
            if(stringBuffer.toString().equals("")){
                stringBuffer.append("?");
            } else {
                stringBuffer.append("&");
            }
            stringBuffer.append(key);
            stringBuffer.append("=");
            stringBuffer.append(value);
        }
        httpUrl += stringBuffer.toString();

        URL url = new URL(httpUrl);
        httpURLConnection = (HttpURLConnection)url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        // connection.setRequestProperty("connection", "Keep-Alive");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        httpURLConnection.connect();

        if(httpURLConnection.getResponseCode()==200){
            is = httpURLConnection.getInputStream();
            br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            StringBuffer resBuffer = new StringBuffer();
            String line = null;
            while((line = br.readLine())!=null){
                //将每次读取的行进行保存
                resBuffer.append(line);
                resBuffer.append("\r\n");
            }
            result = resBuffer.toString();
        }

        if(br!=null){
            br.close();
        }
        if(is!=null){
            is.close();
        }

        SearchLogResponse resp = new SearchLogResponse(httpURLConnection.getHeaderFields());
        resp.SetHttpStatusCode(httpURLConnection.getResponseCode());
        resp.SetResult(result);
        httpURLConnection.disconnect();
        return resp;
    }


    public static PutLogsResponse doPost(RequestMessage requestMessage) throws Exception {
        PutLogsResponse resp;
        HttpURLConnection connection;
        OutputStream outputStream;
        URL url = new URL(buildUri(requestMessage));
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(requestMessage.getMethod().toString());
        connection.setConnectTimeout(Constants.HTTP_CONNECT_TIME_OUT);
        connection.setReadTimeout(Constants.HTTP_SEND_TIME_OUT);
        connection.setDoOutput(true);
        connection.setDoInput(true);

        // connection.setRequestProperty("connection", "Keep-Alive");
        for (Map.Entry<String, String> entry : requestMessage.getHeaders().entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        outputStream = connection.getOutputStream();
        outputStream.write(requestMessage.getContent());

        resp = new PutLogsResponse(connection.getHeaderFields());
        resp.SetHttpStatusCode(connection.getResponseCode());

        outputStream.close();
        return resp;
    }

    /**
     * 构造uri
     * @param requestMessage message
     * @return String
     */
    private static String buildUri(RequestMessage requestMessage) {
        final String delimiter = "/";
        String uri = requestMessage.getEndpoint().toString();
        if (! uri.endsWith(delimiter)
                && (requestMessage.getResourcePath() == null ||
                ! requestMessage.getResourcePath().startsWith(delimiter))){
            uri += delimiter;
        }

        if (requestMessage.getResourcePath() != null){
            uri += requestMessage.getResourcePath();
        }

        String paramString = HttpUtil.paramToQueryString(requestMessage.getParameters(), Constants.UTF_8_ENCODING);
        if (paramString != null ) {
            uri += "?" + paramString;
        }
        return uri;
    }



}

