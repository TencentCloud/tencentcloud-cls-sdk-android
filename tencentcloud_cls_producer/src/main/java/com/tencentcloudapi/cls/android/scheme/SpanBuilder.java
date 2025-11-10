package com.tencentcloudapi.cls.android.scheme;

import com.tencentcloudapi.cls.android.utils.IdGenerator;
import com.tencentcloudapi.cls.android.utils.TimeUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpanBuilder {
    private final String spanName;
    private final ISpanProvider spanProvider;
    private Span parent;
    private SpanKind kind;
    private final List<Attribute> attributes;
    private final Resource resource;
    private Long start;


    public SpanBuilder(String spanName, ISpanProvider provider) {
        this.kind = SpanKind.CLIENT;
        this.attributes = new CopyOnWriteArrayList();
        this.resource = new Resource();
        this.start = null;
        this.spanName = spanName;
        this.spanProvider = provider;
    }

    public SpanBuilder setParent(Span span) {
        this.parent = span;
        return this;
    }

    public SpanBuilder setKind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    public SpanBuilder addAttribute(Attribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    public SpanBuilder addAttribute(List<Attribute> attributes) {
        this.attributes.addAll(attributes);
        return this;
    }

    public SpanBuilder setStart(Long start) {
        this.start = start;
        return this;
    }

    public SpanBuilder addResource(Resource resource) {
        this.resource.merge(resource);
        return this;
    }

    public Span build() {
        Span span = new Span();
        span.setName(this.spanName);
//        span.setSpanId(IdGenerator.generateSpanId());
        span.setTraceId(IdGenerator.generateTraceId());
//        span.setKind(this.kind);
        if (null != this.spanProvider) {
            List<Attribute> attrs = this.spanProvider.provideAttribute();
            if (null != attrs) {
                span.addAttribute(attrs);
            }
        }
        span.addAttribute(this.attributes);
        Resource r = Resource.getDefault();
        if (null != this.spanProvider) {
            r.merge(this.spanProvider.provideResource());
        }
        r.merge(this.resource);
        span.addResource(r);
        span.setStart(null != this.start ? this.start : TimeUtils.instance.now());
        span.setEnd(TimeUtils.instance.now());
        return span;
    }
}
