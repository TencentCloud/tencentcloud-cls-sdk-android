package com.tencentcloudapi.cls.android.scheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Link {
    private String traceId;
    private String spanId;
    private final List<Attribute> attributes = new ArrayList<>();
    private final Object lock = new Object();

    private Link(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    public static Link create(String traceId, String spanId) {
        return new Link(traceId, spanId);
    }

    public Link addAttribute(Attribute... attributes) {
        if (null == attributes) {
            return this;
        } else {
            this.addAttribute(Arrays.asList(attributes));
            return this;
        }
    }

    public Link addAttribute(List<Attribute> attributes) {
        if (null == attributes) {
            return this;
        } else {
            synchronized(this.lock) {
                this.attributes.addAll(attributes);
                return this;
            }
        }
    }

    public List<Attribute> getAttributes() {
        synchronized(this.lock) {
            return this.attributes;
        }
    }

    public String getTraceId() {
        synchronized(this.lock) {
            return this.traceId;
        }
    }

    public void setTraceId(String traceId) {
        synchronized(this.lock) {
            this.traceId = traceId;
        }
    }

    public String getSpanId() {
        synchronized(this.lock) {
            return this.spanId;
        }
    }

    public void setSpanId(String spanId) {
        synchronized(this.lock) {
            this.spanId = spanId;
        }
    }
}
