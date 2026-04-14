package com.tencentcloudapi.cls.android.scheme;

import android.text.TextUtils;
import android.util.Pair;

import com.tencentcloudapi.cls.android.producer.common.LogItem;
import com.tencentcloudapi.cls.android.utils.JSONUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Span {
    protected String name;
    protected SpanKind kind;
    protected String traceID;
    protected String spanID;
    protected String parentSpanID;
    protected Long start;
    protected Long end;
    protected long duration;
    protected List<Attribute> attribute;
    protected List<Event> events;
    protected List<Link> links;
    protected StatusCode statusCode;
    protected String statusMessage;
    protected String host;
    protected Resource resource;
    protected String service;
    protected String sessionId;
    protected String transactionId;
    private final AtomicBoolean finished;
    protected final Object lock;

    protected Span() {
        this.kind = SpanKind.CLIENT;
        this.statusCode = Span.StatusCode.UNSET;
        this.finished = new AtomicBoolean();
        this.lock = new Object();
        this.attribute = new ArrayList<>();
        this.events = new ArrayList<>();
        this.resource = new Resource();
        this.links = new ArrayList<>();
    }

    public String getName() {
        synchronized(this.lock) {
            return this.name;
        }
    }

    public SpanKind getKind() {
        synchronized(this.lock) {
            return this.kind;
        }
    }

    public String getTraceId() {
        synchronized(this.lock) {
            return this.traceID;
        }
    }

    public String getSpanId() {
        synchronized(this.lock) {
            return this.spanID;
        }
    }

    public String getParentSpanId() {
        synchronized(this.lock) {
            return this.parentSpanID;
        }
    }

    public long getStart() {
        synchronized(this.lock) {
            return this.start;
        }
    }

    public long getEnd() {
        synchronized(this.lock) {
            return this.end;
        }
    }

    public long getDuration() {
        synchronized(this.lock) {
            return this.duration;
        }
    }

    public StatusCode getStatusCode() {
        synchronized(this.lock) {
            return this.statusCode;
        }
    }

    public String getStatusMessage() {
        synchronized(this.lock) {
            return this.statusMessage;
        }
    }

    public String getHost() {
        synchronized(this.lock) {
            return this.host;
        }
    }

    public String getService() {
        synchronized(this.lock) {
            return this.service;
        }
    }

    public Span setName(String name) {
        synchronized(this.lock) {
            this.name = name;
            return this;
        }
    }

    public Span setKind(SpanKind kind) {
        synchronized(this.lock) {
            this.kind = kind;
            return this;
        }
    }

    public Span setTraceId(String traceID) {
        synchronized(this.lock) {
            this.traceID = traceID;
            return this;
        }
    }

    public Span setSpanId(String spanID) {
        synchronized(this.lock) {
            this.spanID = spanID;
            return this;
        }
    }

    public Span setParentSpanId(String parentSpanID) {
        synchronized(this.lock) {
            this.parentSpanID = parentSpanID;
            return this;
        }
    }

    public Span setParent(Span span) {
        if (null == span) {
            return this;
        } else {
            synchronized(this.lock) {
                this.parentSpanID = span.spanID;
                this.traceID = span.traceID;
                return this;
            }
        }
    }

    public Span setStart(long start) {
        synchronized(this.lock) {
            this.start = start;
            return this;
        }
    }

    public Span setEnd(long end) {
        synchronized(this.lock) {
            this.end = end;
            return this;
        }
    }

    public Span setDuration(long duration) {
        synchronized(this.lock) {
            this.duration = duration;
            return this;
        }
    }

    public Span setStatus(StatusCode statusCode) {
        synchronized(this.lock) {
            this.statusCode = statusCode;
            return this;
        }
    }

    public Span setStatusMessage(String statusMessage) {
        synchronized(this.lock) {
            this.statusMessage = statusMessage;
            return this;
        }
    }

    public Span setHost(String host) {
        synchronized(this.lock) {
            this.host = host;
            return this;
        }
    }

    public Span setService(String service) {
        synchronized(this.lock) {
            this.service = service;
            return this;
        }
    }

    public Span addAttribute(Attribute attribute) {
        if (null == attribute) {
            return this;
        } else {
            synchronized(this.lock) {
                this.attribute.add(attribute);
                return this;
            }
        }
    }

    public Span addAttribute(Attribute... attributes) {
        if (null == attributes) {
            return this;
        } else {
            synchronized(this.lock) {
                this.addAttribute(Arrays.asList(attributes));
                return this;
            }
        }
    }

    public Span addAttribute(List<Attribute> attributes) {
        if (null == attributes) {
            return this;
        } else {
            synchronized(this.lock) {
                this.attribute.addAll(attributes);
                return this;
            }
        }
    }

    public List<Attribute> getAttribute() {
        return this.attribute;
    }

    public Span addResource(Resource r) {
        if (null == r) {
            return this;
        } else {
            synchronized(this.lock) {
                this.resource.merge(r);
                return this;
            }
        }
    }

    public Resource getResource() {
        return Resource.of(this.resource);
    }

    public Span addEvent(String name) {
        this.addEvent(Event.create(name));
        return this;
    }

    public Span addEvent(String name, Attribute attribute) {
        this.addEvent(Event.create(name).addAttribute(new Attribute[]{attribute}));
        return this;
    }

    public Span addEvent(String name, Attribute... attributes) {
        this.addEvent(Event.create(name).addAttribute(attributes));
        return this;
    }

    public Span addEvent(String name, List<Attribute> attributes) {
        this.addEvent(Event.create(name).addAttribute(attributes));
        return this;
    }

    public Span recordException(Throwable t) {
        this.recordException(t, (Attribute[])null);
        return this;
    }

    public Span recordException(Throwable t, Attribute... attributes) {
        return this.recordException(t, null == attributes ? null : Arrays.asList(attributes));
    }

    public Span recordException(Throwable t, List<Attribute> attributes) {
        StringWriter stringWriter = new StringWriter();

        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            t.printStackTrace(printWriter);
        }

        this.addEvent(Event.create("exception").addAttribute(Attribute.of(new Pair[]{Pair.create("exception.type", t.getClass().getCanonicalName()), Pair.create("exception.message", TextUtils.isEmpty(t.getMessage()) ? "" : t.getMessage()), Pair.create("exception.stacktrace", stringWriter.toString())})).addAttribute(attributes));
        return this;
    }

    public Span addLink(Link link) {
        synchronized(this.lock) {
            this.links.add(link);
            return this;
        }
    }

    private void addEvent(Event event) {
        synchronized(this.lock) {
            this.events.add(event);
        }
    }

    public boolean end() {
        if (this.finished.getAndSet(true)) {
            return false;
        } else {
            synchronized(this.lock) {
                this.duration = this.end - this.start;
                return true;
            }
        }
    }

    public boolean isEnd() {
        return this.finished.get();
    }

    public LogItem toLogItem() {
        synchronized(this.lock) {
            LogItem logItem = new LogItem();
            logItem.SetTime(System.currentTimeMillis());
            logItem.PushBack("name", this.name);
            logItem.PushBack("traceID", this.traceID);
            logItem.PushBack("start", String.valueOf(this.start));
            logItem.PushBack("duration", String.valueOf(this.duration));
            logItem.PushBack("end", String.valueOf(this.end));
            logItem.PushBack("service", TextUtils.isEmpty(this.service) ? "Android" : this.service);
//            logItem.PushBack("kind", this.kind.kind);
//            logItem.PushBack("spanID", this.spanID);
//            logItem.PushBack("parentSpanID", TextUtils.isEmpty(this.parentSpanID) ? "" : this.parentSpanID);
//            logItem.PushBack("sid",  TextUtils.isEmpty(this.sessionId) ? "" : this.sessionId);
//            logItem.PushBack("pid",  TextUtils.isEmpty(this.transactionId) ? "" : this.transactionId);
//            logItem.PushBack("statusCode", this.statusCode.code);
//            logItem.PushBack("statusMessage", TextUtils.isEmpty(this.statusMessage) ? "" : this.statusMessage);
//            logItem.PushBack("host", TextUtils.isEmpty(this.host) ? "" : this.host);

            JSONObject object = new JSONObject();
            Collections.sort(this.attribute);

            for(Attribute attr : this.attribute) {
                JSONUtils.put(object, attr.key, attr.value);
            }

            logItem.PushBack("attribute", object.toString());
            if (null != this.resource) {
                Collections.sort(this.resource.attributes);
                object = new JSONObject();

                for(Attribute attribute : this.resource.attributes) {
                    JSONUtils.put(object, attribute.key, attribute.value);
                }

                logItem.PushBack("resource", object.toString());
            }

//            if (!this.events.isEmpty()) {
//                JSONArray logs = new JSONArray();
//
//                for(Event event : this.events) {
//                    object = new JSONObject();
//                    JSONUtils.put(object, "name", TextUtils.isEmpty(event.getName()) ? "" : event.getName());
//                    JSONUtils.put(object, "epochNanos", event.getEpochNanos());
//                    JSONUtils.put(object, "totalAttributeCount", event.getTotalAttributeCount());
//                    List<Attribute> attributes = event.getAttributes();
//                    Collections.sort(attributes);
//                    JSONObject attrObject = new JSONObject();
//
//                    for(Attribute attr : attributes) {
//                        JSONUtils.put(attrObject, attr.key, attr.value);
//                    }
//
//                    JSONUtils.put(object, "attributes", attrObject);
//                    logs.put(object);
//                }
//
//                logItem.PushBack("logs", logs.toString());
//            }

//            if (!this.links.isEmpty()) {
//                JSONArray links = new JSONArray();
//
//                for(Link link : this.links) {
//                    object = new JSONObject();
//                    JSONUtils.put(object, "traceID", link.getTraceId());
//                    JSONUtils.put(object, "spanID", link.getSpanId());
//                    List<Attribute> attributes = link.getAttributes();
//                    Collections.sort(attributes);
//                    JSONObject attrObject = new JSONObject();
//
//                    for(Attribute attr : attributes) {
//                        JSONUtils.put(attrObject, attr.key, attr.value);
//                    }
//
//                    JSONUtils.put(object, "attributes", attrObject);
//                    links.put(object);
//                }
//
//                logItem.PushBack("links", links.toString());
//            }

            return logItem;
        }
    }

    public String toString() {
        return "Span{name='" + this.name + '\'' + ", kind=" + this.kind + ", traceID='" + this.traceID + '\'' + ", spanID='" + this.spanID + '\'' + ", parentSpanID='" + this.parentSpanID + '\'' + ", start=" + this.start + ", end=" + this.end + ", duration=" + this.duration + ", statusCode=" + this.statusCode + ", statusMessage='" + this.statusMessage + '\'' + ", service='" + this.service + '\'' + ", finished=" + this.finished + '}';
    }



    public static enum StatusCode {
        UNSET("UNSET"),
        OK("OK"),
        ERROR("ERROR");

        public String code;
        public String message;

        private StatusCode(String code) {
            this.code = code;
        }

        public static StatusCode of(String message) {
            StatusCode statusCode = ERROR;
            statusCode.message = message;
            return statusCode;
        }
    }
}
