package com.tencentcloudapi.cls.android.producer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.android.ClsConfigOptions;
import com.tencentcloudapi.cls.android.exceptions.ConnectErrorException;
import com.tencentcloudapi.cls.android.exceptions.InvalidDataException;
import com.tencentcloudapi.cls.android.exceptions.ResponseErrorException;
import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.common.LogException;
import com.tencentcloudapi.cls.android.producer.common.Logs;
import com.tencentcloudapi.cls.android.producer.data.adapter.DbAdapter;
import com.tencentcloudapi.cls.android.producer.data.adapter.DbParams;
import com.tencentcloudapi.cls.android.producer.http.comm.HttpMethod;
import com.tencentcloudapi.cls.android.producer.util.LZ4Encoder;
import com.tencentcloudapi.cls.android.producer.util.NetworkUtils;
import com.tencentcloudapi.cls.android.producer.util.QcloudClsSignature;
import com.tencentcloudapi.cls.android.producer.util.Utils;

import org.json.JSONArray;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class EventMessages {
    private static final String TAG = "CLS.EventMessages";

    private static final AtomicLong BATCH_ID = new AtomicLong(0);

    private final String producerHash;
    private static final int FLUSH_QUEUE = 3;
    private static final int DELETE_ALL = 4;
    private static final int FLUSH_SCHEDULE = 5;
    private static final int FLUSH_INSTANT_EVENT = 7;
    private final Worker mWorker;
    private final Context mContext;
    private final DbAdapter mDbAdapter;

    private static final Map<Context, EventMessages> S_INSTANCES = new HashMap<>();

    private final ClsConfigOptions mClsConfigOptions;

    /**
     * 是否停止
     */
    private boolean isStop = false;

    public void stop() {
        isStop = true;
    }
    public boolean isStopped() {
        return isStop;
    }

    /**
     * 不要直接调用，通过 getInstance 方法获取实例
     */
    private EventMessages(final Context context, ClsConfigOptions clsConfigOptions) {
        mContext = context;
        mDbAdapter = DbAdapter.getInstance(context);
        mWorker = new Worker();
        mClsConfigOptions = clsConfigOptions;
        producerHash = Utils.generateProducerHash(0);
    }

    /**
     * 获取 AnalyticsMessages 对象
     *
     * @param messageContext Context
     */
    public static EventMessages getInstance(final Context messageContext, ClsConfigOptions clsConfigOptions) {
        synchronized (S_INSTANCES) {
            final Context appContext = messageContext.getApplicationContext();
            final EventMessages ret;
            if (!S_INSTANCES.containsKey(appContext)) {
                ret = new EventMessages(appContext, clsConfigOptions);
                S_INSTANCES.put(appContext, ret);
            } else {
                ret = S_INSTANCES.get(appContext);
            }
            return ret;
        }
    }

    public static EventMessages getInstance(Context context) {
        return S_INSTANCES.get(context);
    }

    public int addLogEvent(String event) {
        try {
            return mDbAdapter.addLogEvent(event);
        } catch (Exception e) {
            CLSLog.i(TAG, "add log event error:" + e);
        }
        return -1;
    }


    public void flushEventMessage(boolean isSendImmediately) {
        try {
            synchronized (mDbAdapter) {
                final Message m = Message.obtain();
                m.what = FLUSH_QUEUE;
                if (isSendImmediately) {
                    mWorker.runMessage(m);
                } else {
                    mWorker.runMessageOnce(m, mClsConfigOptions.getFlushInterval());
                }
            }
        } catch (Exception e) {
            CLSLog.i(TAG, "enqueueEventMessage error:" + e);
        }
    }

    public void flushInstanceEvent() {
        try {
            final Message m = Message.obtain();
            m.what = FLUSH_INSTANT_EVENT;

            mWorker.runMessage(m);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    public void flush() {
        try {
            final Message m = Message.obtain();
            m.what = FLUSH_QUEUE;
            mWorker.runMessage(m);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    public void flushScheduled() {
        try {
            if (isStop) {
                CLSLog.i(TAG, "EventMessage stop flushScheduled");
                return;
            }
            final Message m = Message.obtain();
            m.what = FLUSH_SCHEDULE;
            mWorker.runMessageOnce(m, mClsConfigOptions.getFlushInterval());
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    public void deleteAll() {
        try {
            final Message m = Message.obtain();
            m.what = DELETE_ALL;

            mWorker.runMessage(m);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
        }
    }

    private boolean sendCheck() {
        try {
            //无网络
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                return false;
            }
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            return false;
        }
        return true;
    }

    private void sendData(boolean is_instant_event) {
        if (!sendCheck()) {
            return;
        }
        int count = 100;
        while (count > 0) {
            boolean deleteEvents = true;
            byte[][] eventsData;
            synchronized (mDbAdapter) {
                eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, mClsConfigOptions.getFlushBulkSize(), is_instant_event);
            }
            if (eventsData == null) {
                return;
            }
            final String eventIds = new String(eventsData[0], StandardCharsets.UTF_8);
            final byte[] rawMessage = eventsData[1];
            String errorMessage = null;
            try {
                Logs.LogGroup.Builder logGroupBuilder = Logs.LogGroup.newBuilder();
                Logs.LogGroupList.Builder grpList = Logs.LogGroupList.newBuilder();
                byte[] compressedData;
                try {
                    logGroupBuilder.mergeFrom(rawMessage);
                    logGroupBuilder.setContextFlow(Utils.generatePackageId(producerHash, BATCH_ID));
                    // 增加tag
                    for (Map.Entry<String, String> entry : mClsConfigOptions.getTag().entrySet()) {
                        logGroupBuilder.addLogTags(Logs.LogTag.newBuilder().setKey(entry.getKey()).setValue(entry.getValue()));
                    }
                    // 内容压缩
                    compressedData = LZ4Encoder.compressToLhLz4Chunk(grpList.addLogGroupList(logGroupBuilder).build().toByteArray());
                } catch (Exception e) {
                    throw new InvalidDataException(e.getMessage());
                }
                // 构造请求header 头
                HashMap<String, String> headParameter = new HashMap<>(3);
                headParameter.put(Constants.CONST_CONTENT_LENGTH, String.valueOf(compressedData.length));
                headParameter.put(Constants.CONST_CONTENT_TYPE, Constants.CONST_PROTO_BUF);
                headParameter.put(Constants.CONST_HOST, mClsConfigOptions.getHost());
                HashMap<String, String> urlParameter = new HashMap<>(1);
                urlParameter.put(Constants.TOPIC_ID, mClsConfigOptions.getTopicId());
                if (mClsConfigOptions.getCredential().getSecretId() != null && !mClsConfigOptions.getCredential().getSecretId().isEmpty()) {
                    // 构造签名
                    String signature = QcloudClsSignature.buildSignature(
                            mClsConfigOptions.getCredential().getSecretId(),
                            mClsConfigOptions.getCredential().getSecretKey(),
                            HttpMethod.POST.toString(),
                            Constants.UPLOAD_LOG_RESOURCE_URI,
                            urlParameter, headParameter,
                            300000);
                    headParameter.put(Constants.CONST_AUTHORIZATION, signature);
                }
                headParameter.put("x-cls-compress-type", "lz4");
                headParameter.put("x-cls-add-source", "1");
                if (!mClsConfigOptions.getCredential().getToken().isEmpty()) {
                    headParameter.put("X-Cls-Token", mClsConfigOptions.getCredential().getToken());
                }
                headParameter.put("User-Agent", "cls-android-sdk-2.0.0");
                // do send http reuqest
                sendHttpRequest(mClsConfigOptions.getEndpoint()+Constants.UPLOAD_LOG_RESOURCE_URI+"?topic_id="+mClsConfigOptions.getTopicId(), compressedData, headParameter);
            } catch (ConnectErrorException e) {
                deleteEvents = false;
                errorMessage = "Connection error: " + e.getMessage();
            } catch (InvalidDataException e) {
                errorMessage = "Invalid data: " + e.getMessage();
            } catch (ResponseErrorException e) {
                deleteEvents = isDeleteEventsByCode(e.getHttpCode());
                errorMessage = "ResponseErrorException: " + e.getMessage();
            } catch (Exception e) {
                deleteEvents = false;
                errorMessage = "Exception: " + e.getMessage();
            } finally {
                if (!TextUtils.isEmpty(errorMessage)) {
                    CLSLog.i(TAG, errorMessage);
                }
                if (deleteEvents) {
                    try {
                        count = mDbAdapter.cleanupEvents(new JSONArray(eventIds), is_instant_event);
                    } catch (Exception e) {
                        CLSLog.printStackTrace(e);
                    }
                    CLSLog.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                } else {
                    BATCH_ID.decrementAndGet();
                    count = 0;
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void sendHttpRequest(String path, byte[] body,  Map<String, String> headers) throws ConnectErrorException, ResponseErrorException {
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            final URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                CLSLog.i(TAG, String.format("can not connect %s, it shouldn't happen", url));
                return;
            }
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            //设置连接超时时间
            connection.setConnectTimeout(60 * 1000);
            //设置读取超时时间
            connection.setReadTimeout(60 * 1000);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            out = connection.getOutputStream();
            bout = new BufferedOutputStream(out);
            bout.write(body);
            bout.flush();

            int responseCode = connection.getResponseCode();
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = slurp(in);
            String requestID = connection.getHeaderField(Constants.CONST_X_SLS_REQUESTID);

            in.close();
            in = null;

            String response = new String(responseBody, StandardCharsets.UTF_8);
            CLSLog.i("SendHttpRequest", String.format("ret_code: %d, request_id: %s, ret_content: %s", responseCode, requestID, response));
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                // 校验错误
                throw new ResponseErrorException(String.format("flush failure with response '%s', the response code is '%d', request id is '%s'",
                        response, responseCode, requestID), responseCode);
            }
        } catch (IOException e) {
            throw new ConnectErrorException(e);
        } finally {
            closeStream(bout, out, in, connection);
        }
    }

    public static byte[] slurp(final InputStream inputStream)
            throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
    public static void closeStream(BufferedOutputStream bout, OutputStream out, InputStream in, HttpURLConnection connection) {
        if (null != bout) {
            try {
                bout.close();
            } catch (Exception e) {
                CLSLog.i(TAG, e.getMessage());
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                CLSLog.i(TAG, e.getMessage());
            }
        }

        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                CLSLog.i(TAG, e.getMessage());
            }
        }

        if (null != connection) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                CLSLog.i(TAG, e.getMessage());
            }
        }
    }

    /**
     * 在服务器正常返回状态码的情况下，目前只有 (>= 500 && < 600) || 429 || 408 || 403 才不删数据
     *
     * @param httpCode 状态码
     * @return true: 删除数据，false: 不删数据
     */
    private boolean isDeleteEventsByCode(int httpCode) {
        if (httpCode >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
            return false;
        }
        return httpCode != HttpURLConnection.HTTP_CLIENT_TIMEOUT && httpCode != 429 && httpCode != HttpURLConnection.HTTP_FORBIDDEN;
    }

    // Worker will manage the (at most single) IO thread associated with
    private class Worker {

        private final Object mHandlerLock = new Object();
        private final Handler mHandler;

        Worker() {
            final HandlerThread thread =
                    new HandlerThread("com.tencentcloudapi.cls.android.producer.AnalyticsMessages.Worker",
                            Thread.MIN_PRIORITY);
            thread.start();
            mHandler = new EventMessageHandler(thread.getLooper());
        }

        void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    CLSLog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        void runMessageOnce(Message msg, long delay) {
            synchronized (mHandlerLock) {
                // We died under suspicious circumstances. Don't try to send any more events.
                if (mHandler == null) {
                    CLSLog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    if (!mHandler.hasMessages(msg.what)) {
                        mHandler.sendMessageDelayed(msg, delay);
                    }
                }
            }
        }

        private class EventMessageHandler extends Handler {

            EventMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    if (msg.what == FLUSH_QUEUE) {
                        sendData(true);
                        sendData(false);
                    } else if (msg.what == DELETE_ALL) {
                        try {
                            mDbAdapter.deleteAllEvents();
                        } catch (Exception e) {
                            CLSLog.printStackTrace(e);
                        }
                    } else if (msg.what == FLUSH_SCHEDULE) {
                        flushScheduled();
                        sendData(false);
                    } else if (msg.what == FLUSH_INSTANT_EVENT) {
                        sendData(true);
                    } else {
                        CLSLog.i(TAG, "Unexpected message received by CLSData worker: " + msg);
                    }
                } catch (final RuntimeException e) {
                    CLSLog.i(TAG, e.getMessage());
                }
            }
        }
    }

}
