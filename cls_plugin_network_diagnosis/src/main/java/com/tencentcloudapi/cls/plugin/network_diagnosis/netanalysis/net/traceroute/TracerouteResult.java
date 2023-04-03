package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net.traceroute;

import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.bean.CommandStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TracerouteResult implements JsonSerializable {
    private String targetIp;
    private List<TracerouteNodeResult> tracerouteNodeResults;
    private long timestamp;
    private CommandStatus status;
    private String host;

    public TracerouteResult(String targetIp, long timestamp, CommandStatus status, String host) {
        this.targetIp = targetIp;
        this.timestamp = timestamp;
        this.status = status;
        this.host = host;
        tracerouteNodeResults = new ArrayList<>();
    }

    public String getTargetIp() {
        return targetIp;
    }

    public List<TracerouteNodeResult> getTracerouteNodeResults() {
        return tracerouteNodeResults;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public CommandStatus getCommandStatus() {
        return status;
    }


    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JSONArray jarr = new JSONArray();
        if (tracerouteNodeResults != null && !tracerouteNodeResults.isEmpty()) {
            for (TracerouteNodeResult result : tracerouteNodeResults) {
                if (result == null || result.toJson().length() == 0)
                    continue;

                jarr.put(result.toJson());
            }
        }
        try {
            json.put("host", host);
            json.put("host_ip", targetIp);
            json.put("timestamp", timestamp);
            json.put("command_status", status.getName().toString());
            json.put("traceroute_node_results", jarr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}

interface JsonSerializable {
    JSONObject toJson();
}
