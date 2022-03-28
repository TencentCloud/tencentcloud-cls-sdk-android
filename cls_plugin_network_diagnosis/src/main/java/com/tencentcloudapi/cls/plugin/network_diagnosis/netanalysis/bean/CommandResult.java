package com.tencentcloudapi.cls.plugin.network_diagnosis.netanalysis.bean;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class CommandResult implements JsonSerializable {
    protected CommandStatus status;

    public CommandStatus getStatus() {
        return status;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("status", status == null ? null : status.name());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}

interface JsonSerializable {
    JSONObject toJson();
}