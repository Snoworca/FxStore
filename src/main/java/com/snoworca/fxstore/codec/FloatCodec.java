package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Codec for Float values using CANONICAL mode (stored as F64).
 */
public final class FloatCodec implements FxCodec<Float> {

    public static final String CODEC_ID = "fx:f64";
    public static final int VERSION = 1;

    public static final FloatCodec INSTANCE = new FloatCodec();

    private FloatCodec() {
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
    public byte[] encode(Float value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(value.doubleValue());
        return buf.array();
    }

    @Override
    public Float decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        double val = buf.getDouble();
        return (float) val;
    }

    @Override
    public int compareBytes(byte[] a, byte[] b) {
        double va = ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).getDouble();
        double vb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getDouble();
        return Double.compare(va, vb);
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
