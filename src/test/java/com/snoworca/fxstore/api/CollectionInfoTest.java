package com.snoworca.fxstore.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * CollectionInfo 테스트
 * P1 클래스 커버리지 개선
 */
public class CollectionInfoTest {

    // ==================== 생성자 테스트 ====================

    @Test
    public void constructor_shouldSetAllFields() {
        // Given
        CodecRef keyCodec = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);

        // When
        CollectionInfo info = new CollectionInfo("myMap", CollectionKind.MAP, keyCodec, valueCodec);

        // Then
        assertEquals("myMap", info.name());
        assertEquals(CollectionKind.MAP, info.kind());
        assertEquals(keyCodec, info.keyCodec());
        assertEquals(valueCodec, info.valueCodec());
    }

    @Test
    public void constructor_withNullKeyCodec_shouldWork() {
        // Given: SET, LIST, DEQUE have null keyCodec
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);

        // When
        CollectionInfo info = new CollectionInfo("mySet", CollectionKind.SET, null, valueCodec);

        // Then
        assertEquals("mySet", info.name());
        assertEquals(CollectionKind.SET, info.kind());
        assertNull(info.keyCodec());
        assertEquals(valueCodec, info.valueCodec());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullName_shouldThrow() {
        // Given & When & Then
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        new CollectionInfo(null, CollectionKind.MAP, null, valueCodec);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullKind_shouldThrow() {
        // Given & When & Then
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        new CollectionInfo("test", null, null, valueCodec);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullValueCodec_shouldThrow() {
        // Given & When & Then
        new CollectionInfo("test", CollectionKind.SET, null, null);
    }

    // ==================== 접근자 테스트 ====================

    @Test
    public void name_shouldReturnName() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:i64", 1, FxType.I64);
        CollectionInfo info = new CollectionInfo("testCollection", CollectionKind.LIST, null, valueCodec);

        // When & Then
        assertEquals("testCollection", info.name());
    }

    @Test
    public void kind_allKinds_shouldWork() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);

        // When & Then
        for (CollectionKind kind : CollectionKind.values()) {
            CollectionInfo info = new CollectionInfo("test", kind, null, valueCodec);
            assertEquals(kind, info.kind());
        }
    }

    // ==================== equals 테스트 ====================

    @Test
    public void equals_sameValues_shouldBeEqual() {
        // Given
        CodecRef keyCodec = new CodecRef("fx:i64", 1, FxType.I64);
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);

        CollectionInfo info1 = new CollectionInfo("myMap", CollectionKind.MAP, keyCodec, valueCodec);
        CollectionInfo info2 = new CollectionInfo("myMap", CollectionKind.MAP, keyCodec, valueCodec);

        // When & Then
        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    public void equals_differentName_shouldNotBeEqual() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info1 = new CollectionInfo("name1", CollectionKind.SET, null, valueCodec);
        CollectionInfo info2 = new CollectionInfo("name2", CollectionKind.SET, null, valueCodec);

        // When & Then
        assertNotEquals(info1, info2);
    }

    @Test
    public void equals_differentKind_shouldNotBeEqual() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info1 = new CollectionInfo("test", CollectionKind.SET, null, valueCodec);
        CollectionInfo info2 = new CollectionInfo("test", CollectionKind.LIST, null, valueCodec);

        // When & Then
        assertNotEquals(info1, info2);
    }

    @Test
    public void equals_sameInstance_shouldBeEqual() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info = new CollectionInfo("test", CollectionKind.DEQUE, null, valueCodec);

        // When & Then
        assertEquals(info, info);
    }

    @Test
    public void equals_null_shouldNotBeEqual() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info = new CollectionInfo("test", CollectionKind.DEQUE, null, valueCodec);

        // When & Then
        assertNotEquals(info, null);
    }

    @Test
    public void equals_differentType_shouldNotBeEqual() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info = new CollectionInfo("test", CollectionKind.DEQUE, null, valueCodec);

        // When & Then
        assertNotEquals(info, "not a CollectionInfo");
    }

    // ==================== hashCode 테스트 ====================

    @Test
    public void hashCode_sameValues_shouldBeEqual() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:i32", 1, FxType.I64);
        CollectionInfo info1 = new CollectionInfo("test", CollectionKind.LIST, null, valueCodec);
        CollectionInfo info2 = new CollectionInfo("test", CollectionKind.LIST, null, valueCodec);

        // When & Then
        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    public void hashCode_shouldBeConsistent() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info = new CollectionInfo("test", CollectionKind.SET, null, valueCodec);

        // When & Then
        int hash1 = info.hashCode();
        int hash2 = info.hashCode();
        assertEquals(hash1, hash2);
    }

    // ==================== toString 테스트 ====================

    @Test
    public void toString_shouldIncludeName() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info = new CollectionInfo("testName", CollectionKind.MAP, null, valueCodec);

        // When
        String str = info.toString();

        // Then
        assertTrue(str.contains("testName"));
        assertTrue(str.contains("MAP"));
    }

    @Test
    public void toString_shouldIncludeKind() {
        // Given
        CodecRef valueCodec = new CodecRef("fx:string", 1, FxType.STRING);
        CollectionInfo info = new CollectionInfo("test", CollectionKind.DEQUE, null, valueCodec);

        // When
        String str = info.toString();

        // Then
        assertTrue(str.contains("DEQUE"));
    }

    // ==================== CollectionKind 커버리지 ====================

    @Test
    public void collectionKind_values_shouldReturnAllKinds() {
        // Given & When
        CollectionKind[] kinds = CollectionKind.values();

        // Then
        assertEquals(4, kinds.length);
    }

    @Test
    public void collectionKind_valueOf_shouldWork() {
        // Given & When & Then
        assertEquals(CollectionKind.MAP, CollectionKind.valueOf("MAP"));
        assertEquals(CollectionKind.SET, CollectionKind.valueOf("SET"));
        assertEquals(CollectionKind.LIST, CollectionKind.valueOf("LIST"));
        assertEquals(CollectionKind.DEQUE, CollectionKind.valueOf("DEQUE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void collectionKind_valueOf_invalidName_shouldThrow() {
        // Given & When & Then
        CollectionKind.valueOf("INVALID");
    }
}
