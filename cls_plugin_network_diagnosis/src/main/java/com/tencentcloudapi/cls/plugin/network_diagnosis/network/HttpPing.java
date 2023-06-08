package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public final class HttpPing implements Task {
    private volatile boolean stopped;
    private static final int MAX = 1024 * 1024;

    private final String url;
    private final CLSNetDiagnosis.Output output;
    private final CLSNetDiagnosis.Callback complete;

    public HttpPing(String url, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        this.url = url;
        this.output = output;
        this.complete = complete;
        this.stopped = false;
    }

    public static Task start(String url, CLSNetDiagnosis.Output out, CLSNetDiagnosis.Callback complete) {
        final HttpPing h = new HttpPing(url, out, complete);
        Utils.runInBack(new Runnable() {
            @Override
            public void run() {
                h.run();
            }
        });
        return h;
    }


    private void run() {
        long start = System.currentTimeMillis();
        try {
            output.write("Get " + url);
            URL u = new URL(url);
            HttpURLConnection httpConn = (HttpURLConnection) u.openConnection();
            httpConn.setConnectTimeout(10000);
            httpConn.setReadTimeout(20000);
            int responseCode = httpConn.getResponseCode();
            output.write("status " + responseCode);


            Map<String, List<String>> headers = httpConn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                output.write(entry.getKey() + ":" + entry.getValue().get(0));
            }
            InputStream is = httpConn.getInputStream();
            int len = httpConn.getContentLength();
            len = len > MAX || len < 0 ? MAX : len;
            byte[] data = new byte[len];
            int read = is.read(data);
            long duration = System.currentTimeMillis() - start;
            output.write("Done, duration " + duration + "ms");
            is.close();
            if (read <= 0) {
                Result r = new Result(url,responseCode, headers, null, (int) duration, "", httpConn.getContentLength());
                complete.onComplete(r.encode());
                return;
            }
            if (read < data.length) {
                byte[] b = new byte[read];
                System.arraycopy(data, 0, b, 0, read);
                Result r = new Result(url, responseCode, headers, b, (int) duration, "", httpConn.getContentLength());
                complete.onComplete(r.encode());
            }
        } catch (IOException e) {
            e.printStackTrace();
            long duration = System.currentTimeMillis() - start;
            Result r = new Result(url,-1, null, null, (int) duration, e.getMessage(), 0);
            output.write("error : " + e.getMessage());
            complete.onComplete(r.encode());
        }
    }

    @Override
    public void stop() {
        {
            stopped = true;
        }
    }

    public static class Result {
        public final int code;
        public final Map<String, List<String>> headers;
        public final byte[] body;
        public final int duration;
        public final String errorMessage;
        private final String url;
        public final int contentLength;

        private Result(String url, int code,
                       Map<String, List<String>> headers, byte[] body, int duration, String errorMessage, int contentLength) {
            this.code = code;
            this.headers = headers;
            this.body = body;
            this.duration = duration;
            this.errorMessage = errorMessage;
            this.url = url;
            this.contentLength = contentLength;
        }

        public String encode() {
            JSONObject o = new JSONObject();
            try {
                o.put("method", "http");
                o.put("url", this.url);
                o.put("requestTime", this.duration);
                o.put("httpCode", this.code);
                o.put("contentLength", this.contentLength);
                o.put("errorMessage", this.errorMessage);
                JSONObject newHeaders = new JSONObject();
                if (null != this.headers) {
                    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                        if (null==entry.getKey() || entry.getKey().isEmpty()) {
                            newHeaders.put("-" ,entry.getValue().get(0));
                        } else {
                            newHeaders.put(entry.getKey() ,entry.getValue().get(0));
                        }
                    }
                    o.put("headers", newHeaders);
                } else {
                    o.put("headers", "");
                }
                if (null == this.body) {
                    o.put("body", "");
                } else {
                    o.put("body", new String(this.body));
                }
                return o.toString();
            } catch (JSONException err) {
                err.printStackTrace();
                return null;
            }
        }
    }
}
