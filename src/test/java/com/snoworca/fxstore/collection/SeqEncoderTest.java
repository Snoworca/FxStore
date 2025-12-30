package com.snoworca.fxstore.collection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SeqEncoder 단위 테스트
 *
 * <p>INV-DQ1 검증: encode(a) < encode(b) ⟺ a < b</p>
 */
public class SeqEncoderTest {

    // ==================== 바이트 비교 헬퍼 ====================

    /**
     * unsigned byte 비교 (BTree와 동일한 방식)
     */
    private int compareBytes(byte[] a, byte[] b) {
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; i++) {
            int cmp = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (cmp != 0) {
                return cmp;
            }
        }
        return a.length - b.length;
    }

    // ==================== OrderedSeqEncoder 테스트 ====================

    @Test
    public void testOrderedEncoder_positiveSequences() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        byte[] encoded0 = encoder.encode(0);
        byte[] encoded1 = encoder.encode(1);
        byte[] encoded100 = encoder.encode(100);
        byte[] encodedMax = encoder.encode(Long.MAX_VALUE);

        assertTrue("0 < 1", compareBytes(encoded0, encoded1) < 0);
        assertTrue("1 < 100", compareBytes(encoded1, encoded100) < 0);
        assertTrue("100 < MAX", compareBytes(encoded100, encodedMax) < 0);
    }

    @Test
    public void testOrderedEncoder_negativeSequences() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        byte[] encodedMin = encoder.encode(Long.MIN_VALUE);
        byte[] encodedMinus100 = encoder.encode(-100);
        byte[] encodedMinus2 = encoder.encode(-2);
        byte[] encodedMinus1 = encoder.encode(-1);

        assertTrue("MIN < -100", compareBytes(encodedMin, encodedMinus100) < 0);
        assertTrue("-100 < -2", compareBytes(encodedMinus100, encodedMinus2) < 0);
        assertTrue("-2 < -1", compareBytes(encodedMinus2, encodedMinus1) < 0);
    }

    @Test
    public void testOrderedEncoder_mixedSequences() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        // 전체 범위 테스트
        long[] sequences = {Long.MIN_VALUE, -1000, -1, 0, 1, 1000, Long.MAX_VALUE};

        for (int i = 0; i < sequences.length - 1; i++) {
            byte[] a = encoder.encode(sequences[i]);
            byte[] b = encoder.encode(sequences[i + 1]);
            assertTrue("seq " + sequences[i] + " < seq " + sequences[i + 1],
                       compareBytes(a, b) < 0);
        }
    }

    @Test
    public void testOrderedEncoder_dequeTypicalSequences() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        // Deque에서 실제로 사용되는 시퀀스 패턴
        // addFirst: headSeq 감소 (-1, -2, -3, ...)
        // addLast: tailSeq 증가 (0, 1, 2, ...)
        long[] typicalSeqs = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5};

        for (int i = 0; i < typicalSeqs.length - 1; i++) {
            byte[] a = encoder.encode(typicalSeqs[i]);
            byte[] b = encoder.encode(typicalSeqs[i + 1]);
            assertTrue("seq " + typicalSeqs[i] + " < seq " + typicalSeqs[i + 1],
                       compareBytes(a, b) < 0);
        }
    }

    @Test
    public void testOrderedEncoder_roundTrip() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        long[] testValues = {
            Long.MIN_VALUE,
            Long.MIN_VALUE + 1,
            -1000000,
            -1,
            0,
            1,
            1000000,
            Long.MAX_VALUE - 1,
            Long.MAX_VALUE
        };

        for (long val : testValues) {
            byte[] encoded = encoder.encode(val);
            long decoded = encoder.decode(encoded);
            assertEquals("Round trip for " + val, val, decoded);
        }
    }

    @Test
    public void testOrderedEncoder_encodedLength() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        assertEquals(8, encoder.encode(0).length);
        assertEquals(8, encoder.encode(Long.MIN_VALUE).length);
        assertEquals(8, encoder.encode(Long.MAX_VALUE).length);
    }

    @Test
    public void testOrderedEncoder_version() {
        assertEquals(0x01, OrderedSeqEncoder.getInstance().getVersion());
    }

    // ==================== LegacySeqEncoder 테스트 ====================

    @Test
    public void testLegacyEncoder_roundTrip() {
        SeqEncoder encoder = LegacySeqEncoder.getInstance();

        long[] testValues = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};

        for (long val : testValues) {
            byte[] encoded = encoder.encode(val);
            long decoded = encoder.decode(encoded);
            assertEquals("Round trip for " + val, val, decoded);
        }
    }

    @Test
    public void testLegacyEncoder_orderingIssue() {
        // 레거시 인코더는 순서가 보장되지 않음을 검증
        SeqEncoder encoder = LegacySeqEncoder.getInstance();

        byte[] encodedMinus1 = encoder.encode(-1);
        byte[] encoded0 = encoder.encode(0);

        // Little Endian에서는 -1이 0보다 바이트 비교에서 크게 나옴
        // 이것이 문제: 논리적으로 -1 < 0이지만 바이트 비교는 반대
        assertTrue("Legacy: -1 bytes > 0 bytes (this is the problem!)",
                   compareBytes(encodedMinus1, encoded0) > 0);
    }

    @Test
    public void testLegacyEncoder_version() {
        assertEquals(0x00, LegacySeqEncoder.getInstance().getVersion());
    }

    // ==================== 예외 테스트 ====================

    @Test(expected = IllegalArgumentException.class)
    public void testOrderedEncoder_decodeNull() {
        OrderedSeqEncoder.getInstance().decode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOrderedEncoder_decodeWrongLength() {
        OrderedSeqEncoder.getInstance().decode(new byte[4]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyEncoder_decodeNull() {
        LegacySeqEncoder.getInstance().decode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyEncoder_decodeWrongLength() {
        LegacySeqEncoder.getInstance().decode(new byte[16]);
    }

    // ==================== INV-DQ1 전체 검증 ====================

    @Test
    public void testINV_DQ1_OrderPreservation_fullRange() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        // 10,000개의 랜덤 시퀀스로 순서 보존 검증
        java.util.Random random = new java.util.Random(42);
        long[] sequences = new long[1000];
        for (int i = 0; i < sequences.length; i++) {
            sequences[i] = random.nextLong();
        }

        // 정렬
        java.util.Arrays.sort(sequences);

        // 인코딩 후 바이트 비교도 동일 순서인지 검증
        for (int i = 0; i < sequences.length - 1; i++) {
            if (sequences[i] != sequences[i + 1]) {
                byte[] a = encoder.encode(sequences[i]);
                byte[] b = encoder.encode(sequences[i + 1]);
                assertTrue("INV-DQ1 violation: " + sequences[i] + " vs " + sequences[i + 1],
                           compareBytes(a, b) < 0);
            }
        }
    }

    @Test
    public void testINV_DQ1_BoundaryValues() {
        SeqEncoder encoder = OrderedSeqEncoder.getInstance();

        // 경계값 테스트
        long[][] pairs = {
            {Long.MIN_VALUE, Long.MIN_VALUE + 1},
            {-1, 0},
            {0, 1},
            {Long.MAX_VALUE - 1, Long.MAX_VALUE}
        };

        for (long[] pair : pairs) {
            byte[] a = encoder.encode(pair[0]);
            byte[] b = encoder.encode(pair[1]);
            assertTrue("Boundary: " + pair[0] + " < " + pair[1],
                       compareBytes(a, b) < 0);
        }
    }
}
