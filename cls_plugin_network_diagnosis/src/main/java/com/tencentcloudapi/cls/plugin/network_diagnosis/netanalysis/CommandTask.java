package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.plugin.network_diagnosis.network.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

public abstract class CommandTask<T> {
    protected final String TAG = getClass().getSimpleName();

    protected String command;
    protected Process process;
    protected boolean isRunning = false;

    protected InputStream dataInputStream = null;
    protected InputStream errorInputStream = null;
    protected static float COMMAND_ELAPSED_TIME = 0.f;

    protected T resultData;

    protected Process createProcess(String cmd) throws IOException {
        return Runtime.getRuntime().exec(cmd);
    }

    protected String execCommand(String command) throws InterruptedException, IOException {
        CLSLog.d(TAG, "[command]:" + command);
        process = createProcess(command);
        int status = process.waitFor();
        CLSLog.d(TAG, "[status]: " + status);

        BufferedInputStream dataBufferedStream;
        BufferedInputStream errorBufferedStream;

        dataInputStream = process.getInputStream();
        errorInputStream = process.getErrorStream();
        dataBufferedStream = new BufferedInputStream(dataInputStream);
        errorBufferedStream = new BufferedInputStream(errorInputStream);
        String dataStr = "";
        String errorStr = "";

        try {
            dataStr = readData(dataBufferedStream);
            errorStr = readData(errorBufferedStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Utils.closeAllCloseable(dataBufferedStream, dataInputStream, errorBufferedStream, errorInputStream);
            process.destroy();
            parseErrorInfo(errorStr);
        }

        return dataStr;
    }

    protected abstract T run();

    protected abstract void stop();

    protected abstract void parseInputInfo(String input);

    protected abstract void parseErrorInfo(String error);

    protected String readData(InputStream input) throws IOException {
        if (input == null)
            return null;

        int len;
        byte[] buffer = new byte[1024];
        byte[] cache = null;
        while ((len = input.read(buffer)) > 0) {
            if (cache == null) {
                cache = Arrays.copyOf(buffer, len);
            } else {
                int cacheLen = cache.length;
                byte[] tmp = new byte[cacheLen + len];
                System.arraycopy(cache, 0, tmp, 0, cacheLen);
                System.arraycopy(buffer, 0, tmp, cacheLen, len);
                cache = tmp;
            }
        }

        if (cache == null)
            return null;

        return new String(cache, Charset.forName("UTF-8"));
    }

    public T getResultData() {
        return resultData;
    }
}
