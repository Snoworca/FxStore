package com.snoworca.fxstore.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * FxCodecs 및 GlobalCodecRegistry 테스트
 * P2 클래스 커버리지 개선
 */
public class FxCodecsTest {

    // ==================== global() 테스트 ====================

    @Test
    public void global_shouldReturnNonNull() {
        // Given & When
        FxCodecRegistry registry = FxCodecs.global();

        // Then
        assertNotNull(registry);
    }

    @Test
    public void global_shouldReturnSameInstance() {
        // Given
        FxCodecRegistry registry1 = FxCodecs.global();
        FxCodecRegistry registry2 = FxCodecs.global();

        // Then
        assertSame(registry1, registry2);
    }

    // ==================== 내장 코덱 등록 테스트 ====================

    @Test
    public void builtin_longCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<Long> codec = registry.get(Long.class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:i64", codec.id());
    }

    @Test
    public void builtin_doubleCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<Double> codec = registry.get(Double.class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:f64", codec.id());
    }

    @Test
    public void builtin_stringCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<String> codec = registry.get(String.class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:string:utf8", codec.id());
    }

    @Test
    public void builtin_bytesCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<byte[]> codec = registry.get(byte[].class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:bytes:lenlex", codec.id());
    }

    @Test
    public void builtin_integerCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<Integer> codec = registry.get(Integer.class);

        // Then
        assertNotNull(codec);
    }

    @Test
    public void builtin_floatCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<Float> codec = registry.get(Float.class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:f64", codec.id());
    }

    @Test
    public void builtin_shortCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<Short> codec = registry.get(Short.class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:i64", codec.id());
    }

    @Test
    public void builtin_byteCodec_shouldBeRegistered() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<Byte> codec = registry.get(Byte.class);

        // Then
        assertNotNull(codec);
        assertEquals("fx:i64", codec.id());
    }

    // ==================== get() 테스트 ====================

    @Test
    public void get_registeredType_shouldReturnCodec() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<String> codec = registry.get(String.class);

        // Then
        assertNotNull(codec);
    }

    @Test(expected = FxException.class)
    public void get_unregisteredType_shouldThrow() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When & Then: CustomClass is not registered
        registry.get(UnregisteredClass.class);
    }

    @Test
    public void get_unregisteredType_errorCode() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When & Then
        try {
            registry.get(UnregisteredClass.class);
            fail("Should throw FxException");
        } catch (FxException e) {
            assertEquals(FxErrorCode.CODEC_NOT_FOUND, e.code());
        }
    }

    // ==================== getById() 테스트 ====================

    @Test
    public void getById_registeredCodec_shouldReturn() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<?> codec = registry.getById("fx:i64", 1);

        // Then
        assertNotNull(codec);
    }

    @Test
    public void getById_unregisteredId_shouldReturnNull() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When
        FxCodec<?> codec = registry.getById("nonexistent:codec", 1);

        // Then
        assertNull(codec);
    }

    @Test
    public void getById_wrongVersion_shouldReturnNull() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When: Version 999 doesn't exist
        FxCodec<?> codec = registry.getById("fx:i64", 999);

        // Then
        assertNull(codec);
    }

    // ==================== register() 테스트 ====================

    @Test(expected = FxException.class)
    @SuppressWarnings("unchecked")
    public void register_nullType_shouldThrow() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When & Then
        registry.register(null, new MockCodec<UnregisteredClass>());
    }

    @Test(expected = FxException.class)
    public void register_nullCodec_shouldThrow() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When & Then
        registry.register(UnregisteredClass.class, null);
    }

    @Test(expected = FxException.class)
    public void register_alreadyRegistered_shouldThrow() {
        // Given: Long is already registered
        FxCodecRegistry registry = FxCodecs.global();

        // When & Then
        registry.register(Long.class, new MockCodec<Long>());
    }

    @Test
    public void register_alreadyRegistered_errorDetails() {
        // Given
        FxCodecRegistry registry = FxCodecs.global();

        // When & Then
        try {
            registry.register(Long.class, new MockCodec<Long>());
            fail("Should throw FxException");
        } catch (FxException e) {
            assertEquals(FxErrorCode.ILLEGAL_ARGUMENT, e.code());
            assertTrue(e.getMessage().contains("already registered"));
        }
    }

    // ==================== 코덱 사용 테스트 ====================

    @Test
    public void codec_roundTrip_long() {
        // Given
        FxCodec<Long> codec = FxCodecs.global().get(Long.class);
        Long original = 12345678901234L;

        // When
        byte[] encoded = codec.encode(original);
        Long decoded = codec.decode(encoded);

        // Then
        assertEquals(original, decoded);
    }

    @Test
    public void codec_roundTrip_double() {
        // Given
        FxCodec<Double> codec = FxCodecs.global().get(Double.class);
        Double original = 3.141592653589793;

        // When
        byte[] encoded = codec.encode(original);
        Double decoded = codec.decode(encoded);

        // Then
        assertEquals(original, decoded, 0.0);
    }

    @Test
    public void codec_roundTrip_string() {
        // Given
        FxCodec<String> codec = FxCodecs.global().get(String.class);
        String original = "Hello, FxStore! 한글 테스트 🚀";

        // When
        byte[] encoded = codec.encode(original);
        String decoded = codec.decode(encoded);

        // Then
        assertEquals(original, decoded);
    }

    @Test
    public void codec_roundTrip_bytes() {
        // Given
        FxCodec<byte[]> codec = FxCodecs.global().get(byte[].class);
        byte[] original = {1, 2, 3, 4, 5, (byte) 0xFF, 0, (byte) 0x80};

        // When
        byte[] encoded = codec.encode(original);
        byte[] decoded = codec.decode(encoded);

        // Then
        assertArrayEquals(original, decoded);
    }

    // ==================== 헬퍼 클래스 ====================

    private static class UnregisteredClass {
    }

    private static class MockCodec<T> implements FxCodec<T> {
        @Override
        public String id() {
            return "mock:codec";
        }

        @Override
        public int version() {
            return 1;
        }

        @Override
        public byte[] encode(T value) {
            return new byte[0];
        }

        @Override
        @SuppressWarnings("unchecked")
        public T decode(byte[] bytes) {
            return null;
        }

        @Override
        public int compareBytes(byte[] a, byte[] b) {
            return 0;
        }

        @Override
        public boolean equalsBytes(byte[] a, byte[] b) {
            return java.util.Arrays.equals(a, b);
        }

        @Override
        public int hashBytes(byte[] bytes) {
            return java.util.Arrays.hashCode(bytes);
        }
    }
}
