package com.tencentcloudapi.cls.android.utils;

import java.util.Random;

public class IdGenerator {
    private static final int TRACE_ID_BYTES_LENGTH = 16;
    private static final int TRACE_ID_HEX_LENGTH = 32;
    private static final int SPAN_ID_BYTES_LENGTH = 8;
    public static final int SPAN_ID_HEX_LENGTH = 16;
    private static final long INVALID_ID = 0L;
    private static final ThreadLocal<char[]> CHAR_ARRAY = new ThreadLocal();
    private static final Random random = new Random();

    public IdGenerator() {
    }

    public static String generateTraceId() {
        long idHi = random.nextLong();

        long idLo;
        do {
            idLo = random.nextLong();
        } while(0L == idLo);

        char[] chars = temporaryBuffers(32);
        OtelEncodingUtils.longToBase16String(idHi, chars, 0);
        OtelEncodingUtils.longToBase16String(idLo, chars, 16);
        return new String(chars, 0, 32);
    }

    public static String generateSpanId() {
        long id;
        do {
            id = random.nextLong();
        } while(id == 0L);

        char[] result = temporaryBuffers(16);
        OtelEncodingUtils.longToBase16String(id, result, 0);
        return new String(result, 0, 16);
    }

    private static char[] temporaryBuffers(int len) {
        char[] buffer = (char[])CHAR_ARRAY.get();
        if (null == buffer || buffer.length < len) {
            buffer = new char[len];
            CHAR_ARRAY.set(buffer);
        }

        return buffer;
    }
}
