package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Built-in codec for 64-bit signed integers.
 * Stores as 8-byte little-endian.
 */
public final class I64Codec implements FxCodec<Long> {

    public static final String CODEC_ID = "fx:i64";
    public static final int VERSION = 1;

    public static final I64Codec INSTANCE = new I64Codec();

    private I64Codec() {
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
    public byte[] encode(Long value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(value);
        return buf.array();
    }

    @Override
    public Long decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return buf.getLong();
    }

    @Override
    public int compareBytes(byte[] a, byte[] b) {
        long va = ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).getLong();
        long vb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Long.compare(va, vb);
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
