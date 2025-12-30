package com.snoworca.fxstore.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Byte manipulation utilities.
 */
public final class ByteUtils {

    private ByteUtils() {
    }

    // ==================== Little-Endian Read ====================

    public static int readU16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    public static int readI32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    public static long readU32LE(byte[] data, int offset) {
        return readI32LE(data, offset) & 0xFFFFFFFFL;
    }

    public static long readI64LE(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    // ==================== Little-Endian Write ====================

    public static void writeU16LE(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >> 8);
    }

    public static void writeI32LE(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >> 8);
        data[offset + 2] = (byte) (value >> 16);
        data[offset + 3] = (byte) (value >> 24);
    }

    public static void writeI64LE(byte[] data, int offset, long value) {
        ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value);
    }

    // ==================== Varint (LEB128) ====================

    public static byte[] encodeVarint(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative value not supported: " + value);
        }
        byte[] buf = new byte[10];
        int pos = 0;
        while (value > 0x7F) {
            buf[pos++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf[pos++] = (byte) value;
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
        return result;
    }

    public static long decodeVarint(byte[] data, int offset, int[] bytesRead) {
        long result = 0;
        int shift = 0;
        int pos = offset;

        while (true) {
            byte b = data[pos++];
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
            if (shift > 63) {
                throw new IllegalStateException("Varint too long");
            }
        }

        bytesRead[0] = pos - offset;
        return result;
    }

    // ==================== Alignment ====================

    public static long align8(long offset) {
        return (offset + 7) & ~7L;
    }

    public static int align8(int offset) {
        return (offset + 7) & ~7;
    }
}
