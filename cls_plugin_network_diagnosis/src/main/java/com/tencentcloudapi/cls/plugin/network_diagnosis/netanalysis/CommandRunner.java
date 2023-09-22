package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis;

//import androidx.annotation.NonNull;

import com.tencentcloudapi.cls.android.CLSLog;

import java.util.concurrent.LinkedBlockingQueue;

public class CommandRunner extends Thread {
    private final String TAG = getClass().getSimpleName();

    private LinkedBlockingQueue<CommandPerformer> cmdQueue;
    private boolean isRunning;
    private CommandPerformer currentPerformer;

    public CommandRunner() {
        this("CLS-netsdk-threadpool");
    }

    public CommandRunner(String name) {
        super(name);
        this.cmdQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            try {
                currentPerformer = cmdQueue.take();
                if (isRunning)
                    currentPerformer.run();
            } catch (InterruptedException e) {
                CLSLog.d(TAG, "[cmd runner interrupt]:" + e == null ? "" : e.getMessage());
            }
        }
    }

    public boolean addCommand(CommandPerformer performer) {
        if (cmdQueue == null)
            cmdQueue = new LinkedBlockingQueue<>();

        return cmdQueue.offer(performer);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void cancel() {
        cmdQueue.clear();

        if (currentPerformer != null) {
            currentPerformer.stop();
            currentPerformer = null;
        }
    }

    public void shutdownNow() {
        if (!isRunning)
            return;

        cancel();
        isRunning = false;
        interrupt();
    }
}
