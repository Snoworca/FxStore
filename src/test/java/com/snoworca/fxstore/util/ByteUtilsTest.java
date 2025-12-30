package com.snoworca.fxstore.util;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * ByteUtils 유틸리티 클래스 테스트.
 */
public class ByteUtilsTest {

    private static final Random random = new Random(42);

    // ==================== Little-Endian Read Tests ====================

    @Test
    public void testReadU16LE_Basic() {
        byte[] data = {0x01, 0x02};
        assertEquals(0x0201, ByteUtils.readU16LE(data, 0));
    }

    @Test
    public void testReadU16LE_MaxValue() {
        byte[] data = {(byte) 0xFF, (byte) 0xFF};
        assertEquals(0xFFFF, ByteUtils.readU16LE(data, 0));
    }

    @Test
    public void testReadU16LE_WithOffset() {
        byte[] data = {0x00, 0x00, 0x34, 0x12};
        assertEquals(0x1234, ByteUtils.readU16LE(data, 2));
    }

    @Test
    public void testReadI32LE_Positive() {
        byte[] data = {0x78, 0x56, 0x34, 0x12};
        assertEquals(0x12345678, ByteUtils.readI32LE(data, 0));
    }

    @Test
    public void testReadI32LE_Negative() {
        byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertEquals(-1, ByteUtils.readI32LE(data, 0));
    }

    @Test
    public void testReadI32LE_MinValue() {
        byte[] data = {0x00, 0x00, 0x00, (byte) 0x80};
        assertEquals(Integer.MIN_VALUE, ByteUtils.readI32LE(data, 0));
    }

    @Test
    public void testReadU32LE_MaxUnsigned() {
        byte[] data = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertEquals(0xFFFFFFFFL, ByteUtils.readU32LE(data, 0));
    }

    @Test
    public void testReadI64LE_Positive() {
        byte[] data = new byte[8];
        ByteUtils.writeI64LE(data, 0, 0x123456789ABCDEF0L);
        assertEquals(0x123456789ABCDEF0L, ByteUtils.readI64LE(data, 0));
    }

    @Test
    public void testReadI64LE_Negative() {
        byte[] data = new byte[8];
        ByteUtils.writeI64LE(data, 0, -1L);
        assertEquals(-1L, ByteUtils.readI64LE(data, 0));
    }

    @Test
    public void testReadI64LE_MinMax() {
        byte[] data = new byte[8];

        ByteUtils.writeI64LE(data, 0, Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, ByteUtils.readI64LE(data, 0));

        ByteUtils.writeI64LE(data, 0, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, ByteUtils.readI64LE(data, 0));
    }

    // ==================== Little-Endian Write Tests ====================

    @Test
    public void testWriteU16LE_Basic() {
        byte[] data = new byte[2];
        ByteUtils.writeU16LE(data, 0, 0x1234);
        assertEquals(0x34, data[0] & 0xFF);
        assertEquals(0x12, data[1] & 0xFF);
    }

    @Test
    public void testWriteI32LE_Basic() {
        byte[] data = new byte[4];
        ByteUtils.writeI32LE(data, 0, 0x12345678);
        assertEquals(0x78, data[0] & 0xFF);
        assertEquals(0x56, data[1] & 0xFF);
        assertEquals(0x34, data[2] & 0xFF);
        assertEquals(0x12, data[3] & 0xFF);
    }

    @Test
    public void testWriteI32LE_Negative() {
        byte[] data = new byte[4];
        ByteUtils.writeI32LE(data, 0, -1);
        assertEquals(0xFF, data[0] & 0xFF);
        assertEquals(0xFF, data[1] & 0xFF);
        assertEquals(0xFF, data[2] & 0xFF);
        assertEquals(0xFF, data[3] & 0xFF);
    }

