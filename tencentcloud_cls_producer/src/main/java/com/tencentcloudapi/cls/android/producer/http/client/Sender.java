package com.tencentcloudapi.cls.android.producer.http.client;

import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.http.comm.RequestMessage;
import com.tencentcloudapi.cls.android.producer.http.utils.HttpUtil;
import com.tencentcloudapi.cls.android.producer.response.PutLogsResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author farmerx
 */
public class Sender {
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

