package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.util.Arrays;

/**
 * Built-in codec for byte arrays.
 * Comparison: length-prefixed lexicographic (shorter arrays come first,
 * then unsigned lexicographic comparison).
 */
public final class BytesCodec implements FxCodec<byte[]> {

    public static final String CODEC_ID = "fx:bytes:lenlex";
    public static final int VERSION = 1;

    public static final BytesCodec INSTANCE = new BytesCodec();

    private BytesCodec() {
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
    public byte[] encode(byte[] value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public byte[] decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public int compareBytes(byte[] a, byte[] b) {
        // Length-prefixed ordering: shorter arrays come first
        if (a.length != b.length) {
            return Integer.compare(a.length, b.length);
        }
        // Same length: unsigned lexicographic
        for (int i = 0; i < a.length; i++) {
            int va = a[i] & 0xFF;
            int vb = b[i] & 0xFF;
            if (va != vb) {
                return va - vb;
            }
        }
        return 0;
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