    @Test
    public void testWriteI64LE_Basic() {
        byte[] data = new byte[8];
        ByteUtils.writeI64LE(data, 0, 0x0102030405060708L);
        assertEquals(0x08, data[0] & 0xFF);
        assertEquals(0x07, data[1] & 0xFF);
        assertEquals(0x06, data[2] & 0xFF);
        assertEquals(0x05, data[3] & 0xFF);
        assertEquals(0x04, data[4] & 0xFF);
        assertEquals(0x03, data[5] & 0xFF);
        assertEquals(0x02, data[6] & 0xFF);
        assertEquals(0x01, data[7] & 0xFF);
    }

    // ==================== Read/Write Roundtrip Tests ====================

    @Test
    public void testU16LE_Roundtrip() {
        byte[] data = new byte[2];
        for (int value = 0; value < 0x10000; value += 0x1111) {
            ByteUtils.writeU16LE(data, 0, value);
            assertEquals(value, ByteUtils.readU16LE(data, 0));
        }
    }

    @Test
    public void testI32LE_Roundtrip() {
        byte[] data = new byte[4];
        int[] values = {0, 1, -1, 100, -100, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int value : values) {
            ByteUtils.writeI32LE(data, 0, value);
            assertEquals(value, ByteUtils.readI32LE(data, 0));
        }
    }

    @Test
    public void testI64LE_Roundtrip() {
        byte[] data = new byte[8];
        long[] values = {0L, 1L, -1L, 100L, -100L, Long.MAX_VALUE, Long.MIN_VALUE};
        for (long value : values) {
            ByteUtils.writeI64LE(data, 0, value);
            assertEquals(value, ByteUtils.readI64LE(data, 0));
        }
    }

    @Test
    public void testI32LE_RandomRoundtrip() {
        byte[] data = new byte[4];
        for (int i = 0; i < 1000; i++) {
            int value = random.nextInt();
            ByteUtils.writeI32LE(data, 0, value);
            assertEquals(value, ByteUtils.readI32LE(data, 0));
        }
    }

    @Test
    public void testI64LE_RandomRoundtrip() {
        byte[] data = new byte[8];
        for (int i = 0; i < 1000; i++) {
            long value = random.nextLong();
            ByteUtils.writeI64LE(data, 0, value);
            assertEquals(value, ByteUtils.readI64LE(data, 0));
        }
    }

    // ==================== Varint Tests ====================

    @Test
    public void testEncodeVarint_Zero() {
        byte[] result = ByteUtils.encodeVarint(0);
        assertEquals(1, result.length);
        assertEquals(0, result[0]);
    }

    @Test
    public void testEncodeVarint_SingleByte() {
        byte[] result = ByteUtils.encodeVarint(127);
        assertEquals(1, result.length);
        assertEquals(127, result[0]);
    }

    @Test
    public void testEncodeVarint_TwoBytes() {
        byte[] result = ByteUtils.encodeVarint(128);
        assertEquals(2, result.length);
        assertEquals((byte) 0x80, result[0]);
        assertEquals(1, result[1]);
    }

    @Test
    public void testEncodeVarint_LargeValue() {
        byte[] result = ByteUtils.encodeVarint(300);
        assertEquals(2, result.length);
    }

