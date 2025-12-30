package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Built-in codec for UTF-8 strings.
 * Comparison is unsigned lexicographic on UTF-8 bytes.
 */
public final class StringCodec implements FxCodec<String> {

    public static final String CODEC_ID = "fx:string:utf8";
    public static final int VERSION = 1;

    public static final StringCodec INSTANCE = new StringCodec();

    private StringCodec() {
    }

    @Override
    public String id() {
        return CODEC_ID;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public byte[] encode(String value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public int compareBytes(byte[] a, byte[] b) {
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; i++) {
            int va = a[i] & 0xFF;
            int vb = b[i] & 0xFF;
            if (va != vb) {
                return va - vb;
            }
        }
        return Integer.compare(a.length, b.length);
    }

    @Override
    public boolean equalsBytes(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }

    @Override
    public int hashBytes(byte[] bytes) {
        return Arrays.hashCode(bytes);
    }
}
