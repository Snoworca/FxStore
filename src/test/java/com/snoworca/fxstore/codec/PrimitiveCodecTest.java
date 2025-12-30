package com.snoworca.fxstore.codec;

import com.snoworca.fxstore.api.FxCodec;
import com.snoworca.fxstore.api.FxCodecs;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ByteCodec, ShortCodec, FloatCodec 테스트
 * P2 클래스 커버리지 개선
 */
public class PrimitiveCodecTest {

    // ==================== ByteCodec 테스트 ====================

    @Test
    public void byteCodec_boundaryValues_shouldEncodeDecode() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);

        // When & Then: 경계값 라운드트립
        assertRoundTrip(codec, Byte.MIN_VALUE);
        assertRoundTrip(codec, Byte.MAX_VALUE);
        assertRoundTrip(codec, (byte) 0);
        assertRoundTrip(codec, (byte) -1);
        assertRoundTrip(codec, (byte) 1);
    }

    @Test
    public void byteCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        byte[] values = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE};

        // When: 인코딩
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // Then: 순서 유지
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue("Order should be preserved",
                codec.compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    @Test
    public void byteCodec_id_shouldReturnCorrectId() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);

        // When
        String id = codec.id();

        // Then
        assertNotNull(id);
        assertEquals("fx:i64", id);
    }

    @Test
    public void byteCodec_version_shouldReturn1() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);

        // When & Then
        assertEquals(1, codec.version());
    }

    @Test(expected = NullPointerException.class)
    public void byteCodec_encodeNull_shouldThrow() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);

        // When & Then
        codec.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void byteCodec_decodeNull_shouldThrow() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);

        // When & Then
        codec.decode(null);
    }

    @Test
    public void byteCodec_equalsBytes_sameValues_shouldReturnTrue() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        byte[] a = codec.encode((byte) 42);
        byte[] b = codec.encode((byte) 42);

        // When & Then
        assertTrue(codec.equalsBytes(a, b));
    }

    @Test
    public void byteCodec_equalsBytes_differentValues_shouldReturnFalse() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        byte[] a = codec.encode((byte) 42);
        byte[] b = codec.encode((byte) 43);

        // When & Then
        assertFalse(codec.equalsBytes(a, b));
    }

    @Test
    public void byteCodec_hashBytes_shouldBeConsistent() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        byte[] encoded = codec.encode((byte) 100);

        // When
        int hash1 = codec.hashBytes(encoded);
        int hash2 = codec.hashBytes(encoded);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    public void byteCodec_hashBytes_sameValues_shouldMatchEquals() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        byte[] a = codec.encode((byte) 50);
        byte[] b = codec.encode((byte) 50);

        // When & Then
        if (codec.equalsBytes(a, b)) {
            assertEquals(codec.hashBytes(a), codec.hashBytes(b));
        }
    }

    @Test
    public void byteCodec_compareBytes_equalValues_shouldReturnZero() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        byte[] a = codec.encode((byte) 10);
        byte[] b = codec.encode((byte) 10);

        // When & Then
        assertEquals(0, codec.compareBytes(a, b));
    }

    @Test
    public void byteCodec_singleton_shouldBeInstance() {
        // Given & When & Then
        assertNotNull(ByteCodec.INSTANCE);
        assertEquals("fx:i64", ByteCodec.CODEC_ID);
        assertEquals(1, ByteCodec.VERSION);
    }

    @Test(expected = ArithmeticException.class)
    public void byteCodec_decode_outOfRange_shouldThrow() {
        // Given
        FxCodec<Byte> codec = FxCodecs.global().get(Byte.class);
        // Encode a long value that is out of byte range
        FxCodec<Long> longCodec = FxCodecs.global().get(Long.class);
        byte[] encoded = longCodec.encode(1000L); // Out of byte range

        // When & Then
        codec.decode(encoded);
    }

    // ==================== ShortCodec 테스트 ====================

    @Test
    public void shortCodec_boundaryValues_shouldEncodeDecode() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);

        // When & Then
        assertRoundTrip(codec, Short.MIN_VALUE);
        assertRoundTrip(codec, Short.MAX_VALUE);
        assertRoundTrip(codec, (short) 0);
        assertRoundTrip(codec, (short) -1);
        assertRoundTrip(codec, (short) 1);
    }

    @Test
    public void shortCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);
        short[] values = {Short.MIN_VALUE, -100, 0, 100, Short.MAX_VALUE};

        // When: 인코딩
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // Then: 순서 유지
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue("Order should be preserved",
                codec.compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    @Test
    public void shortCodec_id_shouldReturnCorrectId() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);

        // When
        String id = codec.id();

        // Then
        assertEquals("fx:i64", id);
    }

    @Test
    public void shortCodec_version_shouldReturn1() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);

        // When & Then
        assertEquals(1, codec.version());
    }

    @Test(expected = NullPointerException.class)
    public void shortCodec_encodeNull_shouldThrow() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);

        // When & Then
        codec.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void shortCodec_decodeNull_shouldThrow() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);

        // When & Then
        codec.decode(null);
    }

    @Test
    public void shortCodec_equalsBytes_sameValues_shouldReturnTrue() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);
        byte[] a = codec.encode((short) 1000);
        byte[] b = codec.encode((short) 1000);

        // When & Then
        assertTrue(codec.equalsBytes(a, b));
    }

    @Test
    public void shortCodec_hashBytes_shouldBeConsistent() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);
        byte[] encoded = codec.encode((short) 500);

        // When
        int hash1 = codec.hashBytes(encoded);
        int hash2 = codec.hashBytes(encoded);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    public void shortCodec_compareBytes_shouldWork() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);
        byte[] a = codec.encode((short) -100);
        byte[] b = codec.encode((short) 100);

        // When & Then
        assertTrue(codec.compareBytes(a, b) < 0);
        assertTrue(codec.compareBytes(b, a) > 0);
        assertEquals(0, codec.compareBytes(a, a));
    }

    @Test
    public void shortCodec_singleton_shouldBeInstance() {
        // Given & When & Then
        assertNotNull(ShortCodec.INSTANCE);
        assertEquals("fx:i64", ShortCodec.CODEC_ID);
        assertEquals(1, ShortCodec.VERSION);
    }

    @Test(expected = ArithmeticException.class)
    public void shortCodec_decode_outOfRange_shouldThrow() {
        // Given
        FxCodec<Short> codec = FxCodecs.global().get(Short.class);
        FxCodec<Long> longCodec = FxCodecs.global().get(Long.class);
        byte[] encoded = longCodec.encode(100000L); // Out of short range

        // When & Then
        codec.decode(encoded);
    }

    // ==================== FloatCodec 테스트 ====================

    @Test
    public void floatCodec_boundaryValues_shouldEncodeDecode() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);

        // When & Then
        assertRoundTrip(codec, Float.MIN_VALUE);
        assertRoundTrip(codec, Float.MAX_VALUE);
        assertRoundTrip(codec, 0.0f);
        assertRoundTrip(codec, -0.0f);
        assertRoundTrip(codec, 1.0f);
        assertRoundTrip(codec, -1.0f);
    }

    @Test
    public void floatCodec_specialValues_shouldEncodeDecode() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);

        // When & Then
        assertRoundTrip(codec, Float.POSITIVE_INFINITY);
        assertRoundTrip(codec, Float.NEGATIVE_INFINITY);
        // NaN needs special handling
        byte[] nanEncoded = codec.encode(Float.NaN);
        assertTrue(Float.isNaN(codec.decode(nanEncoded)));
    }

    @Test
    public void floatCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);
        float[] values = {Float.NEGATIVE_INFINITY, -1000.0f, -1.0f, 0.0f, 1.0f, 1000.0f, Float.POSITIVE_INFINITY};

        // When: 인코딩
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // Then: 순서 유지
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue("Order should be preserved for " + values[i] + " vs " + values[i+1],
                codec.compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    @Test
    public void floatCodec_id_shouldReturnCorrectId() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);

        // When
        String id = codec.id();

        // Then
        assertEquals("fx:f64", id);
    }

    @Test
    public void floatCodec_version_shouldReturn1() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);

        // When & Then
        assertEquals(1, codec.version());
    }

    @Test(expected = NullPointerException.class)
    public void floatCodec_encodeNull_shouldThrow() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);

        // When & Then
        codec.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void floatCodec_decodeNull_shouldThrow() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);

        // When & Then
        codec.decode(null);
    }

    @Test
    public void floatCodec_equalsBytes_sameValues_shouldReturnTrue() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);
        byte[] a = codec.encode(3.14f);
        byte[] b = codec.encode(3.14f);

        // When & Then
        assertTrue(codec.equalsBytes(a, b));
    }

    @Test
    public void floatCodec_hashBytes_shouldBeConsistent() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);
        byte[] encoded = codec.encode(2.718f);

        // When
        int hash1 = codec.hashBytes(encoded);
        int hash2 = codec.hashBytes(encoded);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    public void floatCodec_compareBytes_shouldWork() {
        // Given
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);
        byte[] a = codec.encode(-100.0f);
        byte[] b = codec.encode(100.0f);

        // When & Then
        assertTrue(codec.compareBytes(a, b) < 0);
        assertTrue(codec.compareBytes(b, a) > 0);
        assertEquals(0, codec.compareBytes(a, a));
    }

    @Test
    public void floatCodec_singleton_shouldBeInstance() {
        // Given & When & Then
        assertNotNull(FloatCodec.INSTANCE);
        assertEquals("fx:f64", FloatCodec.CODEC_ID);
        assertEquals(1, FloatCodec.VERSION);
    }

    @Test
    public void floatCodec_precisionLoss_shouldBeMinimal() {
        // Given: Float를 double로 저장하므로 정밀도 유지
        FxCodec<Float> codec = FxCodecs.global().get(Float.class);
        float original = 0.123456789f;

        // When
        byte[] encoded = codec.encode(original);
        float decoded = codec.decode(encoded);

        // Then: Float 정밀도 내에서 동일
        assertEquals(original, decoded, 1e-7f);
    }

    // ==================== IntegerCodec 테스트 ====================

    @Test
    public void integerCodec_boundaryValues_shouldEncodeDecode() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);

        // When & Then
        assertRoundTrip(codec, Integer.MIN_VALUE);
        assertRoundTrip(codec, Integer.MAX_VALUE);
        assertRoundTrip(codec, 0);
        assertRoundTrip(codec, -1);
        assertRoundTrip(codec, 1);
    }

    @Test
    public void integerCodec_ordering_shouldPreserveNaturalOrder() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);
        int[] values = {Integer.MIN_VALUE, -1000, 0, 1000, Integer.MAX_VALUE};

        // When: 인코딩
        byte[][] encoded = new byte[values.length][];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = codec.encode(values[i]);
        }

        // Then: 순서 유지
        for (int i = 0; i < encoded.length - 1; i++) {
            assertTrue("Order should be preserved",
                codec.compareBytes(encoded[i], encoded[i + 1]) < 0);
        }
    }

    @Test
    public void integerCodec_id_shouldReturnCorrectId() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);

        // When
        String id = codec.id();

        // Then
        assertEquals("fx:i64", id);
    }

    @Test
    public void integerCodec_version_shouldReturn1() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);

        // When & Then
        assertEquals(1, codec.version());
    }

    @Test(expected = NullPointerException.class)
    public void integerCodec_encodeNull_shouldThrow() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);

        // When & Then
        codec.encode(null);
    }

    @Test(expected = NullPointerException.class)
    public void integerCodec_decodeNull_shouldThrow() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);

        // When & Then
        codec.decode(null);
    }

    @Test
    public void integerCodec_equalsBytes_shouldWork() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);
        byte[] a = codec.encode(12345);
        byte[] b = codec.encode(12345);
        byte[] c = codec.encode(54321);

        // When & Then
        assertTrue(codec.equalsBytes(a, b));
        assertFalse(codec.equalsBytes(a, c));
    }

    @Test
    public void integerCodec_hashBytes_shouldBeConsistent() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);
        byte[] encoded = codec.encode(99999);

        // When
        int hash1 = codec.hashBytes(encoded);
        int hash2 = codec.hashBytes(encoded);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    public void integerCodec_singleton_shouldBeInstance() {
        // Given & When & Then
        assertNotNull(IntegerCodec.INSTANCE);
        assertEquals("fx:i64", IntegerCodec.CODEC_ID);
        assertEquals(1, IntegerCodec.VERSION);
    }

    @Test(expected = ArithmeticException.class)
    public void integerCodec_decode_outOfRange_shouldThrow() {
        // Given
        FxCodec<Integer> codec = FxCodecs.global().get(Integer.class);
        FxCodec<Long> longCodec = FxCodecs.global().get(Long.class);
        byte[] encoded = longCodec.encode(Long.MAX_VALUE); // Out of int range

        // When & Then
        codec.decode(encoded);
    }

    // ==================== 공통 헬퍼 메서드 ====================

    private <T> void assertRoundTrip(FxCodec<T> codec, T value) {
        byte[] encoded = codec.encode(value);
        T decoded = codec.decode(encoded);
        assertEquals("Round-trip failed for value: " + value, value, decoded);
    }
}