    @Test
    public void testEncodeVarint_MaxLong() {
        byte[] result = ByteUtils.encodeVarint(Long.MAX_VALUE);
        assertTrue(result.length <= 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeVarint_Negative() {
        ByteUtils.encodeVarint(-1);
    }

    @Test
    public void testDecodeVarint_Zero() {
        byte[] data = {0};
        int[] bytesRead = new int[1];
        long value = ByteUtils.decodeVarint(data, 0, bytesRead);
        assertEquals(0, value);
        assertEquals(1, bytesRead[0]);
    }

    @Test
    public void testDecodeVarint_SingleByte() {
        byte[] data = {127};
        int[] bytesRead = new int[1];
        long value = ByteUtils.decodeVarint(data, 0, bytesRead);
        assertEquals(127, value);
        assertEquals(1, bytesRead[0]);
    }

    @Test
    public void testDecodeVarint_TwoBytes() {
        byte[] data = {(byte) 0x80, 0x01};
        int[] bytesRead = new int[1];
        long value = ByteUtils.decodeVarint(data, 0, bytesRead);
        assertEquals(128, value);
        assertEquals(2, bytesRead[0]);
    }

    @Test
    public void testVarint_Roundtrip() {
        long[] values = {0, 1, 127, 128, 255, 256, 16383, 16384, 100000, 1000000, Long.MAX_VALUE};
        for (long original : values) {
            byte[] encoded = ByteUtils.encodeVarint(original);
            int[] bytesRead = new int[1];
            long decoded = ByteUtils.decodeVarint(encoded, 0, bytesRead);
            assertEquals(original, decoded);
            assertEquals(encoded.length, bytesRead[0]);
        }
    }

    @Test
    public void testVarint_RandomRoundtrip() {
        for (int i = 0; i < 1000; i++) {
            long value = Math.abs(random.nextLong());
            byte[] encoded = ByteUtils.encodeVarint(value);
            int[] bytesRead = new int[1];
            long decoded = ByteUtils.decodeVarint(encoded, 0, bytesRead);
            assertEquals(value, decoded);
        }
    }

    @Test
    public void testDecodeVarint_WithOffset() {
        byte[] data = {0x00, 0x00, (byte) 0xAC, 0x02};
        int[] bytesRead = new int[1];
        long value = ByteUtils.decodeVarint(data, 2, bytesRead);
        assertEquals(300, value);
        assertEquals(2, bytesRead[0]);
    }

    // ==================== Alignment Tests ====================

    @Test
    public void testAlign8_AlreadyAligned() {
        assertEquals(0, ByteUtils.align8(0));
        assertEquals(8, ByteUtils.align8(8));
        assertEquals(16, ByteUtils.align8(16));
        assertEquals(64, ByteUtils.align8(64));
    }

    @Test
    public void testAlign8_NeedsAlignment() {
        assertEquals(8, ByteUtils.align8(1));
        assertEquals(8, ByteUtils.align8(2));
        assertEquals(8, ByteUtils.align8(7));
        assertEquals(16, ByteUtils.align8(9));
        assertEquals(16, ByteUtils.align8(15));
    }

    @Test
    public void testAlign8Long_AlreadyAligned() {
        assertEquals(0L, ByteUtils.align8(0L));
        assertEquals(8L, ByteUtils.align8(8L));
        assertEquals(16L, ByteUtils.align8(16L));
    }

    @Test
    public void testAlign8Long_NeedsAlignment() {
        assertEquals(8L, ByteUtils.align8(1L));
        assertEquals(8L, ByteUtils.align8(7L));
        assertEquals(16L, ByteUtils.align8(9L));
    }

    @Test
    public void testAlign8_LargeValues() {
        assertEquals(1000, ByteUtils.align8(1000)); // 1000 is already aligned (1000 % 8 == 0)
        assertEquals(1008, ByteUtils.align8(1001));
        assertEquals(1008, ByteUtils.align8(1007));
    }

    @Test
    public void testAlign8Long_LargeValues() {
        assertEquals(0x100000000L, ByteUtils.align8(0x100000000L)); // Already aligned
        assertEquals(0x100000008L, ByteUtils.align8(0x100000001L));
    }

    // ==================== Edge Cases ====================

    @Test
    public void testWriteRead_WithOffset() {
        byte[] data = new byte[20];

        // Write at various offsets
        ByteUtils.writeU16LE(data, 0, 0x1234);
        ByteUtils.writeI32LE(data, 4, 0x12345678);
        ByteUtils.writeI64LE(data, 8, 0x123456789ABCDEF0L);

        // Read back
        assertEquals(0x1234, ByteUtils.readU16LE(data, 0));
        assertEquals(0x12345678, ByteUtils.readI32LE(data, 4));
        assertEquals(0x123456789ABCDEF0L, ByteUtils.readI64LE(data, 8));
    }
}
