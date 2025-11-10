package com.tencentcloudapi.cls.android.scheme;

import com.tencentcloudapi.cls.android.utils.TimeUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Event {
    private String name;
    private final List<Attribute> attributes = new LinkedList<>();
    private final Object lock = new Object();
    private long epochNanos;
    private int totalAttributeCount = 0;

    Event(String name) {
        this.name = name;
        this.epochNanos = TimeUtils.instance.now();
    }

    public static Event create(String name) {
        return new Event(name);
    }

    public Event addAttribute(Attribute... attributes) {
        if (null == attributes) {
            return this;
        } else {
            synchronized(this.lock) {
                this.attributes.addAll(Arrays.asList(attributes));
                this.totalAttributeCount += attributes.length;
                return this;
            }
        }
    }

    public Event addAttribute(List<Attribute> attributes) {
        if (null == attributes) {
            return this;
        } else {
            synchronized(this.lock) {
                this.attributes.addAll(attributes);
                this.totalAttributeCount += attributes.size();
                return this;
            }
        }
    }

    public String getName() {
        synchronized(this.lock) {
            return this.name;
        }
    }

    public Event setName(String name) {
        synchronized(this.lock) {
            this.name = name;
            return this;
        }
    }

    public List<Attribute> getAttributes() {
        synchronized(this.lock) {
            return this.attributes;
        }
    }

    public long getEpochNanos() {
        synchronized(this.lock) {
            return this.epochNanos;
        }
    }

    public Event setEpochNanos(long epochNanos) {
        synchronized(this.lock) {
            this.epochNanos = epochNanos;
            return this;
        }
    }

    public int getTotalAttributeCount() {
        synchronized(this.lock) {
            return this.totalAttributeCount;
        }
    }

    public Event setTotalAttributeCount(int totalAttributeCount) {
        synchronized(this.lock) {
            this.totalAttributeCount = totalAttributeCount;
            return this;
        }
    }
}
