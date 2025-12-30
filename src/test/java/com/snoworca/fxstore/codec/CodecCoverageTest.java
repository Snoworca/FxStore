package com.snoworca.fxstore.codec;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 코덱 미커버 브랜치 테스트
 *
 * <p>커버리지 개선 대상:</p>
 * <ul>
 *   <li>BytesCodec: null 체크, compareBytes 브랜치</li>
 *   <li>F64Codec: null 체크</li>
 *   <li>I64Codec: null 체크</li>
 *   <li>StringCodec: null 체크</li>
 * </ul>
 */
public class CodecCoverageTest {

    // ==================== BytesCodec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void bytesCodec_encode_nullValue_shouldThrow() {
        BytesCodec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void bytesCodec_decode_nullBytes_shouldThrow() {
        BytesCodec.INSTANCE.decode(null);
    }

    @Test
    public void bytesCodec_compareBytes_differentLength_shouldCompareByLength() {
        byte[] shorter = {1, 2};
        byte[] longer = {1, 2, 3};

        // 짧은 배열이 먼저
        assertTrue(BytesCodec.INSTANCE.compareBytes(shorter, longer) < 0);
        assertTrue(BytesCodec.INSTANCE.compareBytes(longer, shorter) > 0);
    }

    @Test
    public void bytesCodec_compareBytes_sameLength_shouldCompareLexicographically() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 4};
        byte[] c = {1, 2, 3};

        assertTrue(BytesCodec.INSTANCE.compareBytes(a, b) < 0);
        assertTrue(BytesCodec.INSTANCE.compareBytes(b, a) > 0);
        assertEquals(0, BytesCodec.INSTANCE.compareBytes(a, c));
    }

    @Test
    public void bytesCodec_compareBytes_unsignedComparison() {
        // 0xFF (255 unsigned) vs 0x01 (1)
        byte[] high = {(byte) 0xFF};
        byte[] low = {0x01};

        // unsigned 비교에서 0xFF > 0x01
        assertTrue(BytesCodec.INSTANCE.compareBytes(high, low) > 0);
    }

    @Test
    public void bytesCodec_equalsBytes_shouldWork() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};
        byte[] c = {1, 2, 4};

        assertTrue(BytesCodec.INSTANCE.equalsBytes(a, b));
        assertFalse(BytesCodec.INSTANCE.equalsBytes(a, c));
    }

    @Test
    public void bytesCodec_hashBytes_shouldWork() {
        byte[] a = {1, 2, 3};
        byte[] b = {1, 2, 3};

        assertEquals(BytesCodec.INSTANCE.hashBytes(a), BytesCodec.INSTANCE.hashBytes(b));
    }

    @Test
    public void bytesCodec_encodeAndDecode_shouldPreserveData() {
        byte[] original = {10, 20, 30, 40, 50};
        byte[] encoded = BytesCodec.INSTANCE.encode(original);
        byte[] decoded = BytesCodec.INSTANCE.decode(encoded);

        assertArrayEquals(original, decoded);
        // encode는 복사본을 반환해야 함
        assertNotSame(original, encoded);
    }

    @Test
    public void bytesCodec_idAndVersion() {
        assertEquals("fx:bytes:lenlex", BytesCodec.INSTANCE.id());
        assertEquals(1, BytesCodec.INSTANCE.version());
    }

    // ==================== F64Codec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void f64Codec_encode_nullValue_shouldThrow() {
        F64Codec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void f64Codec_decode_nullBytes_shouldThrow() {
        F64Codec.INSTANCE.decode(null);
    }

    @Test
    public void f64Codec_encodeAndDecode_shouldPreserveData() {
        double original = 3.14159265359;
        byte[] encoded = F64Codec.INSTANCE.encode(original);
        double decoded = F64Codec.INSTANCE.decode(encoded);

        assertEquals(original, decoded, 0.0);
    }

    @Test
    public void f64Codec_compareBytes_shouldWork() {
        byte[] smaller = F64Codec.INSTANCE.encode(1.0);
        byte[] larger = F64Codec.INSTANCE.encode(2.0);
        byte[] equal = F64Codec.INSTANCE.encode(1.0);

        assertTrue(F64Codec.INSTANCE.compareBytes(smaller, larger) < 0);
        assertTrue(F64Codec.INSTANCE.compareBytes(larger, smaller) > 0);
        assertEquals(0, F64Codec.INSTANCE.compareBytes(smaller, equal));
    }

    @Test
    public void f64Codec_specialValues() {
        // NaN 테스트
        byte[] nan = F64Codec.INSTANCE.encode(Double.NaN);
        assertTrue(Double.isNaN(F64Codec.INSTANCE.decode(nan)));

        // Infinity 테스트
        byte[] posInf = F64Codec.INSTANCE.encode(Double.POSITIVE_INFINITY);
        byte[] negInf = F64Codec.INSTANCE.encode(Double.NEGATIVE_INFINITY);

        assertEquals(Double.POSITIVE_INFINITY, F64Codec.INSTANCE.decode(posInf), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, F64Codec.INSTANCE.decode(negInf), 0.0);
    }

    @Test
    public void f64Codec_idAndVersion() {
        assertEquals("fx:f64", F64Codec.INSTANCE.id());
        assertEquals(1, F64Codec.INSTANCE.version());
    }

    // ==================== I64Codec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void i64Codec_encode_nullValue_shouldThrow() {
        I64Codec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void i64Codec_decode_nullBytes_shouldThrow() {
        I64Codec.INSTANCE.decode(null);
    }

    @Test
    public void i64Codec_encodeAndDecode_shouldPreserveData() {
        long original = 1234567890123456789L;
        byte[] encoded = I64Codec.INSTANCE.encode(original);
        long decoded = I64Codec.INSTANCE.decode(encoded);

        assertEquals(original, decoded);
    }

    @Test
    public void i64Codec_compareBytes_shouldWork() {
        byte[] smaller = I64Codec.INSTANCE.encode(100L);
        byte[] larger = I64Codec.INSTANCE.encode(200L);
        byte[] equal = I64Codec.INSTANCE.encode(100L);

        assertTrue(I64Codec.INSTANCE.compareBytes(smaller, larger) < 0);
        assertTrue(I64Codec.INSTANCE.compareBytes(larger, smaller) > 0);
        assertEquals(0, I64Codec.INSTANCE.compareBytes(smaller, equal));
    }

    @Test
    public void i64Codec_negativeValues() {
        byte[] negative = I64Codec.INSTANCE.encode(-100L);
        byte[] positive = I64Codec.INSTANCE.encode(100L);

        long decodedNeg = I64Codec.INSTANCE.decode(negative);
        assertEquals(-100L, decodedNeg);

        // 비교
        assertTrue(I64Codec.INSTANCE.compareBytes(negative, positive) < 0);
    }

    @Test
    public void i64Codec_extremeValues() {
        byte[] max = I64Codec.INSTANCE.encode(Long.MAX_VALUE);
        byte[] min = I64Codec.INSTANCE.encode(Long.MIN_VALUE);

        assertEquals(Long.MAX_VALUE, (long) I64Codec.INSTANCE.decode(max));
        assertEquals(Long.MIN_VALUE, (long) I64Codec.INSTANCE.decode(min));

        assertTrue(I64Codec.INSTANCE.compareBytes(min, max) < 0);
    }

    @Test
    public void i64Codec_idAndVersion() {
        assertEquals("fx:i64", I64Codec.INSTANCE.id());
        assertEquals(1, I64Codec.INSTANCE.version());
    }

    // ==================== StringCodec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void stringCodec_encode_nullValue_shouldThrow() {
        StringCodec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void stringCodec_decode_nullBytes_shouldThrow() {
        StringCodec.INSTANCE.decode(null);
    }

    @Test
    public void stringCodec_encodeAndDecode_shouldPreserveData() {
        String original = "Hello, World! 한글 테스트";
        byte[] encoded = StringCodec.INSTANCE.encode(original);
        String decoded = StringCodec.INSTANCE.decode(encoded);

        assertEquals(original, decoded);
    }

    @Test
    public void stringCodec_emptyString() {
        String empty = "";
        byte[] encoded = StringCodec.INSTANCE.encode(empty);
        String decoded = StringCodec.INSTANCE.decode(encoded);

        assertEquals(empty, decoded);
    }

    @Test
    public void stringCodec_compareBytes_shouldWork() {
        byte[] apple = StringCodec.INSTANCE.encode("apple");
        byte[] banana = StringCodec.INSTANCE.encode("banana");
        byte[] apple2 = StringCodec.INSTANCE.encode("apple");

        assertTrue(StringCodec.INSTANCE.compareBytes(apple, banana) < 0);
        assertTrue(StringCodec.INSTANCE.compareBytes(banana, apple) > 0);
        assertEquals(0, StringCodec.INSTANCE.compareBytes(apple, apple2));
    }

    @Test
    public void stringCodec_idAndVersion() {
        assertEquals("fx:string:utf8", StringCodec.INSTANCE.id());
        assertEquals(1, StringCodec.INSTANCE.version());
    }

    // ==================== IntegerCodec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void integerCodec_encode_nullValue_shouldThrow() {
        IntegerCodec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void integerCodec_decode_nullBytes_shouldThrow() {
        IntegerCodec.INSTANCE.decode(null);
    }

    @Test
    public void integerCodec_encodeAndDecode() {
        int original = 12345;
        byte[] encoded = IntegerCodec.INSTANCE.encode(original);
        int decoded = IntegerCodec.INSTANCE.decode(encoded);
        assertEquals(original, decoded);
    }

    // ==================== ShortCodec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void shortCodec_encode_nullValue_shouldThrow() {
        ShortCodec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void shortCodec_decode_nullBytes_shouldThrow() {
        ShortCodec.INSTANCE.decode(null);
    }

    @Test
    public void shortCodec_encodeAndDecode() {
        short original = 12345;
        byte[] encoded = ShortCodec.INSTANCE.encode(original);
        short decoded = ShortCodec.INSTANCE.decode(encoded);
        assertEquals(original, decoded);
    }

    // ==================== ByteCodec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void byteCodec_encode_nullValue_shouldThrow() {
        ByteCodec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void byteCodec_decode_nullBytes_shouldThrow() {
        ByteCodec.INSTANCE.decode(null);
    }

    @Test
    public void byteCodec_encodeAndDecode() {
        byte original = 123;
        byte[] encoded = ByteCodec.INSTANCE.encode(original);
        byte decoded = ByteCodec.INSTANCE.decode(encoded);
        assertEquals(original, decoded);
    }

    // ==================== FloatCodec 테스트 ====================

    @Test(expected = NullPointerException.class)
    public void floatCodec_encode_nullValue_shouldThrow() {
        FloatCodec.INSTANCE.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void floatCodec_decode_nullBytes_shouldThrow() {
        FloatCodec.INSTANCE.decode(null);
    }

    @Test
    public void floatCodec_encodeAndDecode() {
        float original = 3.14f;
        byte[] encoded = FloatCodec.INSTANCE.encode(original);
        float decoded = FloatCodec.INSTANCE.decode(encoded);
        assertEquals(original, decoded, 0.0001f);
    }
}
