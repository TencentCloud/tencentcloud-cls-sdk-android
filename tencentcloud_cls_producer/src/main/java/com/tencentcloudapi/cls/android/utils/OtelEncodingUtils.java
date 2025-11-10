package com.tencentcloudapi.cls.android.utils;

import java.util.Arrays;

public class OtelEncodingUtils {
    static final int LONG_BYTES = 8;
    static final int BYTE_BASE16 = 2;
    static final int LONG_BASE16 = 16;
    private static final String ALPHABET = "0123456789abcdef";
    private static final int NUM_ASCII_CHARACTERS = 128;
    private static final char[] ENCODING = buildEncodingArray();
    private static final byte[] DECODING = buildDecodingArray();
    private static final boolean[] VALID_HEX = buildValidHexArray();

    private static char[] buildEncodingArray() {
        char[] encoding = new char[512];

        for(int i = 0; i < 256; ++i) {
            encoding[i] = "0123456789abcdef".charAt(i >>> 4);
            encoding[i | 256] = "0123456789abcdef".charAt(i & 15);
        }

        return encoding;
    }

    private static byte[] buildDecodingArray() {
        byte[] decoding = new byte[128];
        Arrays.fill(decoding, (byte)-1);

        for(int i = 0; i < "0123456789abcdef".length(); ++i) {
            char c = "0123456789abcdef".charAt(i);
            decoding[c] = (byte)i;
        }

        return decoding;
    }

    private static boolean[] buildValidHexArray() {
        boolean[] validHex = new boolean['\uffff'];

        for(int i = 0; i < 65535; ++i) {
            validHex[i] = 48 <= i && i <= 57 || 97 <= i && i <= 102;
        }

        return validHex;
    }

    public static long longFromBase16String(CharSequence chars, int offset) {
        return ((long)byteFromBase16(chars.charAt(offset), chars.charAt(offset + 1)) & 255L) << 56 | ((long)byteFromBase16(chars.charAt(offset + 2), chars.charAt(offset + 3)) & 255L) << 48 | ((long)byteFromBase16(chars.charAt(offset + 4), chars.charAt(offset + 5)) & 255L) << 40 | ((long)byteFromBase16(chars.charAt(offset + 6), chars.charAt(offset + 7)) & 255L) << 32 | ((long)byteFromBase16(chars.charAt(offset + 8), chars.charAt(offset + 9)) & 255L) << 24 | ((long)byteFromBase16(chars.charAt(offset + 10), chars.charAt(offset + 11)) & 255L) << 16 | ((long)byteFromBase16(chars.charAt(offset + 12), chars.charAt(offset + 13)) & 255L) << 8 | (long)byteFromBase16(chars.charAt(offset + 14), chars.charAt(offset + 15)) & 255L;
    }

    public static void longToBase16String(long value, char[] dest, int destOffset) {
        byteToBase16((byte)((int)(value >> 56 & 255L)), dest, destOffset);
        byteToBase16((byte)((int)(value >> 48 & 255L)), dest, destOffset + 2);
        byteToBase16((byte)((int)(value >> 40 & 255L)), dest, destOffset + 4);
        byteToBase16((byte)((int)(value >> 32 & 255L)), dest, destOffset + 6);
        byteToBase16((byte)((int)(value >> 24 & 255L)), dest, destOffset + 8);
        byteToBase16((byte)((int)(value >> 16 & 255L)), dest, destOffset + 10);
        byteToBase16((byte)((int)(value >> 8 & 255L)), dest, destOffset + 12);
        byteToBase16((byte)((int)(value & 255L)), dest, destOffset + 14);
    }

    public static byte[] bytesFromBase16(CharSequence value, int length) {
        byte[] result = new byte[length / 2];

        for(int i = 0; i < length; i += 2) {
            result[i / 2] = byteFromBase16(value.charAt(i), value.charAt(i + 1));
        }

        return result;
    }

    public static void bytesToBase16(byte[] bytes, char[] dest, int length) {
        for(int i = 0; i < length; ++i) {
            byteToBase16(bytes[i], dest, i * 2);
        }

    }

    public static void byteToBase16(byte value, char[] dest, int destOffset) {
        int b = value & 255;
        dest[destOffset] = ENCODING[b];
        dest[destOffset + 1] = ENCODING[b | 256];
    }

    public static byte byteFromBase16(char first, char second) {
        if (first < 128 && DECODING[first] != -1) {
            if (second < 128 && DECODING[second] != -1) {
                int decoded = DECODING[first] << 4 | DECODING[second];
                return (byte)decoded;
            } else {
                throw new IllegalArgumentException("invalid character " + second);
            }
        } else {
            throw new IllegalArgumentException("invalid character " + first);
        }
    }

    public static boolean isValidBase16String(CharSequence value) {
        int len = value.length();

        for(int i = 0; i < len; ++i) {
            char b = value.charAt(i);
            if (!isValidBase16Character(b)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidBase16Character(char b) {
        return VALID_HEX[b];
    }

    private OtelEncodingUtils() {
    }
}
