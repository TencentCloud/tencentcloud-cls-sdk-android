package com.tencentcloudapi.cls.android.scheme;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public enum SpanKind {
    INTERNAL("INTERNAL"),
    SERVER("SERVER"),
    CLIENT("CLIENT"),
    PRODUCER("PRODUCER"),
    CONSUMER("CONSUMER");

    private static final Map<String, SpanKind> sSpanKindMap = new HashMap<String, SpanKind>() {
        {
            this.put(SpanKind.INTERNAL.kind, SpanKind.INTERNAL);
            this.put(SpanKind.SERVER.kind, SpanKind.SERVER);
            this.put(SpanKind.CLIENT.kind, SpanKind.CLIENT);
            this.put(SpanKind.PRODUCER.kind, SpanKind.PRODUCER);
            this.put(SpanKind.CONSUMER.kind, SpanKind.CONSUMER);
        }
    };
    public final String kind;

    private SpanKind(String kind) {
        this.kind = kind;
    }

    public static SpanKind kindOf(String kind) {
        if (TextUtils.isEmpty(kind)) {
            return INTERNAL;
        } else {
            return !sSpanKindMap.containsKey(kind.toUpperCase()) ? INTERNAL : (SpanKind)sSpanKindMap.get(kind.toUpperCase());
        }
    }
}
