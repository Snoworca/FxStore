package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Built-in codec for 64-bit floating point (IEEE754).
 * Stores as 8-byte little-endian.
 */
public final class F64Codec implements FxCodec<Double> {

    public static final String CODEC_ID = "fx:f64";
    public static final int VERSION = 1;

    public static final F64Codec INSTANCE = new F64Codec();

    private F64Codec() {
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
    public byte[] encode(Double value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(value);
        return buf.array();
    }

    @Override
    public Double decode(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return buf.getDouble();
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
