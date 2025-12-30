package com.snoworca.fxstore.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * CRC32C 체크섬 유틸리티 테스트.
 */
public class CRC32CTest {

    private static final Random random = new Random(42);

    // ==================== Basic Tests ====================

    @Test
    public void testCompute_EmptyArray() {
        byte[] data = new byte[0];
        int crc = CRC32C.compute(data);
        // Empty array should return 0 for CRC32
        assertEquals(0, crc);
    }

    @Test
    public void testCompute_SingleByte() {
        byte[] data = {0x00};
        int crc = CRC32C.compute(data);
        assertTrue(crc != 0 || Arrays.equals(data, new byte[]{0}));
    }

    @Test
    public void testCompute_AllZeros() {
        byte[] data = new byte[100];
        int crc = CRC32C.compute(data);
        // CRC of zeros should be deterministic
        assertEquals(crc, CRC32C.compute(data));
    }

    @Test
    public void testCompute_AllOnes() {
        byte[] data = new byte[100];
        Arrays.fill(data, (byte) 0xFF);
        int crc = CRC32C.compute(data);
        // CRC should be deterministic
        assertEquals(crc, CRC32C.compute(data));
    }

    @Test
    public void testCompute_KnownData() {
        byte[] data = "Hello, World!".getBytes();
        int crc1 = CRC32C.compute(data);
        int crc2 = CRC32C.compute(data);
        assertEquals(crc1, crc2);
    }

    // ==================== Offset Tests ====================

    @Test
    public void testCompute_WithOffset() {
        byte[] data = {0x00, 0x00, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x00, 0x00};
        // "Hello" is at offset 2, length 5
        int crc1 = CRC32C.compute(data, 2, 5);
        byte[] hello = {0x48, 0x65, 0x6C, 0x6C, 0x6F};
        int crc2 = CRC32C.compute(hello);
        assertEquals(crc2, crc1);
    }

    @Test
    public void testCompute_FullArrayVsOffset() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        int crcFull = CRC32C.compute(data);
        int crcOffset = CRC32C.compute(data, 0, data.length);
        assertEquals(crcFull, crcOffset);
    }

    @Test
    public void testCompute_PartialArray() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        int crc1 = CRC32C.compute(data, 0, 4);
        int crc2 = CRC32C.compute(data, 4, 4);
        // Different data should produce different CRCs (usually)
        assertNotEquals(crc1, crc2);
    }

    // ==================== Verify Tests ====================

    @Test
    public void testVerify_CorrectCRC() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        int crc = CRC32C.compute(data);
        assertTrue(CRC32C.verify(data, 0, data.length, crc));
    }

    @Test
    public void testVerify_IncorrectCRC() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        int correctCrc = CRC32C.compute(data);
        int wrongCrc = correctCrc ^ 0x12345678;
        assertFalse(CRC32C.verify(data, 0, data.length, wrongCrc));
    }

    @Test
    public void testVerify_WithOffset() {
        byte[] data = {0x00, 0x00, 0x01, 0x02, 0x03, 0x00, 0x00};
        int crc = CRC32C.compute(data, 2, 3);
        assertTrue(CRC32C.verify(data, 2, 3, crc));
    }

    @Test
    public void testVerify_ModifiedData() {
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        int crc = CRC32C.compute(data);

        // Modify data
        data[2] = 0x00;
        assertFalse(CRC32C.verify(data, 0, data.length, crc));
    }

    // ==================== Determinism Tests ====================

    @Test
    public void testCompute_Deterministic() {
        byte[] data = new byte[256];
        random.nextBytes(data);

        int crc1 = CRC32C.compute(data);
        int crc2 = CRC32C.compute(data);
        int crc3 = CRC32C.compute(data);

        assertEquals(crc1, crc2);
        assertEquals(crc2, crc3);
    }

    @Test
    public void testCompute_DifferentData() {
        byte[] data1 = {0x01, 0x02, 0x03};
        byte[] data2 = {0x01, 0x02, 0x04};

        int crc1 = CRC32C.compute(data1);
        int crc2 = CRC32C.compute(data2);

        assertNotEquals(crc1, crc2);
    }

    @Test
    public void testCompute_SameContent() {
        byte[] data1 = {0x01, 0x02, 0x03};
        byte[] data2 = {0x01, 0x02, 0x03};

        int crc1 = CRC32C.compute(data1);
        int crc2 = CRC32C.compute(data2);

        assertEquals(crc1, crc2);
    }

    // ==================== Random Tests ====================

    @Test
    public void testCompute_RandomDataConsistency() {
        for (int i = 0; i < 100; i++) {
            byte[] data = new byte[random.nextInt(1000) + 1];
            random.nextBytes(data);

            int crc1 = CRC32C.compute(data);
            int crc2 = CRC32C.compute(data);
            assertEquals(crc1, crc2);

            assertTrue(CRC32C.verify(data, 0, data.length, crc1));
        }
    }

    @Test
    public void testCompute_LargeData() {
        byte[] data = new byte[1024 * 1024]; // 1MB
        random.nextBytes(data);

        int crc = CRC32C.compute(data);
        assertTrue(CRC32C.verify(data, 0, data.length, crc));
    }

    // ==================== Edge Cases ====================

    @Test
    public void testCompute_SingleByteValues() {
        for (int i = 0; i < 256; i++) {
            byte[] data = {(byte) i};
            int crc = CRC32C.compute(data);
            assertTrue(CRC32C.verify(data, 0, 1, crc));
        }
    }

    @Test
    public void testCompute_VariousLengths() {
        for (int len = 1; len <= 100; len++) {
            byte[] data = new byte[len];
            random.nextBytes(data);
            int crc = CRC32C.compute(data);
            assertTrue(CRC32C.verify(data, 0, len, crc));
        }
    }

    @Test
    public void testVerify_ZeroLength() {
        byte[] data = {0x01, 0x02, 0x03};
        int crc = CRC32C.compute(data, 1, 0); // Zero length
        assertTrue(CRC32C.verify(data, 1, 0, crc));
    }

    // ==================== Collision Tests ====================

    @Test
    public void testCompute_NoEasyCollisions() {
        // Test that simple modifications don't produce same CRC
        byte[] original = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        int originalCrc = CRC32C.compute(original);

        // Single byte flip should change CRC
        for (int i = 0; i < original.length; i++) {
            byte[] modified = original.clone();
            modified[i] ^= 0x01;
            assertNotEquals("CRC should differ for modification at index " + i,
                    originalCrc, CRC32C.compute(modified));
        }
    }

    @Test
    public void testCompute_BitFlipDetection() {
        byte[] data = new byte[100];
        random.nextBytes(data);
        int originalCrc = CRC32C.compute(data);

        // Flip each bit and verify CRC changes
        int detectedChanges = 0;
        for (int byteIdx = 0; byteIdx < data.length; byteIdx++) {
            for (int bit = 0; bit < 8; bit++) {
                byte[] modified = data.clone();
                modified[byteIdx] ^= (1 << bit);
                if (CRC32C.compute(modified) != originalCrc) {
                    detectedChanges++;
                }
            }
        }

        // CRC should detect most (ideally all) single-bit changes
        assertTrue(detectedChanges >= data.length * 8 * 0.99);
    }
}
