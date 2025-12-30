package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Codec for Integer values using CANONICAL mode (stored as I64).
 */
public final class IntegerCodec implements FxCodec<Integer> {

    public static final String CODEC_ID = "fx:i64";
    public static final int VERSION = 1;

    public static final IntegerCodec INSTANCE = new IntegerCodec();

    private IntegerCodec() {
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
    public byte[] encode(Integer value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(value.longValue());
        return buf.array();
    }

    @Override
    public Integer decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        long val = buf.getLong();
        if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE) {
            throw new ArithmeticException("Value out of Integer range: " + val);
        }
        return (int) val;
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
