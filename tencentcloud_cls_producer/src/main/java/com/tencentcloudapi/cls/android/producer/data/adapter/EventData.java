package com.tencentcloudapi.cls.android.producer.data.adapter;

import com.tencentcloudapi.cls.android.producer.common.Logs;

import org.json.JSONArray;

import java.nio.charset.StandardCharsets;

public class EventData {
    private final JSONArray eventIds;
    private final Logs.LogGroup.Builder logs;

    public EventData(JSONArray eventIds, Logs.LogGroup.Builder logs) {
        this.eventIds = eventIds;
        this.logs = logs;
    }
    public JSONArray getEventIds() {
        return eventIds;
    }
    public byte[] getByteEventIds() {
        if (eventIds.length() > 0) {
            return eventIds.toString().getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }
    public void setEventId(String eventId) {
        eventIds.put(eventId);
    }

    public Logs.LogGroup.Builder getLogs() {
        return logs;
    }

    public void setLogs(Logs.Log.Builder log) {
        logs.addLogs(log);
    }

    public void setEventData(String eventId, Logs.Log.Builder log){
        setEventId(eventId);
        setLogs(log);
    }


}
