package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Codec for Byte values using CANONICAL mode (stored as I64).
 */
public final class ByteCodec implements FxCodec<Byte> {

    public static final String CODEC_ID = "fx:i64";
    public static final int VERSION = 1;

    public static final ByteCodec INSTANCE = new ByteCodec();

    private ByteCodec() {
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
    public byte[] encode(Byte value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(value.longValue());
        return buf.array();
    }

    @Override
    public Byte decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        long val = buf.getLong();
        if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE) {
            throw new ArithmeticException("Value out of Byte range: " + val);
        }
        return (byte) val;
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
