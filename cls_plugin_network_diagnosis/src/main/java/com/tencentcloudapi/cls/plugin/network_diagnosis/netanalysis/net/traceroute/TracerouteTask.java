package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net.traceroute;

import android.os.SystemClock;
import android.text.TextUtils;

//import androidx.annotation.NonNull;

import com.tencentcloudapi.cls.android.CLSLog;
import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.bean.CommandStatus;
import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net.NetCommandTask;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

final class TracerouteTask extends NetCommandTask<TracerouteNodeResult> {
    private InetAddress targetAddress;
    private int count;
    private int hop;

    private CLSNetDiagnosis.Output output;

    TracerouteTask(InetAddress targetAddress, int hop) {
        this(targetAddress, hop, 3, null);
    }

    TracerouteTask(InetAddress targetAddress, int hop, CLSNetDiagnosis.Output output) {
        this(targetAddress, hop, 3, output);
    }

    TracerouteTask(InetAddress targetAddress, int hop, int count, CLSNetDiagnosis.Output output) {
        this.targetAddress = targetAddress;
        this.hop = hop;
        this.count = count;
        this.output = output;
    }

    @Override
    protected TracerouteNodeResult run() {
        isRunning = true;

        String targetIp = targetAddress == null ? "" : targetAddress.getHostAddress();
        command = String.format("ping -c 1 -W 1 -t %d %s", hop, targetIp);

        int currentCount = 0;

        List<SingleNodeResult> singleNodeList = new ArrayList<>();
        while (isRunning && (currentCount < count)) {
            try {
                long startTime = SystemClock.elapsedRealtime();
                String cmdRes = execCommand(command);
                int delay = (int) (SystemClock.elapsedRealtime() - startTime);

                /**
                 * 耗时滤波规则：
                 * 1、临时性能耗时 = 计算的平均cmd性能耗时
                 * 2、若：此次的cmd执行耗时 - 计算的平均cmd性能耗时 < 此次耗时的 10%，并且 临时性能耗时 > 计算的平均cmd性能耗时的 10%
                 *   则：临时性能耗时 自减 20%
                 * 3、最终延迟 = 此次的cmd执行耗时 - 临时性能耗时
                 */
                float tmpElapsed = COMMAND_ELAPSED_TIME;
                while ((delay - tmpElapsed) < (delay * 0.1) && tmpElapsed > (COMMAND_ELAPSED_TIME * 0.1f))
                    tmpElapsed *= 0.8;

                CLSLog.d(TAG, String.format("[traceroute delay]:%d [COMMAND_ELAPSED_TIME]:%f [tmpElapsed]%f",
                        delay, COMMAND_ELAPSED_TIME, tmpElapsed));
                delay -= tmpElapsed;

                SingleNodeResult nodeResult = parseSingleNodeInfoInput(cmdRes);
                if (!nodeResult.isFinalRoute()
                        && nodeResult.getStatus() == CommandStatus.CMD_STATUS_SUCCESSFUL)
                    nodeResult.setDelay(delay);

                singleNodeList.add(nodeResult);
            } catch (IOException | InterruptedException e) {
                CLSLog.d(TAG, String.format("traceroute[%d]: %s occur error: %s", currentCount, command, e.getMessage()));
            } finally {
                currentCount++;
            }
        }

        resultData = new TracerouteNodeResult(targetAddress.getHostAddress(), hop, singleNodeList);
        if (output != null) {
            output.write(resultData.toString());
        }
        return isRunning ? resultData : null;
    }

    protected SingleNodeResult parseSingleNodeInfoInput(String input) {
        CLSLog.d(TAG, "[hop]:" + hop + " [org data]:" + input);
        SingleNodeResult nodeResult = new SingleNodeResult(targetAddress.getHostAddress(), hop);

        if (TextUtils.isEmpty(input)) {
            nodeResult.setStatus(CommandStatus.CMD_STATUS_NETWORK_ERROR);
            nodeResult.setDelay(0.f);
            return nodeResult;
        }

        Matcher matcherRouteNode = matcherRouteNode(input);

        if (matcherRouteNode.find()) {
            nodeResult.setRouteIp(getIpFromMatcher(matcherRouteNode));
            nodeResult.setStatus(CommandStatus.CMD_STATUS_SUCCESSFUL);
        } else {
            Matcher matcherTargetId = matcherIp(input);
            if (matcherTargetId.find()) {
                nodeResult.setRouteIp(matcherTargetId.group());
                nodeResult.setStatus(CommandStatus.CMD_STATUS_SUCCESSFUL);
                String time = getPingDelayFromMatcher(matcherTime(input));
                nodeResult.setDelay(Float.parseFloat(time));
            } else {
                nodeResult.setStatus(CommandStatus.CMD_STATUS_FAILED);
                nodeResult.setDelay(0.f);
            }
        }

        return nodeResult;
    }

    @Override
    protected void parseInputInfo(String input) {

    }

    @Override
    protected void parseErrorInfo(String error) {
        if (!TextUtils.isEmpty(error)) {
            CLSLog.d(TAG, "[hop]:" + hop + " [error data]:" + error);
        }
    }

    @Override
    protected void stop() {
        isRunning = false;
    }
}