package com.snoworca.fxstore.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CodecRef 테스트
 * P1/P3 클래스 커버리지 개선
 */
public class CodecRefTest {

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_shouldSetAllFields() {
        // Given & When
        CodecRef ref = new CodecRef("fx:i64", 1, FxType.I64);

        // Then
        assertEquals("fx:i64", ref.codecId());
        assertEquals(1, ref.codecVersion());
        assertEquals(FxType.I64, ref.builtinType());
    }

    @Test
    public void constructor_withNullBuiltinType_shouldWork() {
        // Given & When
        CodecRef ref = new CodecRef("custom:uuid", 1, null);

        // Then
        assertEquals("custom:uuid", ref.codecId());
        assertNull(ref.builtinType());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullCodecId_shouldThrow() {
        // Given & When & Then
        new CodecRef(null, 1, FxType.I64);
    }

    // ==================== 접근자 테스트 ====================

    @Test
    public void codecId_shouldReturnValue() {
        // Given
        CodecRef ref = new CodecRef("test:codec", 2, FxType.STRING);

        // When & Then
        assertEquals("test:codec", ref.codecId());
        assertEquals("test:codec", ref.getCodecId()); // alias
    }

    @Test
    public void codecVersion_shouldReturnValue() {
        // Given
        CodecRef ref = new CodecRef("test:codec", 5, FxType.I64);

        // When & Then
        assertEquals(5, ref.codecVersion());
        assertEquals(5, ref.getCodecVersion()); // alias
    }

    @Test
    public void builtinType_shouldReturnValue() {
        // Given
        CodecRef ref = new CodecRef("fx:bytes", 1, FxType.BYTES);

        // When & Then
        assertEquals(FxType.BYTES, ref.builtinType());
        assertEquals(FxType.BYTES, ref.getType()); // alias
    }

    // ==================== equals 테스트 ====================

    @Test
    public void equals_sameValues_shouldBeEqual() {
        // Given
        CodecRef ref1 = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef ref2 = new CodecRef("fx:i64", 1, FxType.I64);

        // When & Then
        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    public void equals_differentCodecId_shouldNotBeEqual() {
        // Given
        CodecRef ref1 = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef ref2 = new CodecRef("fx:i32", 1, FxType.I64);

        // When & Then
        assertNotEquals(ref1, ref2);
    }

    @Test
    public void equals_differentVersion_shouldNotBeEqual() {
        // Given
        CodecRef ref1 = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef ref2 = new CodecRef("fx:i64", 2, FxType.I64);

        // When & Then
        assertNotEquals(ref1, ref2);
    }

    @Test
    public void equals_differentBuiltinType_shouldNotBeEqual() {
        // Given
        CodecRef ref1 = new CodecRef("fx:num", 1, FxType.I64);
        CodecRef ref2 = new CodecRef("fx:num", 1, FxType.STRING);

        // When & Then
        assertNotEquals(ref1, ref2);
    }

    @Test
    public void equals_sameInstance_shouldBeEqual() {
        // Given
        CodecRef ref = new CodecRef("fx:i64", 1, FxType.I64);

        // When & Then
        assertEquals(ref, ref);
    }

    @Test
    public void equals_null_shouldNotBeEqual() {
        // Given
        CodecRef ref = new CodecRef("fx:i64", 1, FxType.I64);

        // When & Then
        assertNotEquals(ref, null);
    }

    @Test
    public void equals_differentType_shouldNotBeEqual() {
        // Given
        CodecRef ref = new CodecRef("fx:i64", 1, FxType.I64);

        // When & Then
        assertNotEquals(ref, "not a CodecRef");
    }

    // ==================== hashCode 테스트 ====================

    @Test
    public void hashCode_sameValues_shouldBeEqual() {
        // Given
        CodecRef ref1 = new CodecRef("fx:string", 1, FxType.STRING);
        CodecRef ref2 = new CodecRef("fx:string", 1, FxType.STRING);

        // When & Then
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    public void hashCode_shouldBeConsistent() {
        // Given
        CodecRef ref = new CodecRef("fx:i32", 1, FxType.I64);

        // When & Then
        int hash1 = ref.hashCode();
        int hash2 = ref.hashCode();
        assertEquals(hash1, hash2);
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldIncludeCodecId() {
        // Given
        CodecRef ref = new CodecRef("fx:i64", 1, FxType.I64);

        // When
        String str = ref.toString();

        // Then
        assertTrue(str.contains("fx:i64"));
    }

    @Test
    public void toString_shouldIncludeVersion() {
        // Given
        CodecRef ref = new CodecRef("fx:string", 5, FxType.STRING);

        // When
        String str = ref.toString();

        // Then
        assertTrue(str.contains("5"));
    }

    @Test
    public void toString_shouldIncludeBuiltinType() {
        // Given
        CodecRef ref = new CodecRef("fx:bytes", 1, FxType.BYTES);

        // When
        String str = ref.toString();

        // Then
        assertTrue(str.contains("BYTES"));
    }

    // ==================== encode/decode 테스트 ====================

    @Test
    public void encode_decode_shouldRoundTrip() {
        // Given
        CodecRef original = new CodecRef("fx:i64", 1, FxType.I64);

        // When
        byte[] encoded = original.encode();
        CodecRef decoded = CodecRef.decode(encoded);

        // Then
        assertEquals(original, decoded);
    }

    @Test
    public void encode_decode_withNullBuiltinType_shouldRoundTrip() {
        // Given
        CodecRef original = new CodecRef("custom:uuid", 2, null);

        // When
        byte[] encoded = original.encode();
        CodecRef decoded = CodecRef.decode(encoded);

        // Then
        assertEquals(original.codecId(), decoded.codecId());
        assertEquals(original.codecVersion(), decoded.codecVersion());
        assertNull(decoded.builtinType());
    }

    @Test
    public void encode_decode_allBuiltinTypes_shouldRoundTrip() {
        // Given & When & Then
        for (FxType type : FxType.values()) {
            CodecRef original = new CodecRef("fx:test", 1, type);
            byte[] encoded = original.encode();
            CodecRef decoded = CodecRef.decode(encoded);

            assertEquals("Failed for type " + type, original, decoded);
        }
    }

    @Test
    public void encode_decode_largeVersion_shouldRoundTrip() {
        // Given
        CodecRef original = new CodecRef("fx:test", Integer.MAX_VALUE, FxType.I64);

        // When
        byte[] encoded = original.encode();
        CodecRef decoded = CodecRef.decode(encoded);

        // Then
        assertEquals(original, decoded);
    }

    @Test
    public void encode_decode_longCodecId_shouldRoundTrip() {
        // Given
        String longId = "com.example.very.long.package.name:CustomCodecWithLongName";
        CodecRef original = new CodecRef(longId, 1, FxType.STRING);

        // When
        byte[] encoded = original.encode();
        CodecRef decoded = CodecRef.decode(encoded);

        // Then
        assertEquals(original, decoded);
        assertEquals(longId, decoded.codecId());
    }

    @Test
    public void encode_shouldProduceNonEmptyBytes() {
        // Given
        CodecRef ref = new CodecRef("fx:i32", 1, FxType.I64);

        // When
        byte[] encoded = ref.encode();

        // Then
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    // ==================== FxType 커버리지 ====================

    @Test
    public void fxType_values_shouldReturnAllTypes() {
        // Given & When
        FxType[] types = FxType.values();

        // Then
        assertTrue(types.length > 0);
    }

    @Test
    public void fxType_commonTypes_shouldExist() {
        // Given & When & Then
        assertNotNull(FxType.valueOf("I64"));
        assertNotNull(FxType.valueOf("F64"));
        assertNotNull(FxType.valueOf("STRING"));
        assertNotNull(FxType.valueOf("BYTES"));
    }
}
