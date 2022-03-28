package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.net;

import com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.bean.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class NetCommandResult extends CommandResult {
    protected String targetIp;

    protected NetCommandResult(String targetIp) {
        this.targetIp = targetIp;
    }

    public String getTargetIp() {
        return targetIp;
    }

    protected NetCommandResult setTargetIp(String targetIp) {
        this.targetIp = targetIp;
        return this;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        try {
            json.put("targetIp", targetIp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
